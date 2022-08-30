package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.helper.ARCoreCheckHelper;

public class StartOptionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_option);

        ARCoreCheckHelper.PreCheckARCore(this);
    }

    public void openMeasureOption(View view) {
        Intent intent = new Intent(this, MeasureOptionActivity.class);
        startActivity(intent);
    }

    public void openStudyOption(View view) {
        Intent intent = new Intent(this, StudyOptionActivity.class);
        startActivity(intent);
    }

    public void startTestActivity(View view) {
        Intent intent = new Intent(this, TestActivity.class);
        startActivity(intent);
    }
}