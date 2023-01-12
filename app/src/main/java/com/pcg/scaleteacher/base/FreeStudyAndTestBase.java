package com.pcg.scaleteacher.base;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;

import com.pcg.scaleteacher.R;

/* 该类作为自由教学活动和测试活动的基类，主要处理两者共通的测量方式识别问题 */
public class FreeStudyAndTestBase extends CompletedFunctionBase {

    //所处的学习环节
    protected enum FreeStudyState {
        FREE_TRY,   //自由尝试阶段，用户自行启动测量功能（如果当前是测试模式，将一直保持该值）
        WAIT_REPEATING,     //等待重复阶段（仅限自由学习时已经获取了输入之后再进行的调整）
        REPEATING,     //重复巩固阶段（仅限自由学习时已经获取了输入之后再进行的调整）
    }
    protected int repeatingCounter = 0;      //自由学习的巩固练习次数
    protected static final int repeatingLimit = 2;       //自由学习的巩固练习次数上限

    protected FreeStudyState currentStudyState = FreeStudyState.FREE_TRY;

    @Override
    protected boolean onFirstFingerDown(MotionEvent e) {
        //未开启无障碍服务时，需要模拟监听双击
        if (!hasScreenReader) {
            //当前点击是双击的后一次点击且间隔时间达标，意味着成功激活手指监测区域
            if (isDoubleTapping && doubleTappingTime <= doubleTappingTimeLimit)
                endDoubleTapping();
                //当前点击为双击的前一次点击，需要等待下一次点击
            else {
                startDoubleTapping();
                return false;
            }
        }
        tts.speak(getString(R.string.gesture_detection_ready), TextToSpeech.QUEUE_FLUSH, null, "gestureDetectionReady");
        currentActivationState = ActivationState.WAITING_FINGER1;
        startHolding();

        return false;
    }

    @Override
    protected boolean onMoreFingersDown(MotionEvent e) {
        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        int pointerIndex = e.findPointerIndex(pointerId);

        if (pointerId == 0)
            return false;
        if (pointerId == 1) {
            currentFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
            originalFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
            finger1Ready = true;
            currentActivationState = ActivationState.WAITING_FINGER2;
            startHolding();
        }
        else if (pointerId == 2) {
            currentFinger2.update(e.getX(pointerIndex), e.getY(pointerIndex));
            originalFinger2.update(e.getX(pointerIndex), e.getY(pointerIndex));
            finger2Ready = true;
            currentActivationState = ActivationState.FINGER_ACTIVATING;
            startHolding();
        }
        else {
            tts.speak(getString(R.string.too_many_fingers), TextToSpeech.QUEUE_FLUSH, null, "tooManyFingers");
            currentActivationState = ActivationState.UNACTIVATED;
        }

        return false;
    }

    @Override
    protected boolean onFingerMove(MotionEvent e) {
        int finger1Index, finger2Index;
        int xDiff1, yDiff1, xDiff2, yDiff2;
        if (finger1Ready) {
            finger1Index = e.findPointerIndex(1);
            currentFinger1.update(e.getX(finger1Index), e.getY(finger1Index));
            xDiff1 = currentFinger1.getX() - originalFinger1.getX();
            yDiff1 = currentFinger1.getY() - originalFinger1.getY();
            if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit) {
                originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                startHolding();
            }
        }
        if (finger2Ready) {
            finger2Index = e.findPointerIndex(2);
            currentFinger2.update(e.getX(finger2Index), e.getY(finger2Index));
            xDiff2 = currentFinger2.getX() - originalFinger2.getX();
            yDiff2 = currentFinger2.getY() - originalFinger2.getY();
            if (Math.abs(xDiff2) > holdingPositionLimit || Math.abs(yDiff2) > holdingPositionLimit) {
                originalFinger2.update(currentFinger2.getX(), currentFinger2.getY());
                startHolding();
            }
        }

        return false;
    }

    @Override
    protected boolean onCertainFingerUp(MotionEvent e) {
        //未开启无障碍服务时需要模拟双击，因此需要区分出双击的前一次点击，不进行处理
        if (!hasScreenReader && isDoubleTapping)
            return false;

        //如果还没有播报结果就抬起了手指，则认为取消了激活或测量行为
        if (currentActivationState != ActivationState.FINISHED && currentActivationState != ActivationState.UNACTIVATED)
            tts.speak(getString(R.string.activation_canceled), TextToSpeech.QUEUE_FLUSH, null, "activationCanceled");

        endHolding();
        currentActivationState = ActivationState.UNACTIVATED;

        if (currentStudyState == FreeStudyState.WAIT_REPEATING)
            currentStudyState = FreeStudyState.REPEATING;

        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        if (pointerId == 0 && !isDoubleTapping)
            tts.speak(getString(R.string.gesture_detection_finish), TextToSpeech.QUEUE_FLUSH, null, "gestureDetectionFinish");
        else if (pointerId == 1) {
            currentFinger1.isValid = false;
            originalFinger1.isValid = false;
            finger1Ready = false;
        }
        else if (pointerId == 2) {
            currentFinger2.isValid = false;
            originalFinger2.isValid = false;
            finger2Ready = false;
        }

        if (spatialPose1 != null)
            spatialPose1.isValid = false;
        if (spatialPose2 != null)
            spatialPose2.isValid = false;

        return false;
    }

    @Override
    protected boolean onAllFingersUp(MotionEvent e) {
        onCertainFingerUp(e);
        currentMeasureMethod = SizeMeasureMethod.UNKNOWN;
        return false;
    }

    @Override
    protected void onTimerTick() {
        super.onTimerTick();

        //模拟双击时，如果未能在规定时间内完成第二次点击，则认为双击失败
        if (!hasScreenReader && isDoubleTapping && doubleTappingTime > doubleTappingTimeLimit) {
            endDoubleTapping();
        }

        //如果测距尚未激活，屏幕上只有第0根手指，且保持时间达标，则认为成功激活单手/双手/躯体移动测距或角度测量
        if (currentActivationState == ActivationState.WAITING_FINGER1 && holdingTime >= holdingTimeLimitB
                && (!finger1Ready && !finger2Ready)) {
            currentActivationState = ActivationState.ACTIVATED;
            tts.speak(getString(R.string.mixed_confirmed), TextToSpeech.QUEUE_FLUSH, null, "mixedConfirmed");
            spatialPose1.update(glView.getCameraTransform());
            originalPose.update(glView.getCameraTransform());
            startRealTimeMotionTracking();
            endHolding();
            currentMeasureMethod = SizeMeasureMethod.MIXED;
        }

        //如果测距尚未激活，屏幕上只有第0根和第1根手指，且时间达标，则认为成功激活了单指测距
        else if (currentActivationState == ActivationState.WAITING_FINGER2 && holdingTime >= holdingTimeLimitA
                && (finger1Ready && !finger2Ready)) {
            currentActivationState = ActivationState.ACTIVATED;
            tts.speak(getString(R.string.single_finger_confirmed), TextToSpeech.QUEUE_FLUSH, null, "singleFingerConfirmed");
            fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
            endHolding();
            currentMeasureMethod = SizeMeasureMethod.SINGLE_FINGER;
        }

        //如果测距尚未激活，屏幕上有第0、1、2根手指，且时间达标，则认为成功激活了双指测距
        else if (currentActivationState == ActivationState.FINGER_ACTIVATING) {
            currentActivationState = ActivationState.ACTIVATED;
            tts.speak(getString(R.string.two_fingers_confirmed), TextToSpeech.QUEUE_FLUSH, null, "twoFingersConfirmed");
            endHolding();
            currentMeasureMethod = SizeMeasureMethod.TWO_FINGERS;
        }

        //如果测距已激活，且当前是自由尝试环节，则需要在用户保持稳定之时播报结果
        else if (currentActivationState == ActivationState.ACTIVATED && currentStudyState == FreeStudyState.FREE_TRY) {
            currentPose.update(glView.getCameraTransform());
            if (holdingTime >= holdingTimeLimitC) {
                endHolding();
                if (currentMeasureMethod == SizeMeasureMethod.SINGLE_FINGER) {
                    fingerPosition2.update(currentFinger1.getX(), currentFinger1.getY());
                    handleMeasurement();
                }
                else if (currentMeasureMethod == SizeMeasureMethod.TWO_FINGERS) {
                    fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
                    fingerPosition2.update(currentFinger2.getX(), currentFinger2.getY());
                    handleMeasurement();
                }
            }
            if (currentPose != null && currentPose.isValid
                    && currentMeasureMethod != SizeMeasureMethod.SINGLE_FINGER && currentMeasureMethod != SizeMeasureMethod.TWO_FINGERS) {
                if (currentPose.calDistanceSquare(originalPose) > steadyDistanceLimit * steadyDistanceLimit) {
                    originalPose.update(glView.getCameraTransform());
                    startSteady();
                }
                //手机在某一位置保持稳定，则需要播报结果
                else if (steadyTime >= steadyTimeLimitA) {
                    spatialPose2 = recentPoses.getAveragePose();
                    if (spatialPose2 == null) {
                        tts.speak(getString(R.string.tracking_error), TextToSpeech.QUEUE_FLUSH, null, "trackingError");
                        return;
                    }
                    endRealTimeMotionTracking();
                    endSteady();
                    handleMeasurement();
                }
            }
        }

        //如果测距已激活，且当前是巩固练习环节，则需要实时监测当前情况并给予反馈
        //具体实现见FreeStudyActivity
    }

    //处理一次成功的测量，需要由FreeStudyActivity和TestActivity去具体实现
    protected void handleMeasurement() {};
}
