package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.TextView;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.FormalStudyBase;
import com.pcg.scaleteacher.base.FreeStudyAndTestBase;
import com.pcg.scaleteacher.helper.MeasureReportBuilder;

public class TestActivity extends FreeStudyAndTestBase {

    private int testGoal;

    private int tryCounter = 0;     //尝试次数
    private static final int tryLimit = 3;      //最多尝试3次

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initMotionTracking();
        buildNewTest();
    }

    @Override
    protected void initTextToSpeech() {
        super.initTextToSpeech();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                //当播报通过测试后，刷新题目，开启下一轮测试
                if (utteranceId.equals("testPassed"))
                    runOnUiThread(() -> buildNewTest());
                //当播报测试未通过后，跳转到对应的正式学习界面
                else if (utteranceId.equals("testFailed"))
                    runOnUiThread(() -> leadToFormalStudy());
            }

            @Override
            public void onError(String utteranceId) {}
        });
    }

    @Override
    protected void handleMeasurement() {
        TextView lastTryValue = findViewById(R.id.last_try_value);

        float result = 0f;   //测量结果
        float tolerance = 0f;    //允许误差
        //如果已确定为单指测量或双指测量
        if (currentMeasureMethod == SizeMeasureMethod.SINGLE_FINGER || currentMeasureMethod == SizeMeasureMethod.TWO_FINGERS) {
            result = fingerPosition1.calPhysicDistance(fingerPosition2, dpi);
            tolerance = fingerToleranceA;
            lastTryValue.setText(String.format("%.1f厘米", result));
        }

        //如果初步判定为单手/双手/躯体移动测量或角度测量
        else if (currentMeasureMethod == SizeMeasureMethod.MIXED) {
            //如果当前正在测验的就是角度，那么毋论如何就默认用户做出的是角度
            if (currentStudyContent == StudyContent.STUDY_ANGLE) {
                currentMeasureMethod = AngleMeasureMethod.ANGLE;
                result = spatialPose1.calAngleChange(spatialPose2);
                tolerance = angleToleranceA;
                lastTryValue.setText(String.format("%.0f度", result));
            }

            //如果当前正在测验的是长度，那么根据是否检测到手（双手距离是否为正数）来判定是否为双手测量
            else if (currentStudyContent == StudyContent.STUDY_SIZE) {
                tolerance = spatialToleranceA;
                result = getDistanceBetweenHands();
                if (result > 0)
                    currentMeasureMethod = SizeMeasureMethod.TWO_HANDS;
                else {
                    currentMeasureMethod = SizeMeasureMethod.ONE_HAND;
                    result = (float) Math.sqrt(spatialPose1.calDistanceSquare(spatialPose2));
                }
                lastTryValue.setText(String.format("%.0f厘米", result));
            }
        }

        if (currentStudyContent == StudyContent.STUDY_SIZE)
            tts.speak(MeasureReportBuilder.buildSizeReport(result, testGoal, currentMeasureMethod),
                TextToSpeech.QUEUE_FLUSH, null, "report");
        else if (currentStudyContent == StudyContent.STUDY_ANGLE)
            tts.speak(MeasureReportBuilder.buildAngleReport(result, testGoal),
                    TextToSpeech.QUEUE_FLUSH, null, "report");

        if (Math.abs(result - testGoal) <= tolerance)
            tts.speak(getString(R.string.test_passed), TextToSpeech.QUEUE_ADD, null, "testPassed");
        else {
            tryCounter++;
            if (tryCounter >= tryLimit)
                tts.speak(getString(R.string.test_failed), TextToSpeech.QUEUE_ADD, null, "testFailed");
            else
                tts.speak(getString(R.string.test_need_another_try), TextToSpeech.QUEUE_ADD, null, "testNeedAnotherTry");
        }

        currentActivationState = ActivationState.FINISHED;

    }

    //生成本次测试目标
    private void generateTestGoal() {
        //
        //待完成：需要根据本地存储的历史数据选择
        //
        currentStudyContent = StudyContent.STUDY_SIZE;
        testGoal = 30;
        spatialToleranceA = FormalStudyBase.getSpatialToleranceA(testGoal);
        spatialToleranceB = FormalStudyBase.getSpatialToleranceB(testGoal);
    }

    //开始新一轮测试
    private void buildNewTest() {
        generateTestGoal();

        tryCounter = 0;
        currentMeasureMethod = SizeMeasureMethod.UNKNOWN;

        String testGoalString = "";
        if (currentStudyContent == StudyContent.STUDY_SIZE)
            testGoalString = String.format("%d厘米", testGoal);
        else if (currentStudyContent == StudyContent.STUDY_ANGLE)
            testGoalString = String.format("%d度", testGoal);

        TextView testGoalValue = findViewById(R.id.test_goal_value);
        testGoalValue.setText(testGoalString);
        tts.speak("正在测验" + testGoalString, TextToSpeech.QUEUE_FLUSH, null, "testGoal");
    }

    //跳转到正式学习
    private void leadToFormalStudy() {
        Intent intent;
        if (currentStudyContent == StudyContent.STUDY_ANGLE)
            intent = new Intent(this, AngleFormalStudyActivity.class);
        else
            intent = new Intent(this, SizeFormalStudyActivity.class);
        intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
        intent.putExtra(studyContentTag, currentStudyContent);
        intent.putExtra(sizeMeasureMethodTag, currentMeasureMethod);
        intent.putExtra(studyGoalTag, testGoal);
        startActivity(intent);
        finish();
    }
}