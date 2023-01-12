package com.pcg.scaleteacher.main;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FreeStudyAndTestBase;

public class FreeStudyActivity extends FreeStudyAndTestBase {

    private int practiceGoal;   //巩固重复阶段的目标
    private int practiceContent;    //StudyContent可选值，指示巩固重复阶段的目标是尺寸还是角度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initMotionTracking();
    }

    private void initUI() {
        setContentView(R.layout.activity_free_study);
    }

    @Override
    protected void onTimerTick() {
        super.onTimerTick();

        //如果当前是巩固练习阶段，还需要实时监测并给予振动提示
        if (currentActivationState == ActivationState.ACTIVATED && currentStudyState == FreeStudyState.REPEATING) {
            currentPose.update(glView.getCameraTransform());
            float distanceBetweenHands = getDistanceBetweenHands();
            switch (currentMeasureMethod) {
                case SizeMeasureMethod.SINGLE_FINGER:
                    if (Math.abs(currentFinger1.calPhysicDistance(fingerPosition1, dpi) - practiceGoal) < fingerToleranceA) {
                        startVibrator();
                        if (Math.abs(currentFinger1.getX() - originalFinger1.getX()) > holdingPositionLimit
                                || Math.abs(currentFinger1.getY() - originalFinger1.getY()) > holdingPositionLimit) {
                            startHolding();
                            originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        }
                    }
                    if (holdingTime > holdingTimeLimitD)
                        handlePractice();
                    break;
                case SizeMeasureMethod.TWO_FINGERS:
                    if (Math.abs(currentFinger1.calPhysicDistance(currentFinger2, dpi) - practiceGoal) < fingerToleranceA)
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
                    if (holdingTime > holdingTimeLimitD)
                        handlePractice();
                case SizeMeasureMethod.MIXED:
                case SizeMeasureMethod.TWO_HANDS:
                case SizeMeasureMethod.ONE_HAND:
                case SizeMeasureMethod.BODY:
                case AngleMeasureMethod.ANGLE:
                    //如果当前正在重复巩固角度，则直接认为进行角度监测
                    if (practiceContent == StudyContent.STUDY_ANGLE) {
                        if (Math.abs(currentPose.calAngleChange(spatialPose1) - practiceGoal) < angleToleranceA) {
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
                            handlePractice();
                        }
                        break;
                    }
                    //如果正在重复巩固长度，先判定是不是在用两手测距
                    else if (distanceBetweenHands > 0) {
                        //
                        //
                        //待补充
                        //
                        //
                    }
                    else {
                        if (Math.abs(Math.sqrt(currentPose.calDistanceSquare(spatialPose1)) - practiceGoal) < spatialToleranceA) {
                            startVibrator();
                            if (!isSteady)
                                startSteady();
                            if (Math.sqrt(currentPose.calDistanceSquare(originalPose)) > steadyDistanceLimit * steadyDistanceLimit) {
                                originalPose.update(glView.getCameraTransform());
                                startSteady();
                            }
                        }
                    }
                    if (steadyTime > steadyTimeLimitB) {
                        tts.speak(getString(R.string.start_size_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                        endSteady();
                        endRealTimeMotionTracking();
                        handlePractice();
                    }
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    protected void handleMeasurement() {
        TextView lastMeasureValue = findViewById(R.id.last_measure_value);

        float result = 0f;
        practiceContent = StudyContent.STUDY_SIZE;

        //如果已确定为单指测量或双指测量
        if (currentMeasureMethod == SizeMeasureMethod.SINGLE_FINGER || currentMeasureMethod == SizeMeasureMethod.TWO_FINGERS) {
            result = fingerPosition1.calPhysicDistance(fingerPosition2, dpi);
            lastMeasureValue.setText(String.format("%.1f厘米", result));
            tts.speak(String.format("本次测得%.1f厘米", result), TextToSpeech.QUEUE_FLUSH, null, "report");
        }

        //如果初步判定为单手/双手/躯体移动测量或角度测量
        else if (currentMeasureMethod == SizeMeasureMethod.MIXED) {
            //尝试获取两手间距离
            result = getDistanceBetweenHands();
            //如果尝试获得的两手间距离为正数，说明视野里有手，当前用户采用的是双手测距；否则意味着实际上视野里没有手
            if (result > 0) {
                currentMeasureMethod = SizeMeasureMethod.TWO_HANDS;
                lastMeasureValue.setText(String.format("%.0f厘米", result));
                tts.speak(String.format("本次测得%.0f厘米", result), TextToSpeech.QUEUE_FLUSH, null, "report");
            }
            else {
                //尝试获取角度变化
                result = spatialPose1.calAngleChange(spatialPose2);
                //如果手机平行与地面并且有明显角度变化，则认为是在测量角度，否则意味着在使用单手/躯体移动测量
                if (isPoseForAngle() && result > rotationDetectionLimit) {
                    currentMeasureMethod = AngleMeasureMethod.ANGLE;
                    lastMeasureValue.setText(String.format("%.0f度", result));
                    tts.speak(String.format("本次测得%.0f度", result), TextToSpeech.QUEUE_FLUSH, null, "report");
                    practiceContent = StudyContent.STUDY_ANGLE;
                }
                else {
                    result = (float) Math.sqrt(spatialPose1.calDistanceSquare(spatialPose2));
                    currentMeasureMethod = SizeMeasureMethod.ONE_HAND;
                    lastMeasureValue.setText(String.format("%.0f厘米", result));
                    tts.speak(String.format("本次测得%.0f厘米", result), TextToSpeech.QUEUE_FLUSH, null, "report");
                }
            }
        }

        currentActivationState = ActivationState.FINISHED;

        //如果刚刚获取到的是自由尝试的结果，则需要提醒用户再巩固练习两次
        if (currentStudyState == FreeStudyState.FREE_TRY) {
            tts.speak(getString(R.string.start_repeating), TextToSpeech.QUEUE_ADD, null, "startRepeating");
            currentStudyState = FreeStudyState.WAIT_REPEATING;
            practiceGoal = Integer.parseInt(String.format("%.0f", result));

            //为了方便用户不必松开现有手指，直接调整当前激活状态
            if (currentMeasureMethod != SizeMeasureMethod.SINGLE_FINGER && currentMeasureMethod != SizeMeasureMethod.TWO_FINGERS) {
                currentActivationState = ActivationState.WAITING_FINGER1;
                startHolding();
            }
        }
    }

    private void handlePractice() {
        repeatingCounter++;
        //重复巩固还没有结束
        if (repeatingCounter < repeatingLimit) {
            if (currentMeasureMethod == AngleMeasureMethod.ANGLE)
                tts.speak(getString(R.string.start_angle_repeating_again), TextToSpeech.QUEUE_FLUSH, null, "startAngleRepeatingAgain");
            else
                tts.speak(getString(R.string.start_size_repeating_again), TextToSpeech.QUEUE_FLUSH, null, "startSizeRepeatingAgain");
            currentStudyState = FreeStudyState.WAIT_REPEATING;
        }
        //重复巩固已经结束
        else {
            tts.speak(getString(R.string.repeating_finished), TextToSpeech.QUEUE_FLUSH, null, "repeatingFinished");
            currentStudyState = FreeStudyState.FREE_TRY;
            repeatingCounter = 0;
            //
            //
            //待完成：将本次学习记录到本地
            //
            //
        }
    }
}