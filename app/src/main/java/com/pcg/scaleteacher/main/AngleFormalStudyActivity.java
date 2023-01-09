package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FormalStudyBase;
import com.pcg.scaleteacher.helper.MeasureReportBuilder;

public class AngleFormalStudyActivity extends FormalStudyBase {

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
    }

    @Override
    protected boolean onFirstFingerDown(MotionEvent e) {
        if (hasScreenReader) {
            tts.speak(getString(R.string.gesture_detection_ready), TextToSpeech.QUEUE_FLUSH, null, "gestureDetectionReady");
            return false;
        }

        int pointerIndex = e.findPointerIndex(finger1Id);
        float fingerX = e.getX(pointerIndex);
        float fingerY = e.getY(pointerIndex);
        currentFinger1.update(fingerX, fingerY);
        originalFinger1.update(fingerX, fingerY);
        currentActivationState = ActivationState.FINGER_ACTIVATING;
        finger1Ready = true;

        tts.speak(getString(R.string.wait_sweep_horizontally), TextToSpeech.QUEUE_FLUSH, null, "waitSweepHorizontally");
        return false;
    }

    @Override
    protected boolean onMoreFingersDown(MotionEvent e) {
        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        int pointerIndex = e.findPointerIndex(pointerId);

        //如果没有开启无障碍服务，理论上不会进入pointerId==finger1Id的情况
        if (pointerId == finger1Id) {
            float fingerX = e.getX(pointerIndex);
            float fingerY = e.getY(pointerIndex);
            currentFinger1.update(fingerX, fingerY);
            originalFinger1.update(fingerX, fingerY);
            currentActivationState = ActivationState.FINGER_ACTIVATING;
            finger1Ready = true;

            tts.speak(getString(R.string.wait_sweep_horizontally), TextToSpeech.QUEUE_FLUSH, null, "waitSweepHorizontally");
        }
        else if (pointerId > finger1Id) {
            currentActivationState = ActivationState.UNACTIVATED;
            tts.speak(getString(R.string.too_many_fingers), TextToSpeech.QUEUE_FLUSH, null, "tooManyFingers");
        }

        return false;
    }

    @Override
    protected boolean onFingerMove(MotionEvent e) {
        int finger1Index;
        if (finger1Ready) {
            finger1Index = e.findPointerIndex(finger1Id);
            currentFinger1.update(e.getX(finger1Index), e.getY(finger1Index));
        }
        else
            return false;

        int xDiff = currentFinger1.getX() - originalFinger1.getY();
        int yDiff = currentFinger1.getY() - originalFinger1.getY();

        //如果测距还未激活，并且手指有明显左右平移，则需要在新位置等待一定事件后激活
        if (currentActivationState == ActivationState.FINGER_ACTIVATING
                && Math.abs(xDiff) > sweepPositionLimit) {
            startHolding();
            originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
            currentActivationState = ActivationState.MOVE_ACTIVATING;
        }
        return false;
    }

    @Override
    protected boolean onCertainFingerUp(MotionEvent e) {
        //如果还没有激活就抬起了手指，则认为取消了激活行为
        if (currentActivationState != ActivationState.ACTIVATED) {
            tts.speak(getString(R.string.activation_canceled), TextToSpeech.QUEUE_FLUSH, null, "activationCanceled");
        }

        endHolding();
        currentActivationState = ActivationState.UNACTIVATED;

        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        if (pointerId == finger1Id) {
            currentFinger1.isValid = false;
            originalFinger1.isValid = false;
            finger1Ready = false;
        }

        if (spatialPose1 != null)
            spatialPose1.isRotationValid = false;
        if (spatialPose2 != null)
            spatialPose2.isRotationValid = false;

        return false;
    }

    @Override
    protected boolean onAllFingersUp(MotionEvent e) {
        return onCertainFingerUp(e);
    }

    @Override
    protected void onTimerTick() {
        super.onTimerTick();

        //如果当前正在激活阶段，并且手指在要求保持不动的位置保持了一定时间，则判定测距功能激活成功
        if (currentActivationState == ActivationState.MOVE_ACTIVATING
                && holdingTime >= holdingTimeLimitB) {
            tts.speak(getString(R.string.angle_activated), TextToSpeech.QUEUE_FLUSH, null, "angleActivated");
            spatialPose1.updateRotation(glView.getQuaternion());
            originalPose.updateLocation(glView.getTranslation());
            startRealTimeMotionTracking();
            endHolding();
            currentActivationState = ActivationState.ACTIVATED;
        }

        if (currentActivationState == ActivationState.ACTIVATED) {
            //运动跟踪结果的实时更新在定时执行的本函数中解决
            currentPose.updateLocation(glView.getTranslation());
            currentPose.updateRotation(glView.getQuaternion());
            //如果当前在初次尝试或反复练习阶段，且测距功能已激活，则需要在保持静止位置判定
            if (currentStudyState == StudyState.FIRST_TRY || currentStudyState == StudyState.PRACTICING) {
                if (currentPose.calDistanceSquare(originalPose) > steadyDistanceLimit * steadyDistanceLimit) {
                    originalPose.updateLocation(glView.getTranslation());
                    startSteady();
                } else if (steadyTime >= steadyTimeLimitA) {
                    spatialPose2 = recentPoses.getAveragePose();
                    endRealTimeMotionTracking();
                    endSteady();
                    tellAngleResult();
                }
            }
        }
        //如果当前正在矫正阶段且测距功能已激活，则需要实时监测当前做出的长度，以确认是否达标；并且在达标之后提示转入练习阶段
        if (currentStudyState == StudyState.CORRECTING) {
            if (Math.abs(currentPose.calAngleChange(spatialPose1) - studyGoal) < angleToleranceA) {
                startVibrator();
                if (Math.sqrt(currentPose.calDistanceSquare(originalPose)) > steadyDistanceLimit * steadyDistanceLimit) {
                    originalPose.updateLocation(glView.getTranslation());
                    startSteady();
                }
            }
            if (steadyTime > steadyTimeLimitB) {
                tts.speak(getString(R.string.start_angle_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                endSteady();
                endRealTimeMotionTracking();
                currentStudyState = StudyState.CORRECTED;
            }
        }
    }

    private void tellAngleResult() {
        TextView lastTryValue = findViewById(R.id.last_try_value);
        int rotation = spatialPose1.calAngleChange(spatialPose2);
        lastTryValue.setText(String.format("%d度", rotation));
        tts.speak(MeasureReportBuilder.buildAngleFormalReport(rotation, studyGoal),
                TextToSpeech.QUEUE_FLUSH, null, "result");
        if (Math.abs(rotation - studyGoal) <= angleToleranceA) {
            tts.speak(getString(R.string.formal_study_success), TextToSpeech.QUEUE_ADD, null, "formalStudySuccess");
            return;
        }
        else if (currentStudyState == StudyState.FIRST_TRY) {
            tts.speak(getString(R.string.start_angle_correction), TextToSpeech.QUEUE_ADD, null, "");
            currentStudyState = StudyState.CORRECTING;
            practiceCounter = 0;
        }
        else if (currentStudyState == StudyState.PRACTICING) {
            practiceCounter++;
            if (practiceCounter >= practiceLimit) {
                tts.speak(getString(R.string.formal_study_complete), TextToSpeech.QUEUE_ADD, null, "formalStudyComplete");
            }
            else {
                tts.speak(getString(R.string.practice_again), TextToSpeech.QUEUE_ADD, null, "practiceAgain");
            }
        }
    }
}