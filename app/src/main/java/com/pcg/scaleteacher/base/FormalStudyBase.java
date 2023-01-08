package com.pcg.scaleteacher.base;

import android.content.Intent;
import android.os.Bundle;

/* 该类作为正式教学活动的基类，主要处理正式教学过程中的三环节流程 */
public class FormalStudyBase extends CompletedFunctionBase {

    //学习目标
    protected int studyGoal;

    //所处的学习环节
    protected enum StudyState {
        FIRST_TRY,      //初次尝试阶段
        CORRECTING,     //对初次尝试的纠正阶段
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
}
