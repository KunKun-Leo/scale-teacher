package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FingerPosition;
import com.pcg.scaleteacher.base.FormalStudyBase;
import com.pcg.scaleteacher.helper.MeasureReportBuilder;

public class SizeFormalStudyActivity extends FormalStudyBase {

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
        float fingerX = e.getX();
        float fingerY = e.getY();
        currentFinger1.update(fingerX, fingerY);
        originalFinger1.update(fingerX, fingerY);
        currentActivationState = ActivationState.FINGER_ACTIVATING;
        fingerCounter++;

        switch (currentMeasureMethod) {
            //如果当前是单手测量，则放下第一根手指之后就开始对holdingTime计时
            case SizeMeasureMethod.SINGLE_FINGER:
                startHolding();
                tts.speak(getString(R.string.hold_single_finger), TextToSpeech.QUEUE_FLUSH, null, "holdSingleFinger");
                break;
            //如果当前是双手测量，则提醒放下第二根手指
            case SizeMeasureMethod.TWO_FINGERS:
                tts.speak(getString(R.string.wait_second_finger), TextToSpeech.QUEUE_FLUSH, null, "waitSecondFinger");
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    protected boolean onMoreFingersDown(MotionEvent e) {
        fingerCounter++;

        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        int pointerIndex = e.findPointerIndex(pointerId);

        //如果当前是双指测量模式，且放下的是第二根手指，则开始计数
        if (pointerId == 1 && currentMeasureMethod == SizeMeasureMethod.TWO_FINGERS) {
            startHolding();
            originalFinger2.update(e.getX(pointerIndex), e.getY(pointerIndex));
            currentActivationState = ActivationState.FINGER_ACTIVATING;
            tts.speak(getString(R.string.holding_two_fingers), TextToSpeech.QUEUE_FLUSH, null, "holdTwoFingers");
        }
        //否则放下的手指数超出了当前模式的支持个数
        else {
            endHolding();
            currentActivationState = ActivationState.UNACTIVATED;
            tts.speak(getString(R.string.too_many_fingers), TextToSpeech.QUEUE_FLUSH, null, "tooManyFingers");
        }
        return false;
    }

    @Override
    protected boolean onFingerMove(MotionEvent e) {
        int finger1Index, finger2Index;
        if (fingerCounter > 0) {
            finger1Index = e.findPointerIndex(0);
            currentFinger1.update(e.getX(finger1Index), e.getY(finger1Index));
        }
        if (fingerCounter > 1) {
            finger2Index = e.findPointerIndex(1);
            currentFinger2.update(e.getX(finger2Index), e.getY(finger2Index));
        }

        int xDiff1, yDiff1, xDiff2, yDiff2;

        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
                xDiff1 = currentFinger1.getX() - originalFinger1.getX();
                yDiff1 = currentFinger1.getY() - originalFinger1.getY();
                //如果在单指测距等待激活的阶段手指发生了明显位移，则认为激活失败
                if (currentActivationState == ActivationState.FINGER_ACTIVATING) {
                    if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit) {
                        tts.speak(getString(R.string.invalid_gesture), TextToSpeech.QUEUE_FLUSH, null, "invalidGesture");
                        currentActivationState = ActivationState.UNACTIVATED;
                        endHolding();
                        return false;
                    }
                }
                //如果已成功激活了单指测距功能，则每次大幅度移动都会刷新判定基准位置和重置holdingTime计时
                if (currentActivationState == ActivationState.ACTIVATED) {
                    if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit) {
                        originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        startHolding();
                    }
                }
                break;
            case SizeMeasureMethod.TWO_FINGERS:
                xDiff1 = currentFinger1.getX() - originalFinger1.getX();
                yDiff1 = currentFinger1.getY() - originalFinger1.getY();
                xDiff2 = currentFinger2.getX() - originalFinger2.getX();
                yDiff2 = currentFinger2.getY() - originalFinger2.getY();
                //如果在双指测距等待激活的阶段手指发生了明显位移，则认为激活失败
                if (currentActivationState == ActivationState.FINGER_ACTIVATING) {
                    if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit
                            || ( fingerCounter > 1 && (Math.abs(xDiff2) > holdingPositionLimit || Math.abs(yDiff2) > holdingPositionLimit))) {
                        Log.e("Debug", "" + xDiff1 + " " + yDiff1 + " " + xDiff2 + " " + yDiff2);
                        tts.speak(getString(R.string.invalid_gesture), TextToSpeech.QUEUE_FLUSH, null, "invalidGesture");
                        currentActivationState = ActivationState.UNACTIVATED;
                        endHolding();
                        return false;
                    }
                }
                //如果已成功激活了双指测距功能，则每次大幅度移动都会刷新判定基准位置和重置holdingTime计时
                if (currentActivationState == ActivationState.ACTIVATED) {
                    if (Math.abs(xDiff1) > holdingPositionLimit || Math.abs(yDiff1) > holdingPositionLimit) {
                        originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        startHolding();
                    }
                    if (fingerCounter > 1
                            && (Math.abs(xDiff2) > holdingPositionLimit || Math.abs(yDiff2) > holdingPositionLimit)){
                        originalFinger2.update(currentFinger2.getX(), currentFinger2.getY());
                        startHolding();
                    }
                }
                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                yDiff1 = currentFinger1.getY() - originalFinger1.getY();
                //测距尚未激活时
                if (currentActivationState == ActivationState.FINGER_ACTIVATING) {
                    //如果手指有明显上移，则认为手势错误
                    if (yDiff1 < -holdingPositionLimit) {
                        tts.speak(getString(R.string.invalid_gesture), TextToSpeech.QUEUE_FLUSH, null, "invalidGesture");
                        currentActivationState = ActivationState.UNACTIVATED;
                        return false;
                    }
                    //如果手指有明显下移，则需要在新位置等待一定时间后激活
                    if (yDiff1 > sweepPositionLimit) {
                        startHolding();
                        originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                        currentActivationState = ActivationState.MOVE_ACTIVATING;
                    }
                }
                //如果测距已经激活，单手/双手/躯体模式下不再监测手指移动
                break;
            default:
                break;
        }

        return false;
    }

    @Override
    protected boolean onCertainFingerUp(MotionEvent e) {
        fingerCounter = fingerCounter > 0 ? fingerCounter - 1 : 0;

        //如果还没有激活就抬起了手指，则认为取消了激活行为
        if (currentActivationState != ActivationState.ACTIVATED) {
            tts.speak(getString(R.string.activation_canceled), TextToSpeech.QUEUE_FLUSH, null, "activationCanceled");
        }

        endHolding();
        currentActivationState = ActivationState.UNACTIVATED;

        if (currentStudyState == StudyState.CORRECTED)
            currentStudyState = StudyState.PRACTICING;

        int index = e.getActionIndex();
        int pointerId = e.getPointerId(index);
        if (pointerId == 0) {
            currentFinger1.isValid = false;
            originalFinger1.isValid = false;
        }
        else if (pointerId == 1) {
            currentFinger2.isValid = false;
            originalFinger2.isValid = false;
        }

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
        if (currentActivationState == ActivationState.FINGER_ACTIVATING
        && holdingTime >= holdingTimeLimitA) {
            switch (currentMeasureMethod) {
                case SizeMeasureMethod.SINGLE_FINGER:
                    tts.speak(getString(R.string.single_finger_activated), TextToSpeech.QUEUE_FLUSH, null, "singleFingerActivated");
                    //单指测量的激活位置，就是两点测距的起始位置
                    fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
                    break;
                case SizeMeasureMethod.TWO_FINGERS:
                    tts.speak(getString(R.string.two_fingers_activated), TextToSpeech.QUEUE_FLUSH, null, "twoFingersActivated");
                    break;
                default:
                    break;
            }
            endHolding();
            currentActivationState = ActivationState.ACTIVATED;
        }
        else if (currentActivationState == ActivationState.MOVE_ACTIVATING
        && holdingTime >= holdingTimeLimitB) {
            switch (currentMeasureMethod) {
                case SizeMeasureMethod.ONE_HAND:
                    tts.speak(getString(R.string.one_hand_activated), TextToSpeech.QUEUE_FLUSH, null, "oneHandActivated");
                    break;
                case SizeMeasureMethod.TWO_HANDS:
                    tts.speak(getString(R.string.two_hands_activated), TextToSpeech.QUEUE_FLUSH, null, "twoHandsActivated");
                    break;
                case SizeMeasureMethod.BODY:
                    tts.speak(getString(R.string.body_activated), TextToSpeech.QUEUE_FLUSH, null, "bodyActivated");
                    break;
                default:
                    break;
            }
            endHolding();
            currentActivationState = ActivationState.ACTIVATED;
        }


        if (currentActivationState == ActivationState.ACTIVATED) {
            //如果当前在初次尝试或反复练习阶段，且测距功能已激活，则需要在保持静止位置判定
            if (currentStudyState == StudyState.FIRST_TRY || currentStudyState == StudyState.PRACTICING) {
                if (holdingTime >= holdingTimeLimitC) {
                    endHolding();
                    switch (currentMeasureMethod) {
                        case SizeMeasureMethod.SINGLE_FINGER:
                            fingerPosition2.update(currentFinger1.getX(), currentFinger1.getY());
                            tellSizeResult();
                            break;
                        case SizeMeasureMethod.TWO_FINGERS:
                            if (fingerCounter < 2)
                                break;
                            fingerPosition1.update(currentFinger1.getX(), currentFinger1.getY());
                            fingerPosition2.update(currentFinger2.getX(), currentFinger2.getY());
                            tellSizeResult();
                            break;
                    }
                }
            }
            //如果当前正在矫正阶段且测距功能已激活，则需要实时监测当前做出的长度，以确认是否达标；并且在达标之后提示转入练习阶段
            if (currentStudyState == StudyState.CORRECTING) {
                /************待补充**************/
                switch (currentMeasureMethod) {
                    case SizeMeasureMethod.SINGLE_FINGER:
                        if (Math.abs(currentFinger1.calPhysicDistance(fingerPosition1, dpi) - studyGoal) < fingerToleranceA) {
                            startVibrator();
                            if (Math.abs(currentFinger1.getX() - originalFinger1.getX()) > holdingPositionLimit
                                    || Math.abs(currentFinger1.getY() - originalFinger1.getY()) > holdingPositionLimit) {
                                startHolding();
                                originalFinger1.update(currentFinger1.getX(), currentFinger1.getY());
                            }
                        }
                        if (holdingTime > holdingTimeLimitD) {
                            tts.speak(getString(R.string.start_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                            currentStudyState = StudyState.CORRECTED;
                        }
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
                            tts.speak(getString(R.string.start_practice), TextToSpeech.QUEUE_FLUSH, null, "startPractice");
                            currentStudyState = StudyState.CORRECTED;
                        }
                        break;
                    case SizeMeasureMethod.ONE_HAND:
                    case SizeMeasureMethod.TWO_HANDS:
                    case SizeMeasureMethod.BODY:
                        /************待补充**************/
                        break;
                }
            }
        }
    }

    //播报测量结果
    private void tellSizeResult() {
        switch (currentMeasureMethod) {
            case SizeMeasureMethod.SINGLE_FINGER:
            case SizeMeasureMethod.TWO_FINGERS:
                if (!fingerPosition1.isValid || !fingerPosition2.isValid) {
                    tts.speak(getString(R.string.unknown_problem), TextToSpeech.QUEUE_FLUSH, null, "unknownProblem");
                    return;
                }

                float fingerDistance = fingerPosition1.calPhysicDistance(fingerPosition2, dpi);
                TextView lastTryValue = findViewById(R.id.last_try_value);
                lastTryValue.setText(String.format("%.1f厘米", fingerDistance));

                tts.speak(MeasureReportBuilder.buildFormalReport(fingerDistance, studyGoal, currentMeasureMethod),
                        TextToSpeech.QUEUE_FLUSH, null, "result");

                if (Math.abs(fingerDistance - studyGoal) <= fingerToleranceA) {
                    tts.speak(getString(R.string.formal_study_success), TextToSpeech.QUEUE_ADD, null, "formalStudySuccess");
                    return;
                }
                else if (currentStudyState == StudyState.FIRST_TRY) {
                    tts.speak(getString(R.string.start_correction), TextToSpeech.QUEUE_ADD, null, "");
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
                break;
            case SizeMeasureMethod.ONE_HAND:
            case SizeMeasureMethod.TWO_HANDS:
            case SizeMeasureMethod.BODY:
                /*****************待补充*****************/
                break;
        }
    }
}