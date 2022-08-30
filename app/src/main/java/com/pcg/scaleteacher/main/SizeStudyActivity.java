package com.pcg.scaleteacher.main;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ScreenReaderCheckHelper;

import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

public class SizeStudyActivity extends MoveMeasureBase {

    public static final int[] defaultSizes = new int[] {
            2, 3, 5, 8, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 120, 150, 200
    };

    private ConstraintLayout page;
    private LinearLayout helpUI;
    private TextView tipContent;
    private LinearLayout studyUI;
    private LinearLayout preparationUI;
    private LinearLayout confirmUI;
    private EditText customInput;
    private TextView targetUI;

    private int target;
    private boolean isRandomTarget;
    private SimplePose startLocation;
    private SimplePose currentLocation;
    private boolean needMoveFarther;    //用来检测是否移动过快而需要提醒
    private FingerLocation finger1;
    private FingerLocation finger2;
    private boolean hasOneFinger;
    private boolean hasTwoFingers;
    private int dpi;

    public static final float touchMeasureDiff = 1f;   //手指比划达标误差要求
    public static final float moveMeasureDiff = 16f;    //移动比划达标误差要求
    private static final float moveCautionDiff = 400f;   //移动过快而越界时进行警告
    private static final int passStandard = 50;        //学习达标对保持连续稳定位置的帧数要求
    private int holdCounter = 0;    //记录已保持目标位置的连续帧数
    private int passCounter = 0;    //记录某一次距离学习已达标次数
    private boolean readyForOnce = true;   //每次开始新一轮的学习之前，都必须保证取值为true

    private static final int PREPARING = 0;
    private static final int STUDYING = 1;
    private static final int CONFIRMING = 2;
    private int workingState;

    private boolean hasScreenReader;

    private Vibrator vibrator;
    private boolean isVibrating;

    private static final String parseProblem = "您未输入有效整数，请重新输入，或选择随机学习尺寸。";
    private static final String cameraProblem = "位置标记失败！请保证相机无遮挡后重试。";
    private static final String manyFingersProblem = "您在屏幕上放置了太多手指，请全部抬起后重试。";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_size_study);

        initGLData();
        initTouchMeasure();
        initVibrator();
        findUIElements();
        initInteractionTip();
        hasScreenReader = ScreenReaderCheckHelper.check(this);
    }

    public void randomSize(View view) {
        Random rand = new Random();
        target = defaultSizes[rand.nextInt(defaultSizes.length)];
        isRandomTarget = true;
        startStudy();
    }

    public void customSize(View view) {
        customInput = findViewById(R.id.custom_input);
        try {
            target = Integer.parseInt(customInput.getText().toString());
        }
        catch (Exception e) {
            tts.speak(parseProblem, TextToSpeech.QUEUE_FLUSH, null, "parseProblem");
            return;
        }
        if (target <= 0) {
            tts.speak(parseProblem, TextToSpeech.QUEUE_FLUSH, null, "parseProblem");
            return;
        }
        isRandomTarget = false;
        startStudy();
    }

    public void studyNext(View view) {
        if (isRandomTarget)
            randomSize(view);
        else {
            runOnUiThread(() -> page.removeView(confirmUI));
            prepareForStudy();
        }
    }

    public void practiceMore(View view) {
        startStudy();
    }

    public void markStartPose(View view) {
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

        if (startLocation != null)
            tts.speak("已重置起点位置，请将手机缓慢移动，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "resetStart");
        else
            tts.speak("成功标记起始位置，请将手机缓慢移动，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "markStart");
        startLocation = new SimplePose(translation, SimplePose.ONLY_LOCATION);
        needMoveFarther = true;
        readyForOnce = true;
    }

    public void shutTip(View view) {
        page.removeView(helpUI);
        prepareForStudy();
    }

    public void goBack(View view) {
        this.finish();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (workingState != STUDYING)
            return;
        super.onDrawFrame(gl);

        if (!readyForOnce)
            return;

        if (hasTwoFingers)
            checkFingerDistance();

        if (frame == null || startLocation == null)
            return;
        getCurrentLocation(frame);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (workingState != STUDYING)
            return super.onTouchEvent(e);

        int index = e.getActionIndex();
        int eventType = e.getAction() & MotionEvent.ACTION_MASK;
        if (hasScreenReader) {
            switch (eventType) {
                case MotionEvent.ACTION_DOWN:
                    tts.speak("屏幕测量已激活，请另外放下两根手指。", TextToSpeech.QUEUE_FLUSH, null, "activated");
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = e.getPointerId(index);
                    if (pointerId == 1) {
                        finger1.update(e.getX(), e.getY());
                        tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
                        hasOneFinger = true;
                    }
                    else if (pointerId == 2) {
                        finger2.update(e.getX(), e.getY());
                        tts.speak("已放置两根手指，请缓慢调整手指间距，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "finger2Ready");
                        hasTwoFingers = true;
                    }
                    else if (pointerId > 2)
                        tts.speak(manyFingersProblem, TextToSpeech.QUEUE_FLUSH, null, "manyFingersProblem");
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (hasOneFinger) {
                        int pointerIndex1 = e.findPointerIndex(1);
                        finger1.update(e.getX(pointerIndex1), e.getY(pointerIndex1));
                    }
                    if (hasTwoFingers) {
                        int pointerIndex2 = e.findPointerIndex(2);
                        finger2.update(e.getX(pointerIndex2), e.getY(pointerIndex2));
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (e.getPointerId(index) == 1)
                        hasOneFinger = false;
                    hasTwoFingers = false;
                    readyForOnce = true;
                    return false;
                default:
                    break;
            }
        }
        else {
            switch (eventType) {
                case MotionEvent.ACTION_DOWN:
                    finger1.update(e.getX(), e.getY());
                    tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
                    return false;
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = e.getPointerId(index);
                    if (pointerId == 1) {
                        finger2.update(e.getX(), e.getY());
                        tts.speak("已放置两根手指，请缓慢调整手指间距，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "finger2Ready");
                        hasTwoFingers = true;
                    }
                    else if (pointerId > 1)
                        tts.speak(manyFingersProblem, TextToSpeech.QUEUE_FLUSH, null, "manyFingersProblem");
                    return false;
                case MotionEvent.ACTION_MOVE:
                    int pointerIndex1 = e.findPointerIndex(0);
                    finger1.update(e.getX(pointerIndex1), e.getY(pointerIndex1));
                    if (hasTwoFingers) {
                        int pointerIndex2 = e.findPointerIndex(1);
                        finger2.update(e.getX(pointerIndex2), e.getY(pointerIndex2));
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    hasTwoFingers = false;
                    readyForOnce = true;
                    return false;
                default:
                    break;
            }
        }

        return super.onTouchEvent(e);
    }

    private void findUIElements() {
        page = findViewById(R.id.size_study_page);
        studyUI = findViewById(R.id.study_ui);

        LayoutInflater inflater = this.getLayoutInflater();
        preparationUI = (LinearLayout) inflater.inflate(R.layout.size_study_preparation, null);
        confirmUI = (LinearLayout) inflater.inflate(R.layout.study_confirm, null);

        targetUI = findViewById(R.id.study_target);
    }

    private void initInteractionTip() {
        LayoutInflater inflater = this.getLayoutInflater();
        helpUI = (LinearLayout) inflater.inflate(R.layout.interaction_tip, null);
        runOnUiThread(()->page.addView(helpUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));

        tipContent = (TextView) findViewById(R.id.tip_content);
        tipContent.setText(R.string.size_study_tip);

        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void prepareForStudy() {
        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        workingState = PREPARING;
        runOnUiThread(() -> page.addView(preparationUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));
    }

    private void startStudy() {
        switch (workingState) {
            case PREPARING:
                runOnUiThread(() -> page.removeView(preparationUI));
                break;
            case CONFIRMING:
                runOnUiThread(() -> page.removeView(confirmUI));
                break;
        }

        workingState = STUDYING;
        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        targetUI.setText("正在学习：" + target + "厘米");

        finger1 = new FingerLocation();
        finger2 = new FingerLocation();
        startLocation = null;
        needMoveFarther = true;

        passCounter = holdCounter = 0;
        readyForOnce = true;
    }

    private void confirmStudy() {
        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        workingState = CONFIRMING;
        runOnUiThread(() -> page.addView(confirmUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));
    }

    private void getCurrentLocation(Frame frame) {
        Camera camera = frame.getCamera();
        Pose currentPose = camera.getPose();
        float [] translation = currentPose.getTranslation();
        currentLocation = new SimplePose(translation, SimplePose.ONLY_LOCATION);

        float distanceSquare = currentLocation.calDistanceSquare(startLocation) * 10000;
        Log.v("distanceSquare", String.valueOf(distanceSquare));
        float difference = distanceSquare - target * target;
        if (Math.abs(difference) <= moveMeasureDiff) {
            if (updateStability(true)) {
                handlePassOnce();
                return;
            }
            if (!isVibrating) {
                isVibrating = true;
                vibrator.vibrate(1000);
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isVibrating = false;
                    }
                }.start();
            }
        }
        else
            updateStability(false);
        if (difference > moveCautionDiff && needMoveFarther) {
            tts.speak("你移动的距离太远了，请稍微缓慢缩短距离。", TextToSpeech.QUEUE_FLUSH, null, "tooFar");
            needMoveFarther = false;
            return;
        }
        if (difference < -moveCautionDiff && !needMoveFarther) {
            tts.speak("您移动的距离太近了，请稍微缓慢扩大距离。", TextToSpeech.QUEUE_FLUSH, null, "tooNear");
            needMoveFarther = true;
        }

    }

    private void initTouchMeasure() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        hasOneFinger = false;
        hasTwoFingers = false;
        dpi = metrics.densityDpi;
    }

    private void initVibrator() {
        vibrator = (Vibrator) this.getSystemService(MoveMeasureActivity.VIBRATOR_SERVICE);
        isVibrating = false;
    }

    private void checkFingerDistance() {
        if (finger1 == null || finger2 == null)
            return;
        float current = finger2.calPhysicDistance(finger1, dpi);
        if(vibrator == null)
            return;
        if((current - target >= -touchMeasureDiff) && (current - target <= touchMeasureDiff)){
            if (updateStability(true)) {
                handlePassOnce();
                return;
            }
            if (!isVibrating) {
                isVibrating = true;
                vibrator.vibrate(1000);
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        isVibrating = false;
                    }
                }.start();
            }
        }
        else
            updateStability(false);
    }

    private boolean updateStability(boolean isStable) {
        if (isStable)
            holdCounter++;
        else
            holdCounter = 0;
        if (holdCounter == passStandard) {
            holdCounter = 0;
            return true;
        }
        else
            return false;
    }

    private void handlePassOnce() {
        passCounter++;
        readyForOnce = false;
        if (passCounter == 1) {
            tts.speak("已成功学习了一次" + target + "厘米，请再巩固一次吧。", TextToSpeech.QUEUE_FLUSH, null, "passOnce");
        }
        else {
            tts.speak("已成功学习了两次" + target + "厘米，请选择如何继续。", TextToSpeech.QUEUE_FLUSH, null, "passTwice");
            confirmStudy();
        }
    }
}