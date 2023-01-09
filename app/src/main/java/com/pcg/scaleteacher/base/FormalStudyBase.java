package com.pcg.scaleteacher.base;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;

import com.pcg.scaleteacher.main.InputStudyContentActivity;

/* 该类作为正式教学活动的基类，主要处理正式教学过程中的三环节流程 */
public class FormalStudyBase extends CompletedFunctionBase {

    //学习目标
    protected int studyGoal;

    //所处的学习环节
    protected enum StudyState {
        FIRST_TRY,      //初次尝试阶段
        CORRECTING,     //对初次尝试的纠正阶段
        CORRECTED,      //已经纠正完成，但还没有开始练习的中间阶段
        PRACTICING      //反复练习阶段
    }
    protected StudyState currentStudyState = StudyState.FIRST_TRY;
    protected int practiceCounter = 0;      //练习阶段重复次数
    protected static final int practiceLimit = 5;   //练习阶段重复上限

    //测量是否已经激活
    protected boolean isMeasureActivated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int defaultGoal;
        switch (currentStudyContent) {
            case StudyContent.STUDY_SIZE:
                defaultGoal = 30;
                break;
            case StudyContent.STUDY_ANGLE:
                defaultGoal = 90;
                break;
            default:
                defaultGoal = 0;
                break;
        }
        studyGoal = intent.getIntExtra(studyGoalTag, defaultGoal);
    }

    @Override
    protected void initMotionTracking() {
        super.initMotionTracking();

        spatialToleranceA = getSpatialToleranceA(studyGoal);
        spatialToleranceB = getSpatialToleranceB(studyGoal);
    }

    public static float getSpatialToleranceA(int studyGoal) {
        return studyGoal > 100 ? (float) (studyGoal * 0.05f) : 5f;
    }

    public static float getSpatialToleranceB(int studyGoal) {
        return studyGoal > 100 ? (float) (studyGoal * 0.15f) : 15f;
    }

    @Override
    protected void initTextToSpeech() {
        super.initTextToSpeech();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals("formalStudySuccess") || utteranceId.equals("formalStudyComplete")) {
                    runOnUiThread(() -> returnToInput());
                }
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    protected void returnToInput() {
        Intent intent = new Intent(this, InputStudyContentActivity.class);
        intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
        intent.putExtra(studyContentTag, currentStudyContent);
        startActivity(intent);
        finish();
    }
}
