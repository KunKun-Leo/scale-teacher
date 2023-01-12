package com.pcg.scaleteacher.main;


import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FormalStudyBase;
import com.pcg.scaleteacher.helper.MeasureReportBuilder;

public class SizeFormalStudyActivity extends FormalStudyBase {

    TextView x;
    TextView y;
    TextView z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initMotionTracking();
    }

    private void initUI() {
        setContentView(R.layout.activity_size_formal_study);

        //当前测量方法标签加粗显示
        int viewId;
        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
                viewId = R.id.switch_to_single_finger;
                break;
            case SizeMeasureMethod.TWO_FINGERS:
                viewId = R.id.switch_to_two_fingers;
                break;
            case SizeMeasureMethod.ONE_HAND:
                viewId = R.id.switch_to_one_hand;
                break;
            case SizeMeasureMethod.TWO_HANDS:
                viewId = R.id.switch_to_two_hands;
                break;
            default:
                viewId = R.id.switch_to_body;
                break;
        }
        TextView activeTab = findViewById(viewId);
        activeTab.setTypeface(Typeface.create(activeTab.getTypeface(), Typeface.NORMAL), Typeface.BOLD);

        //学习目标显示
        TextView studyGoalValue = findViewById(R.id.study_goal_value);
        studyGoalValue.setText(studyGoal + "厘米");

        x = findViewById(R.id.x);
        y = findViewById(R.id.y);
        z = findViewById(R.id.z);
    }

    public void switchMeasureMethod(View view) {
        int viewId = view.getId();

        int targetMethod = SizeMeasureMethod.SINGLE_FINGER;
        if (viewId == R.id.switch_to_two_fingers)
            targetMethod = SizeMeasureMethod.TWO_FINGERS;
        else if (viewId == R.id.switch_to_one_hand)
            targetMethod = SizeMeasureMethod.ONE_HAND;
        else if (viewId == R.id.switch_to_two_hands)
            targetMethod = SizeMeasureMethod.TWO_HANDS;
        else if (viewId == R.id.switch_to_body)
            targetMethod = SizeMeasureMethod.BODY;

        if (currentMeasureMethod != targetMethod) {
            Intent intent = new Intent(this, SizeFormalStudyActivity.class);
            intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
            intent.putExtra(studyContentTag, StudyContent.STUDY_SIZE);
            intent.putExtra(sizeMeasureMethodTag, targetMethod);
            intent.putExtra(studyGoalTag, studyGoal);
            startActivity(intent);
            finish();
        }
    }

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

        //当开启了无障碍服务时，监听到第一根手指放下，意味着用户实际上双击了屏幕
        currentActivationState = ActivationState.WAITING_FINGER1;
        switch (currentMeasureMethod) {
            //对于单指和双指测距而言，此时仅意味着手势监测已就绪，还需要提醒用户按住当前手指，并使用其他手指操作
            case SizeMeasureMethod.SINGLE_FINGER:
            case SizeMeasureMethod.TWO_FINGERS:
                tts.speak(getString(R.string.gesture_detection_ready), TextToSpeech.QUEUE_FLUSH, null, "gestureDetectionReady");
                tts.speak(getString(R.string.put_finger_to_activate), TextToSpeech.QUEUE_ADD, null, "putFingerToActivate");
                return false;
            //对于单手/双手/躯体测距而言，此时仅需要提醒用户保持按住当前手指
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                tts.speak(getString(R.string.hold_to_activate), TextToSpeech.QUEUE_FLUSH, null, "holdToActivate");
                startHolding();
                return false;
            default:
                break;
        }

        return false;
    }

    @Override
    protected boolean onMoreFingersDown(MotionEvent e) {
        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        int pointerIndex = e.findPointerIndex(pointerId);

        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
                if (pointerId == 0)
                    return false;
                //当前放下的是（继用于双击并按住激活手势监测区域的第0根手指之后的）第1根手指
                if (pointerId == 1) {
                    tts.speak(getString(R.string.hold_current_finger), TextToSpeech.QUEUE_FLUSH, null, "holdCurrentFinger");
                    currentFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    originalFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    finger1Ready = true;
                    currentActivationState = ActivationState.WAITING_FINGER2;
                    startHolding();
                    return false;
                }
                //单指测量模式下，屏幕上最多放置2根手指
                break;
            case SizeMeasureMethod.TWO_FINGERS:
                if (pointerId == 0)
                    return false;
                //当前放下的是（继用于双击并按住激活手势监测区域的第0根手指之后的）第1根手指
                if (pointerId == 1) {
                    tts.speak(getString(R.string.wait_another_finger), TextToSpeech.QUEUE_FLUSH, null, "waitAnotherFinger");
                    currentFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    originalFinger1.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    finger1Ready = true;
                    currentActivationState = ActivationState.WAITING_FINGER2;
                    return false;
                }
                //当前放下的是第2根手指
                else if (pointerId == 2) {
                    tts.speak(getString(R.string.hold_current_finger), TextToSpeech.QUEUE_FLUSH, null, "holdCurrentFinger");
                    currentFinger2.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    originalFinger2.update(e.getX(pointerIndex), e.getY(pointerIndex));
                    finger2Ready = true;
                    currentActivationState = ActivationState.FINGER_ACTIVATING;
                    startHolding();
                    return false;
                }
                //双指测量模式下，屏幕上最多放置3根手指
                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                if (pointerId == 0)
                    return false;
                //单手/双手/躯体移动测距模式下，屏幕上最多放置1根手指
                break;
            default:
                break;
        }

        //能够运行到此处的，都说明当前放置的手指数超过了测量模式支持的手指数
        tts.speak(getString(R.string.too_many_fingers), TextToSpeech.QUEUE_FLUSH, null, "tooManyFingers");
        currentActivationState = ActivationState.UNACTIVATED;

        return false;
    }

    @Override
    protected boolean onFingerMove(MotionEvent e) {
        int finger1Index, finger2Index;
        if (finger1Ready) {
            finger1Index = e.findPointerIndex(1);
            currentFinger1.update(e.getX(finger1Index), e.getY(finger1Index));
        }
        if (finger2Ready) {
            finger2Index = e.findPointerIndex(2);
            currentFinger2.update(e.getX(finger2Index), e.getY(finger2Index));
        }

        int xDiff1, yDiff1, xDiff2, yDiff2;

        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
                xDiff1 = currentFinger1.getX() - originalFinger1.getX();
                yDiff1 = currentFinger1.getY() - originalFinger1.getY();
                //如果手指发生了大幅度移动，则会刷新判定基准位置和重置holdingTime计时
                if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit) {
                    originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                    startHolding();
                }
                //}
                break;
            case SizeMeasureMethod.TWO_FINGERS:
                xDiff1 = currentFinger1.getX() - originalFinger1.getX();
                yDiff1 = currentFinger1.getY() - originalFinger1.getY();
                xDiff2 = currentFinger2.getX() - originalFinger2.getX();
                yDiff2 = currentFinger2.getY() - originalFinger2.getY();
                if (finger1Ready
                        && (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit)) {
                    originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                    startHolding();
                }
                if (finger2Ready
                        && (Math.abs(xDiff2) > holdingPositionLimit || Math.abs(yDiff2) > holdingPositionLimit)){
                    originalFinger2.update(currentFinger2.getX(), currentFinger2.getY());
                    startHolding();
                }
//                }
                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    protected boolean onCertainFingerUp(MotionEvent e) {
        //未开启无障碍服务时需要模拟双击，因此需要区分出双击的前一次点击，不进行处理
        if (!hasScreenReader && isDoubleTapping)
            return false;

        //如果还没有播报结果就抬起了手指，则认为取消了激活或测量行为
        if ((currentActivationState != ActivationState.FINISHED && currentActivationState != ActivationState.UNACTIVATED)
                || currentStudyState == FormalStudyState.CORRECTING) {
            tts.speak(getString(R.string.activation_canceled), TextToSpeech.QUEUE_FLUSH, null, "activationCanceled");
        }

        endHolding();
        currentActivationState = ActivationState.UNACTIVATED;

        if (currentStudyState == FormalStudyState.CORRECTED)
            currentStudyState = FormalStudyState.PRACTICING;

        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        if (pointerId == 0)
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
        return onCertainFingerUp(e);
    }

    @Override
    protected void onTimerTick() {
        super.onTimerTick();

        if ((neededFunction == FunctionType.MOTION_TRACKING || neededFunction == FunctionType.ALL) && currentPose != null && glView != null) {
            currentPose.update(glView.getCameraTransform());
            x.setText(String.valueOf(currentPose.getSway()));
            y.setText(String.valueOf(currentPose.getHeave()));
            z.setText(String.valueOf(currentPose.getSurge()));
        }

        //模拟双击时，如果未能在规定时间内完成第二次点击，则认为双击失败
        if (!hasScreenReader && isDoubleTapping && doubleTappingTime > doubleTappingTimeLimit) {
            endDoubleTapping();
        }

        //如果测距尚未激活，屏幕上只有第0根手指，且保持时间达标，则认为成功激活单手/双手/躯体移动测距
        if (currentActivationState == ActivationState.WAITING_FINGER1 && holdingTime >= holdingTimeLimitB
                && (currentMeasureMethod == SizeMeasureMethod.ONE_HAND || currentMeasureMethod == SizeMeasureMethod.TWO_HANDS || currentMeasureMethod == SizeMeasureMethod.BODY)) {
            currentActivationState = ActivationState.ACTIVATED;
            if (currentStudyState == FormalStudyState.CORRECTING) {
                tts.speak(getString(R.string.start_size_correction), TextToSpeech.QUEUE_FLUSH, null, "startSizeCorrection");
                currentActivationState = ActivationState.FINISHED;      //矫正阶段激活状态都强制修改为FINISHED
            }
            else {
                int hintStringId = R.string.one_hand_activated;
                switch (currentMeasureMethod) {
                    case SizeMeasureMethod.TWO_HANDS:
                        hintStringId = R.string.two_hands_activated;
                        break;
                    case SizeMeasureMethod.BODY:
                        hintStringId = R.string.body_activated;
                        break;
                    default:
                        break;
                }
                tts.speak(getString(hintStringId), TextToSpeech.QUEUE_FLUSH, null, "moveMeasureActivated");
            }
            spatialPose1.update(glView.getCameraTransform());
            originalPose.update(glView.getCameraTransform());
            startRealTimeMotionTracking();
            endHolding();
        }

        //如果测距尚未激活，屏幕上只有第0根和第1根手指，且时间达标，则认为成功激活了单指测距
        else if (currentActivationState == ActivationState.WAITING_FINGER2 && holdingTime >= holdingTimeLimitA
                && currentMeasureMethod == SizeMeasureMethod.SINGLE_FINGER) {
            currentActivationState = ActivationState.ACTIVATED;
            if (currentStudyState == FormalStudyState.CORRECTING) {
                tts.speak(getString(R.string.start_size_correction), TextToSpeech.QUEUE_FLUSH, null, "startSizeCorrection");
                currentActivationState = ActivationState.FINISHED;      //矫正阶段激活状态都强制修改为FINISHED
            }
            else
                tts.speak(getString(R.string.single_finger_activated), TextToSpeech.QUEUE_FLUSH, null, "singleFingerActivated");
            fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
            endHolding();
        }

        //如果测距尚未激活，屏幕上有第0、1、2根手指，且时间达标，则认为成功激活了双指测距
        else if (currentActivationState == ActivationState.FINGER_ACTIVATING
                && currentMeasureMethod == SizeMeasureMethod.TWO_FINGERS) {
            currentActivationState = ActivationState.ACTIVATED;
            if (currentStudyState == FormalStudyState.CORRECTING) {
                tts.speak(getString(R.string.start_size_correction), TextToSpeech.QUEUE_FLUSH, null, "startSizeCorrection");
                currentActivationState = ActivationState.FINISHED;      //矫正阶段激活状态都强制修改为FINISHED
            }
            else
                tts.speak(getString(R.string.two_fingers_activated), TextToSpeech.QUEUE_FLUSH, null, "twoFingersActivated");
            endHolding();
        }

        //如果测距已激活
        else if (currentActivationState == ActivationState.ACTIVATED) {
            //手指在屏幕上位置的实时更新通过触摸事件解决，而运动跟踪结果的实时更新必须在此定时执行的函数中解决
            if (neededFunction == FunctionType.MOTION_TRACKING || neededFunction == FunctionType.ALL)
                currentPose.update(glView.getCameraTransform());
            //如果当前在初次尝试或反复练习阶段，则需要在保持静止位置判定
            if (currentStudyState == FormalStudyState.FIRST_TRY || currentStudyState == FormalStudyState.PRACTICING) {
                //手指静止时间达标（仅针对单指/双指测距）
                if (holdingTime >= holdingTimeLimitC) {
                    endHolding();
                    switch (currentMeasureMethod) {
                        case SizeMeasureMethod.SINGLE_FINGER:
                            fingerPosition2.update(currentFinger1.getX(), currentFinger1.getY());
                            tellSizeResult();
                            break;
                        case SizeMeasureMethod.TWO_FINGERS:
                            if (!finger2Ready)
                                break;
                            fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
                            fingerPosition2.update(currentFinger2.getX(), currentFinger2.getY());
                            tellSizeResult();
                            break;
                        default:
                            break;
                    }
                }
                //检查手机位置是否稳定（仅针对单手/双手/躯体移动测量）
                if (currentPose != null && currentPose.isValid) {
                    //Log.e("distanceSquare", currentPose.calDistanceSquare(originalPose) + "");
                    //手机位置相比于之前出现了较大偏移，则在新位置重新开始稳定性判定
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
                        tellSizeResult();
                    }
                }
            }
        }

        //如果当前正在矫正阶段，则需要实时监测当前做出的长度，以确认是否达标；并且在达标之后提示转入练习阶段
        else if (currentStudyState == FormalStudyState.CORRECTING && currentActivationState == ActivationState.FINISHED) {
            switch (currentMeasureMethod) {
                case SizeMeasureMethod.SINGLE_FINGER:
                    if (Math.abs(currentFinger1.calPhysicDistance(fingerPosition1, dpi) - studyGoal) < fingerToleranceA) {
                        Log.e("start Vibrator", currentFinger1.calPhysicDistance(fingerPosition1, dpi) + " " + studyGoal);
                        startVibrator();
                        if (Math.abs(currentFinger1.getX() - originalFinger1.getX()) > holdingPositionLimit
                                || Math.abs(currentFinger1.getY() - originalFinger1.getY()) > holdingPositionLimit) {
                            startHolding();
                            originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        }
                    }
                    if (holdingTime > holdingTimeLimitD) {
                        tts.speak(getString(R.string.start_size_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                        currentStudyState = FormalStudyState.CORRECTED;
                    }
                    break;
                case SizeMeasureMethod.TWO_FINGERS:
                    if (Math.abs(currentFinger1.calPhysicDistance(currentFinger2, dpi) - studyGoal) < fingerToleranceA) {
                        startVibrator();
                        if (Math.abs(currentFinger1.getX() - originalFinger1.getX()) > holdingPositionLimit
                                || Math.abs(currentFinger1.getY() - originalFinger1.getY()) > holdingPositionLimit) {
                            startHolding();
                            originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        }
                        if (Math.abs(currentFinger2.getX() - originalFinger2.getX()) > holdingPositionLimit
                                || Math.abs(currentFinger2.getY() - originalFinger2.getY()) > holdingPositionLimit) {
                            startHolding();
                            originalFinger2.update(currentFinger2.getX(), currentFinger2.getY());
                        }
                    }
                    if (holdingTime > holdingTimeLimitD) {
                        tts.speak(getString(R.string.start_size_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                        currentStudyState = FormalStudyState.CORRECTED;
                    }
                    break;
                case SizeMeasureMethod.ONE_HAND:
                case SizeMeasureMethod.BODY:
                    if (Math.abs(Math.sqrt(currentPose.calDistanceSquare(spatialPose1)) - studyGoal) < spatialToleranceA) {
                        startVibrator();
                        if (!isSteady)
                            startSteady();
                        if (Math.sqrt(currentPose.calDistanceSquare(originalPose)) > steadyDistanceLimit * steadyDistanceLimit) {
                            originalPose.update(glView.getCameraTransform());
                            startSteady();
                        }
                    }
                    if (steadyTime > steadyTimeLimitB) {
                        tts.speak(getString(R.string.start_size_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                        endSteady();
                        endRealTimeMotionTracking();
                        currentStudyState = FormalStudyState.CORRECTED;
                    }
                    break;
                case SizeMeasureMethod.TWO_HANDS:
                    //
                    //
                    // 待补充
                    //
                    //
                    break;
                default:
                    break;
            }
        }
    }

    //播报测量结果
    private void tellSizeResult() {
        TextView lastTryValue = findViewById(R.id.last_try_value);
        float distance = (float) studyGoal;   //后续将会替换为实际值
        float tolerance = 0f;

        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
            case SizeMeasureMethod.TWO_FINGERS:
                if (!fingerPosition1.isValid || !fingerPosition2.isValid) {
                    tts.speak(getString(R.string.unknown_problem), TextToSpeech.QUEUE_FLUSH, null, "unknownProblem");
                    return;
                }
                distance = fingerPosition1.calPhysicDistance(fingerPosition2, dpi);
                tolerance = fingerToleranceA;
                lastTryValue.setText(String.format("%.1f厘米", distance));

                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.BODY:
                if (!spatialPose1.isValid || !spatialPose2.isValid) {
                    Log.e("Debug", String.valueOf(spatialPose1.isValid) + " " + String.valueOf(spatialPose2.isValid));
                    tts.speak(getString(R.string.unknown_problem), TextToSpeech.QUEUE_FLUSH, null, "unknownProblem");
                    return;
                }

                //Log.e("pose1", spatialPose1.getSway() + " " + spatialPose1.getHeave() + " " + spatialPose1.getSurge());
                //Log.e("pose2", spatialPose2.getSway() + " " + spatialPose2.getHeave() + " " + spatialPose2.getSurge());
                //Log.e("distanceSquare", "" + spatialPose1.calDistanceSquare(spatialPose2));
                distance = (float) Math.sqrt(spatialPose1.calDistanceSquare(spatialPose2));
                tolerance = spatialToleranceA;
                lastTryValue.setText(String.format("%.0f厘米", distance));
                break;
            case SizeMeasureMethod.TWO_HANDS:
                break;
            default:
                break;
        }

        tts.speak(MeasureReportBuilder.buildSizeReport(distance, studyGoal, currentMeasureMethod),
                TextToSpeech.QUEUE_FLUSH, null, "result");

        boolean isSuccessful = (Math.abs(distance - studyGoal) <= tolerance);
        if (currentStudyState == FormalStudyState.FIRST_TRY) {
            //如果第一次尝试就成功了，则直接转入巩固练习阶段
            if (isSuccessful) {
                tts.speak(getString(R.string.start_size_practice), TextToSpeech.QUEUE_ADD, null, "startPractice");
                currentStudyState = FormalStudyState.CORRECTED;
            }
            //如果第一次尝试失败了，则转入矫正阶段
            else {
                tts.speak(getString(R.string.start_size_correction), TextToSpeech.QUEUE_ADD, null, "startSizeCorrection");
                currentStudyState = FormalStudyState.CORRECTING;
            }
            currentActivationState = ActivationState.FINISHED;
            practiceSuccessCounter = 0;
        }
        else if (currentStudyState == FormalStudyState.PRACTICING) {
            if (isSuccessful) {
                practiceSuccessCounter++;
                if (practiceSuccessCounter >= practiceSuccessLimit) {
                    tts.speak(getString(R.string.formal_study_success), TextToSpeech.QUEUE_ADD, null, "formalStudySuccess");
                    currentActivationState = ActivationState.FINISHED;
                    return;
                }
            }
            tts.speak(getString(R.string.practice_again), TextToSpeech.QUEUE_ADD, null, "practiceAgain");
            currentActivationState = ActivationState.FINISHED;
        }
    }
}