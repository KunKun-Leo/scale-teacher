package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ScreenReaderCheckHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class TestActivity extends MoveMeasureBase {

    private static final int testMax = 5;   //标记单次测试题目个数
    private int currentTestIndex = 0;   //标记已经在测试第几题
    private int passCounter = 0;    //记录达标次数

    private TestObject[] testBank = new TestObject[SizeStudyActivity.defaultSizes.length + AngleStudyActivity.defaultAngles.length];   //总题库
    private TestObject[] testSet;    //本次测试题目集

    private TestObject testObj; //当前测试对象
    private int target;         //当前测试目标值
    private TextView targetUI;
    private static final int TEST_SIZE = 0;
    private static final int TEST_ANGLE = 1;
    private int testState;      //当前测试状态

    private SimplePose startPose = null;
    private SimplePose endPose = null;

    private boolean hasScreenReader;
    private FingerLocation finger1, finger2;
    private int dpi;

    private static final String cameraProblem = "位置标记失败！请保证相机无遮挡后重试.";
    private static final String manyFingersProblem = "您在屏幕上放置了太多手指，请全部抬起后重试。";
    private static final String finishProblem = "本轮测试已完成，请使用退出测试键退出。";

    ConstraintLayout page;
    private LinearLayout helpUI;
    private TextView tipContent;
    LinearLayout testUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        initGLData();
        initTouchMeasure();
        initTestSet();
        initInteractionTip();

        hasScreenReader = ScreenReaderCheckHelper.check(this);
    }

    public void markPose(View view) {
        if (currentTestIndex >= testMax) {
            tts.speak(finishProblem, TextToSpeech.QUEUE_FLUSH, null, "isOver");
            return;
        }

        if (frame == null) {
            Toast.makeText(this, "frame is null", Toast.LENGTH_SHORT).show();
            return;
        }

        Camera camera = frame.getCamera();
        if (camera.getTrackingState() != TrackingState.TRACKING) {
            tts.speak(cameraProblem, TextToSpeech.QUEUE_FLUSH, null, "cameraProblem");
            return;
        }
        Pose currentRawPose = camera.getPose();
        float[] translation = currentRawPose.getTranslation();
        float[] quaternion = currentRawPose.getRotationQuaternion();

        if (startPose == null) {
            startPose = new SimplePose(translation, quaternion);
            tts.speak("成功标记初始位姿状态", TextToSpeech.QUEUE_FLUSH, null, "markStart");
        }
        else if (endPose == null) {
            endPose = new SimplePose(translation, quaternion);
            judge(true);
            startPose = endPose = null; //clear all the marked locations
        }
    }

    public void shutTip(View view) {
        page.removeView(helpUI);
        testUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        nextTest();
    }

    public void goBack(View view) {
        this.finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (currentTestIndex >= testMax) {
            tts.speak(finishProblem, TextToSpeech.QUEUE_FLUSH, null, "isOver");
            return false;
        }
        if (testState == TEST_ANGLE)
            return false;
        int index = e.getActionIndex();
        startPose = endPose = null;
        if (hasScreenReader) {
            switch (e.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    tts.speak("屏幕测量已激活，请另外放下两根手指。", TextToSpeech.QUEUE_FLUSH, null, "activated");
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = e.getPointerId(index);
                    if (pointerId == 1) {
                        handleFinger(e, pointerId, finger1);
                    }
                    else if (pointerId == 2) {
                        handleFinger(e, pointerId, finger2);
                    }
                    else if (pointerId > 2)
                        tts.speak(manyFingersProblem, TextToSpeech.QUEUE_FLUSH, null, "manyFingersProblem");
                    return false;
                default:
                    break;
            }
        }
        else {
            switch (e.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    finger1.update(e.getX(), e.getY());
                    tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
                    return false;
                //break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = e.getPointerId(index);
                    if (pointerId == 1) {
                        handleFinger(e, pointerId, finger2);
                    }
                    else if (pointerId > 1)
                        tts.speak(manyFingersProblem, TextToSpeech.QUEUE_FLUSH, null, "manyFingersProblem");
                    return false;
                default:
                    break;
            }
        }

        return super.onTouchEvent(e);
    }

    private void initInteractionTip() {
        page = findViewById(R.id.test_page);
        LayoutInflater inflater = this.getLayoutInflater();
        helpUI = (LinearLayout) inflater.inflate(R.layout.interaction_tip, null);
        runOnUiThread(()->page.addView(helpUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));

        tipContent = (TextView) findViewById(R.id.tip_content);
        tipContent.setText(R.string.test_tip);

        testUI = (LinearLayout) findViewById(R.id.test_ui);
        testUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void judge(boolean isJudgingMove) {
        currentTestIndex++;

        boolean pass;
        if (isJudgingMove) {
            if (testState == TEST_SIZE)
                pass = judgeSizeMove();
            else
                pass = judgeAngleMove();
        }
        else {
            pass = judgeTouch();
        }

        if (pass)
            passCounter ++;

        nextTest();
    }

    private void nextTest() {
        if (currentTestIndex < testMax) {
            initSingleTest(currentTestIndex);
        }
        else
            finishTest();
    }

    private void finishTest() {
        String report = "你已完成本轮测试，共计达标" + passCounter + "次。";
        if (passCounter <= 3)
            report = report + "还需要认真学习哦。";
        else if (passCounter == 4)
            report = report + "还差一点就可以满分啦。";
        else if (passCounter == 5)
            report = report + "完成得真不错！";
        tts.speak(report, TextToSpeech.QUEUE_ADD, null, "report");

        /*LayoutInflater inflater = this.getLayoutInflater();
        reportPageUI = (LinearLayout) inflater.inflate(R.layout.test_report, null);

        runOnUiThread(() -> page.addView(reportPageUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));
        testUI = (LinearLayout) findViewById(R.id.test_ui);
        testUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);*/
    }

    private void initTouchMeasure() {
        finger1 = new FingerLocation();
        finger2 = new FingerLocation();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dpi = metrics.densityDpi;
    }

    private void initTestBank() {
        for (int i = 0; i < SizeStudyActivity.defaultSizes.length; i++)
            testBank[i] = new TestObject(SizeStudyActivity.defaultSizes[i], TEST_SIZE);
        for (int j = 0; j < AngleStudyActivity.defaultAngles.length; j++)
            testBank[j + SizeStudyActivity.defaultSizes.length] = new TestObject(AngleStudyActivity.defaultAngles[j], TEST_ANGLE);
    }

    private void initTestSet() {
        initTestBank();

        Random rand = new Random();
        HashSet<TestObject> tempTestSet = new HashSet<TestObject>();
        while (tempTestSet.size() < testMax) {
            int randomIndex = rand.nextInt(testBank.length);
            tempTestSet.add(testBank[randomIndex]);
        }

        testSet = tempTestSet.toArray(new TestObject[testMax]);

        targetUI = findViewById(R.id.test_target);
    }

    private void initSingleTest(int testIndex) {
        testObj = testSet[testIndex];
        target = testObj.value;
        testState = testObj.testType;

        String targetDescription;
        if (testState == TEST_SIZE)
            targetDescription = "正在测试：" + target + "厘米";
        else
            targetDescription = "正在测试：" + target + "度";

        targetUI.setText(targetDescription);
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tts.speak(targetDescription, TextToSpeech.QUEUE_ADD, null, "singleTest");   //如果不延时执行，第一次播报会失效
            }
        }.start();
    }

    private void handleFinger (MotionEvent e, int pointerId, FingerLocation finger) {
        int pointer =  e.findPointerIndex(pointerId);
        finger.update(e.getX(pointer), e.getY(pointer));
        if (finger == finger1)
            tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
        else if (finger == finger2)
            judge(false);
    }

    private boolean judgeSizeMove() {
        boolean pass;
        int distance = (int)Math.sqrt(endPose.calDistanceSquare(startPose) * 10000);

        String result = "成功标记结束位姿状态。";

        if (Math.abs(distance - target) <= target * 0.1) {
            result = result + target + "厘米测试达标。";
            pass = true;
        }
        else {
            result = result + "两个位置相距" + distance + "厘米。误差过大，测试未达标。";
            pass = false;
        }
        tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "lengthResult");
        return pass;
    }

    private boolean judgeAngleMove() {
        boolean pass = false;
        String[] orientations = new String[]{
                "左右转向", "前后倾斜", "左右倾斜"
        };
        int[] angleChange = endPose.calAngleChange(startPose);

        boolean relaxedPass = false;            //指示是否不严格地满足要求，即有一个方向的旋转角度接近目标角度
        int relaxedPassIndex = 0;                   //不严格地满足要求的方向
        StringBuilder result = new StringBuilder("成功标记结束位姿状态。");
        for (int i = 0; i < 3; i++) {
            boolean strictPass = false;           //指示是否严格满足要求，即除了旋转角度接近目标角度的方向外，其他方向是否几乎没有旋转
            if (Math.abs(Math.abs(angleChange[i]) - target) <= AngleStudyActivity.mainAngleDiff) {
                relaxedPass = true;
                relaxedPassIndex = i;
                switch (i) {
                    case 0:
                        strictPass = (Math.abs(angleChange[1]) <= AngleStudyActivity.secondaryAngleDiff) && (Math.abs(angleChange[2]) <= AngleStudyActivity.secondaryAngleDiff);
                        break;
                    case 1:
                        strictPass = (Math.abs(angleChange[0]) <= AngleStudyActivity.secondaryAngleDiff) && (Math.abs(angleChange[2]) <= AngleStudyActivity.secondaryAngleDiff);
                        break;
                    case 2:
                        strictPass = (Math.abs(angleChange[0]) <= AngleStudyActivity.secondaryAngleDiff) && (Math.abs(angleChange[1]) <= AngleStudyActivity.secondaryAngleDiff);
                        break;
                }
                if (strictPass) {
                    result.append(target).append("度测试达标。");
                    pass = true;
                    break;
                }
            }
        }
        if (!pass && relaxedPass)
            result.append(orientations[relaxedPassIndex]).append("度数满足要求，但其他方向也出现明显偏转，测试未达标");
        else if (!pass) {
            for (int j = 0; j < 3; j++) {
                result.append(orientations[j]).append("了").append(Math.abs(angleChange[j])).append("度，");
            }
            result.append("均未达标。");
        }

        tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "rotationResult");

        return pass;
    }

    @SuppressLint("DefaultLocale")
    private boolean judgeTouch() {
        boolean pass;
        float currentDistance = finger2.calPhysicDistance(finger1, dpi);
        @SuppressLint("DefaultLocale") String result = "已放置两根手指。";
        if (Math.abs(currentDistance - target) <= SizeStudyActivity.touchMeasureDiff) {
            result = result + target + "厘米测试达标。";
            pass = true;
        }
        else {
            result = result + "两手指间距离" + String.format("%.1f", currentDistance) + "厘米。误差过大，测试未达标。";
            pass = false;
        }
        tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "result");
        return pass;
    }


}

class TestObject {
    public final int value;
    public final int testType;

    public TestObject(int _value, int _type) {
        value = _value;
        testType = _type;
    }
}