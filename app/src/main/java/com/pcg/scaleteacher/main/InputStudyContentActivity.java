package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.ConstantBase;

import java.util.Locale;

public class InputStudyContentActivity extends ConstantBase implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private int currentStudyContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        currentStudyContent = intent.getIntExtra(IntroductionActivity.studyContentTag, IntroductionActivity.StudyContent.STUDY_SIZE);

        initUI();
        initTextToSpeech();
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

    //根据具体场景更新UI
    private void initUI() {
        setContentView(R.layout.activity_input_study_content);

        TextView pageTitle = findViewById(R.id.input_page_title);
        EditText editText = findViewById(R.id.study_goal_edit);
        TextView activeTab;
        switch (currentStudyContent) {
            case IntroductionActivity.StudyContent.STUDY_SIZE:
                pageTitle.setText(R.string.input_size);
                editText.setHint(R.string.input_angle_hint);

                activeTab = findViewById(R.id.switch_to_size_formal);
                activeTab.setTypeface(Typeface.create(activeTab.getTypeface(), Typeface.NORMAL), Typeface.BOLD);
                break;
            case IntroductionActivity.StudyContent.STUDY_ANGLE:
                pageTitle.setText(R.string.input_angle);
                editText.setHint(R.string.input_angle_hint);

                activeTab = findViewById(R.id.switch_to_angle_formal);
                activeTab.setTypeface(Typeface.create(activeTab.getTypeface(), Typeface.NORMAL), Typeface.BOLD);
                break;
        }
    }

    public void confirmInput(View view) {
        EditText editText = findViewById(R.id.study_goal_edit);
        int studyGoal;
        Intent intent;
        try {
            studyGoal = Integer.parseInt(editText.getText().toString());
        }
        catch (Exception e) {
            tts.speak(getString(R.string.integer_parse_problem), TextToSpeech.QUEUE_FLUSH, null, "integerParseProblem");
            return;
        }
        switch (currentStudyContent) {
            case StudyContent.STUDY_SIZE:
                if (studyGoal <= 0 || studyGoal > 500) {
                    tts.speak(getString(R.string.size_range_problem), TextToSpeech.QUEUE_FLUSH, null, "integerParseProblem");
                    return;
                }
                intent = new Intent(this, SizeFormalStudyActivity.class);
                intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
                intent.putExtra(studyContentTag, StudyContent.STUDY_SIZE);
                intent.putExtra(studyGoalTag, studyGoal);
                startActivity(intent);
                break;
            case StudyContent.STUDY_ANGLE:
                if (studyGoal <= 0 || studyGoal > 180) {
                    tts.speak(getString(R.string.angle_range_problem), TextToSpeech.QUEUE_FLUSH, null, "integerParseProblem");
                    return;
                }
//                intent = new Intent(this, SizeFormalStudyActivity.class);
//                intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
//                intent.putExtra(studyContentTag, StudyContent.STUDY_SIZE);
//                intent.putExtra(studyGoalTag, studyGoal);
//                startActivity(intent);
                break;
            default:
                break;
        }
    }

    public void switchStudyContent(View view) {
        int viewId = view.getId();
        Intent intent;
        if (currentStudyContent == IntroductionActivity.StudyContent.STUDY_SIZE && viewId == R.id.switch_to_angle_formal) {
            intent = new Intent(this, InputStudyContentActivity.class);
            intent.putExtra(IntroductionActivity.studyContentTag, IntroductionActivity.StudyContent.STUDY_ANGLE);
            startActivity(intent);
            finish();
        }
        else if (currentStudyContent == IntroductionActivity.StudyContent.STUDY_ANGLE && viewId == R.id.switch_to_size_formal) {
            intent = new Intent(this, InputStudyContentActivity.class);
            intent.putExtra(IntroductionActivity.studyContentTag, IntroductionActivity.StudyContent.STUDY_SIZE);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // setLanguage设置语言
            int result = tts.setLanguage(Locale.CHINESE);   //网上很多资料都用的是Locale.CHINA，会导致无法正常播放，原因不明
            // TextToSpeech.LANG_MISSING_DATA：表示语言的数据丢失
            // TextToSpeech.LANG_NOT_SUPPORTED：不支持
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initTextToSpeech() {
        tts = new TextToSpeech(this, this);
        tts.setPitch(1.0f);
        tts.setSpeechRate(1f);
    }

}