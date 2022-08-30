package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.pcg.scaleteacher.R;

public class StudyOptionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_option);
    }

    public void startSizeStudy(View view) {
        Intent intent = new Intent(this, SizeStudyActivity.class);
        startActivity(intent);
    }

    public void startAngleStudy(View view) {
        Intent intent = new Intent(this, AngleStudyActivity.class);
        startActivity(intent);
    }

    public void goBack(View view) {
        this.finish();
    }
}