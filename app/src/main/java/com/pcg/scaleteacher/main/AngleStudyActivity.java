package com.pcg.scaleteacher.main;

import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
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

import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

public class AngleStudyActivity extends MoveMeasureBase {

    public static final int[] defaultAngles = new int[] {
            30, 45, 60, 90, 120, 135, 180
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
    private SimplePose startRotation;
    private SimplePose currentRotation;

    public static final int mainAngleDiff = 5;   //角度达标达标误差要求
    public static final int secondaryAngleDiff = 15;    //某单一角度达标时，还需保证其他角度变化不大
    private static final int passStandard = 50;        //学习达标对保持连续稳定位置的帧数要求
    private int holdCounter = 0;    //记录已保持目标位置的连续帧数
    private int passCounter = 0;    //记录某一次距离学习已达标次数
    private boolean readyForOnce = true;   //每次开始新一轮的学习之前，都必须保证取值为true

    private static final int PREPARING = 0;
    private static final int STUDYING = 1;
    private static final int CONFIRMING = 2;
    private int workingState;

    private Vibrator vibrator;
    private boolean isVibrating;

    private static final String parseProblem = "您未输入有效整数，请重新输入，或选择随机学习角度。";
    private static final String cameraProblem = "位置标记失败！请保证相机无遮挡后重试。";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_angle_study);

        initGLData();
        initVibrator();
        findUIElements();
        initInteractionTip();
    }

    public void randomSize(View view) {
        Random rand = new Random();
        target = defaultAngles[rand.nextInt(defaultAngles.length)];
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
        else
            prepareForStudy();
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
        float[] quaternion = currentRawPose.getRotationQuaternion();

        if (startRotation != null)
            tts.speak("已重置起点位置，请将手机缓慢移动，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "resetStart");
        else
            tts.speak("成功标记起始位置，请将手机缓慢移动，等待手机振动后保持静止。", TextToSpeech.QUEUE_FLUSH, null, "markStart");
        startRotation = new SimplePose(quaternion, quaternion);
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

        if (frame == null || startRotation == null)
            return;
        getCurrentRotation(frame);
    }

    private void findUIElements() {
        page = findViewById(R.id.angle_study_page);
        studyUI = findViewById(R.id.study_ui);

        LayoutInflater inflater = this.getLayoutInflater();
        preparationUI = (LinearLayout) inflater.inflate(R.layout.angle_study_preparation, null);
        confirmUI = (LinearLayout) inflater.inflate(R.layout.study_confirm, null);

        targetUI = findViewById(R.id.study_target);
    }

    private void initInteractionTip() {
        LayoutInflater inflater = this.getLayoutInflater();
        helpUI = (LinearLayout) inflater.inflate(R.layout.interaction_tip, null);
        runOnUiThread(()->page.addView(helpUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));

        tipContent = (TextView) findViewById(R.id.tip_content);
        tipContent.setText(R.string.angle_study_tip);

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
        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        targetUI.setText("正在学习：" + target + "度");

        startRotation = null;

        passCounter = holdCounter = 0;
        readyForOnce = true;
    }

    private void confirmStudy() {
        studyUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        workingState = CONFIRMING;
        runOnUiThread(() -> page.addView(confirmUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));
    }

    private void getCurrentRotation(Frame frame) {
        Camera camera = frame.getCamera();
        Pose currentPose = camera.getPose();
        float [] quaternion = currentPose.getRotationQuaternion();
        currentRotation = new SimplePose(quaternion, SimplePose.ONLY_ANGLE);

        int[] angleDiffs = currentRotation.calAngleChange(startRotation);

        boolean isOneOrientationPass = false;   //指示是否有某一个方向的旋转角度接近目标
        for (int i = 0; i < 3; i++) {
            boolean isStrict = false;           //指示是否严格满足要求，即除了旋转角度接近目标角度的方向外，其他方向是否几乎没有旋转
            if (Math.abs(Math.abs(angleDiffs[i]) - target) <= mainAngleDiff) {
                switch (i) {
                    case 0:
                        isStrict = (Math.abs(angleDiffs[1]) <= secondaryAngleDiff) && (Math.abs(angleDiffs[2]) <= secondaryAngleDiff);
                        break;
                    case 1:
                        isStrict = (Math.abs(angleDiffs[0]) <= secondaryAngleDiff) && (Math.abs(angleDiffs[2]) <= secondaryAngleDiff);
                        break;
                    case 2:
                        isStrict = (Math.abs(angleDiffs[0]) <= secondaryAngleDiff) && (Math.abs(angleDiffs[1]) <= secondaryAngleDiff);
                        break;
                }
                if (isStrict) {
                    isOneOrientationPass = true;
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
                else {
                    updateStability(false);
                }
            }
        }
        if ( !isOneOrientationPass )
            updateStability(false);
    }

    private void initVibrator() {
        vibrator = (Vibrator) this.getSystemService(MoveMeasureActivity.VIBRATOR_SERVICE);
        isVibrating = false;
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
            tts.speak("已成功学习了一次" + target + "度，请再巩固一次吧。", TextToSpeech.QUEUE_FLUSH, null, "passOnce");
        }
        else {
            tts.speak("已成功学习了两次" + target + "度，请选择如何继续。", TextToSpeech.QUEUE_FLUSH, null, "passTwice");
            confirmStudy();
        }
    }
}