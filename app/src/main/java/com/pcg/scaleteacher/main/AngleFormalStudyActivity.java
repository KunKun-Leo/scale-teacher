package com.pcg.scaleteacher.main;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FormalStudyBase;
import com.pcg.scaleteacher.helper.MeasureReportBuilder;

public class AngleFormalStudyActivity extends FormalStudyBase {

    TextView yaw;
    TextView pitch;
    TextView roll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initMotionTracking();
    }

    private void initUI() {
        setContentView(R.layout.activity_angle_formal_study);
        //学习目标显示
        TextView studyGoalValue = findViewById(R.id.study_goal_value);
        studyGoalValue.setText(studyGoal + "度");

        yaw = findViewById(R.id.yaw);
        pitch = findViewById(R.id.pitch);
        roll = findViewById(R.id.roll);
    }

    @Override
    protected boolean onFirstFingerDown(MotionEvent e) {
        //未开启无障碍服务时，需要模拟监听双击
        if (!hasScreenReader) {
            //当前点击是双击的后一次点击且间隔时间达标，意味着成功激活手指监测区域
            if (isDoubleTapping && doubleTappingTime <= doubleTappingTimeLimit) {
                endDoubleTapping();
            }
            //当前点击为双击的前一次点击，需要等待下一次点击
            else {
                startDoubleTapping();
                return false;
            }
        }

        //当开启了无障碍服务时，监听到第一根手指放下，意味着用户实际上双击了屏幕，此时需要提醒用户保持并按住当前手指和调整手机方向
        currentActivationState = ActivationState.WAITING_FINGER1;
        tts.speak(getString(R.string.hold_to_activate), TextToSpeech.QUEUE_FLUSH, null, "hold_to_activate");
        tts.speak(getString(R.string.parallel_phone_and_ground), TextToSpeech.QUEUE_ADD, null, "parallelPhoneAndGround");
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
        //当前模式只支持在屏幕上放下第0根手指
        else {
            tts.speak(getString(R.string.too_many_fingers), TextToSpeech.QUEUE_FLUSH, null, "tooManyFingers");
            currentActivationState = ActivationState.UNACTIVATED;
        }

        return false;
    }

    @Override
    protected boolean onFingerMove(MotionEvent e) {
        //当前模式不监听手势移动
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

        //如果还没有激活就抬起了手指，则认为取消了激活行为
        if (currentActivationState != ActivationState.ACTIVATED) {
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

        if (glView != null) {
            float[] rotation = glView.getCameraRotation();
            yaw.setText(String.valueOf(rotation[0]));
            pitch.setText(String.valueOf(rotation[1]));
            roll.setText(String.valueOf(rotation[2]));
        }

        //模拟双击时，如果未能在规定时间内完成第二次点击，则认为双击失败
        if (!hasScreenReader && isDoubleTapping && doubleTappingTime > doubleTappingTimeLimit) {
            endDoubleTapping();
        }

        //如果测距尚未激活，屏幕上只有第0根手指，且保持时间达标，则认为成功激活单手/双手/躯体移动测距
        if (currentActivationState == ActivationState.WAITING_FINGER1 && holdingTime >= holdingTimeLimitB) {
            currentActivationState = ActivationState.ACTIVATED;
            if (currentStudyState == FormalStudyState.CORRECTING) {
                tts.speak(getString(R.string.start_angle_correction), TextToSpeech.QUEUE_FLUSH, null, "startAngleCorrection");
                currentActivationState = ActivationState.FINISHED;      //矫正阶段激活状态都强制修改为FINISHED
            }
            else
                tts.speak(getString(R.string.angle_activated), TextToSpeech.QUEUE_FLUSH, null, "angleActivated");
            spatialPose1.update(glView.getCameraTransform());
            originalPose.update(glView.getCameraTransform());
            startRealTimeMotionTracking();
            endHolding();
        }

        //如果测距已激活
        else if (currentActivationState == ActivationState.ACTIVATED) {
            currentPose.update(glView.getCameraTransform());
            //如果当前在初次尝试或反复练习阶段，则需要在保持静止位置判定
            if (currentStudyState == FormalStudyState.FIRST_TRY || currentStudyState == FormalStudyState.PRACTICING) {
                //检查手机位置是否稳定（仅针对单手/双手/躯体移动测量）
                if (currentPose != null && currentPose.isValid) {
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
                        tellAngleResult();
                    }
                }
            }
        }

        //如果当前正在矫正阶段，则需要实时监测当前做出的角度，以确认是否达标；并且在达标之后提示转入练习阶段
        else if (currentStudyState == FormalStudyState.CORRECTING && currentActivationState == ActivationState.FINISHED) {
            currentPose.update(glView.getCameraTransform());
            //Log.e("Debug", "" + Math.abs(currentPose.calAngleChange(spatialPose1) - studyGoal));
            if (Math.abs(currentPose.calAngleChange(spatialPose1) - studyGoal) < angleToleranceA) {
                startVibrator();
                if (!isSteady)
                    startSteady();
                if (Math.sqrt(currentPose.calDistanceSquare(originalPose)) > steadyDistanceLimit * steadyDistanceLimit) {
                    originalPose.update(glView.getCameraTransform());
                    startSteady();
                }
            }
            if (steadyTime > steadyTimeLimitB) {
                tts.speak(getString(R.string.start_angle_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                endSteady();
                endRealTimeMotionTracking();
                currentStudyState = FormalStudyState.CORRECTED;
            }
        }


    }

    private void tellAngleResult() {
        TextView lastTryValue = findViewById(R.id.last_try_value);
        float rotation = spatialPose1.calAngleChange(spatialPose2);
        lastTryValue.setText(String.format("%.0f度", rotation));
        tts.speak(MeasureReportBuilder.buildAngleReport(rotation, studyGoal),
                TextToSpeech.QUEUE_FLUSH, null, "result");

        boolean isSuccessful = (Math.abs(rotation - studyGoal) <= angleToleranceA);
        if (currentStudyState == FormalStudyState.FIRST_TRY) {
            //如果第一次尝试就成功了，则直接转入巩固练习阶段
            if (isSuccessful) {
                tts.speak(getString(R.string.start_angle_practice), TextToSpeech.QUEUE_ADD, null, "startPractice");
                currentStudyState = FormalStudyState.CORRECTED;
            }
            //如果第一次尝试失败了，则转入矫正阶段
            else {
                tts.speak(getString(R.string.start_angle_correction), TextToSpeech.QUEUE_ADD, null, "startAngleCorrection");
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