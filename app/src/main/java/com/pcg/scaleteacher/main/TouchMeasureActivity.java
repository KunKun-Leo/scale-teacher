package com.pcg.scaleteacher.main;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ScreenReaderCheckHelper;

import java.util.Locale;

public class TouchMeasureActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private boolean hasScreenReader;    //由于读屏软件会阻碍触摸事件的正常运行，因此需要判断并进行一些特别处理

    private TextToSpeech tts;
    private FingerLocation finger1, finger2;
    private int dpi;

    private ConstraintLayout page;
    private LinearLayout helpUI;
    private LinearLayout measureUI;
    private TextView tipContent;

    private static final String manyFingersProblem = "您在屏幕上放置了太多手指，请全部抬起后重试。";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touch_measure);

        initTouchMeasure();
        initTextToSpeech();
        initInteractionTip();

        hasScreenReader = ScreenReaderCheckHelper.check(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int index = e.getActionIndex();
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
                    Log.e("debug", "0");
                    finger1.update(e.getX(), e.getY());
                    tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
                    return false;
                //break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = e.getPointerId(index);
                    if (pointerId == 1) {
                        Log.e("debug", "1");
                        int pointer2 = e.findPointerIndex(1);
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


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // setLanguage设置语言
            int result = tts.setLanguage(Locale.CHINESE);   //网上很多资料都用的是Locale.CHINA，会导致无法正常播放，原因不明
            // TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失
            // TextToSpeech.LANG_NOT_SUPPORTED：不支持
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, this);
        tts.setPitch(1.0f);
        tts.setSpeechRate(0.5f);
    }

    public void shutTip(View view) {
        page.removeView(helpUI);
        measureUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    public void goBack(View view) {
        this.finish();
    }

    private void initInteractionTip() {
        page = findViewById(R.id.touch_measure_page);
        LayoutInflater inflater = this.getLayoutInflater();
        helpUI = (LinearLayout) inflater.inflate(R.layout.interaction_tip, null);
        runOnUiThread(()->page.addView(helpUI, new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)));

        tipContent = (TextView) findViewById(R.id.tip_content);
        tipContent.setText(R.string.touch_measure_tip);

        measureUI = (LinearLayout) findViewById(R.id.measure_ui);
        measureUI.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void initTouchMeasure() {
        finger1 = new FingerLocation();
        finger2 = new FingerLocation();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        dpi = metrics.densityDpi;
    }

    private void handleFinger (MotionEvent e, int pointerId, FingerLocation finger) {
        int pointer =  e.findPointerIndex(pointerId);
        finger.update(e.getX(pointer), e.getY(pointer));
        if (finger == finger1)
            tts.speak("已放置一根手指", TextToSpeech.QUEUE_FLUSH, null, "finger1Ready");
        else if (finger == finger2)
            tellResult();
    }

    private void tellResult() {
        if (finger1 == null || finger2 == null)
            return;
        float currentDistance = finger2.calPhysicDistance(finger1, dpi);
        @SuppressLint("DefaultLocale") String result = "已放置两根手指。两手指间距离" + String.format("%.1f", currentDistance) + "厘米。";
        tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "result");
    }
}

class FingerLocation {
    private int x, y;

    public FingerLocation() {
        this.x = 0;
        this.y = 0;
    }

    public FingerLocation(float _x, float _y) {
        this.x = (int)_x;
        this.y = (int)_y;
    }

    public void update(float _x, float _y) {
        this.x = (int)_x;
        this.y = (int)_y;
    }

    public float calPhysicDistance(FingerLocation other, int dpi) {
        int pixelCount = (int) Math.sqrt( (this.x - other.x) * (this.x - other.x) + (this.y -other.y) * (this.y - other.y) );
        return (float)pixelCount / (float)dpi * 2.54f;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}