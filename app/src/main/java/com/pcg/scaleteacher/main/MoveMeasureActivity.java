package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Camera;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.pcg.scaleteacher.R;

public class MoveMeasureActivity extends MoveMeasureBase {

    private boolean isMeasuringLength;

    private SimplePose startPose = null;
    private SimplePose endPose = null;

    private ConstraintLayout page;
    private LinearLayout helpUI;
    private LinearLayout measureUI;
    private TextView tipContent;

    private static final String cameraProblem = "位置标记失败！请保证相机无遮挡后重试.";
    private static final String markProblem = "位置标记出现混乱，无法正确计算。请重新标记起始和结束状态.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_measure);

        Intent intent = getIntent();
        isMeasuringLength = intent.getBooleanExtra(MeasureOptionActivity.isMeasuringLength, true);

        initGLData();
        initInteractionTip();
    }

    public void markPose(View view) {
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
            tellResult();
            startPose = endPose = null; //clear all the marked locations
        }
    }

    public void shutTip(View view) {
        page.removeView(helpUI);
        measureUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    public void goBack(View view) {
        this.finish();
    }

    private void initInteractionTip() {
        page = findViewById(R.id.move_measure_page);
        LayoutInflater inflater = this.getLayoutInflater();
        helpUI = (LinearLayout) inflater.inflate(R.layout.interaction_tip, null);
        runOnUiThread(()->page.addView(helpUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));

        tipContent = (TextView) findViewById(R.id.tip_content);
        if (isMeasuringLength)
            tipContent.setText(R.string.length_measure_tip);
        else
            tipContent.setText(R.string.rotation_measure_tip);

        measureUI = (LinearLayout) findViewById(R.id.measure_ui);
        measureUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void tellResult() {
        if (startPose == null || endPose == null) {
            tts.speak(markProblem, TextToSpeech.QUEUE_FLUSH, null, "markProblem");
            startPose = endPose = null;
            return;
        }
        if(isMeasuringLength) {
            int distance = (int)Math.sqrt(endPose.calDistanceSquare(startPose) * 10000);
            tts.speak(composeLengthResult(distance), TextToSpeech.QUEUE_FLUSH, null, "lengthResult");
        }
        else {
            int[] angleChange = endPose.calAngleChange(startPose);
            tts.speak(composeRotationResult(angleChange), TextToSpeech.QUEUE_FLUSH, null, "rotationResult");
        }
    }

    private String composeLengthResult(int distance) {
        return "成功标记结束位姿状态。本次标记的两个位置,相距" + String.valueOf(distance) + "厘米。";
    }

    private String composeRotationResult(int[] angleChange) {
        boolean isChangeObvious = false;
        final int obviousStandard = 5;

        StringBuilder result = new StringBuilder();
        result.append("成功标记结束位姿状态。相对于初始状态,手机");
        if (angleChange[0] < -obviousStandard) {
            result.append("向左转向了").append(Math.abs(angleChange[0])).append("度,");
            isChangeObvious = true;
        }
        else if (angleChange[0] > obviousStandard) {
            result.append("向右转向了").append(angleChange[0]).append("度,");
            isChangeObvious = true;
        }
        if (angleChange[1] < -obviousStandard) {
            result.append("向背面倾倒了").append(Math.abs(angleChange[1])).append("度,");
            isChangeObvious = true;
        }
        else if (angleChange[1] > obviousStandard) {
            result.append("向正面倾倒了").append(angleChange[1]).append("度,");
            isChangeObvious = true;
        }
        if (angleChange[2] < -obviousStandard) {
            result.append("向右倾倒了").append(Math.abs(angleChange[2])).append("度,");
            isChangeObvious = true;
        }
        else if (angleChange[2] > obviousStandard) {
            result.append("向左倾倒了").append(angleChange[2]).append("度,");
            isChangeObvious = true;
        }
        if(!isChangeObvious) {
            result.append("没有发生明显转动。");
        }

        return result.toString();
    }

}