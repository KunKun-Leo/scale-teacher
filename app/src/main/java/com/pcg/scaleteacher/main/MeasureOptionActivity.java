package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.pcg.scaleteacher.R;

public class MeasureOptionActivity extends AppCompatActivity {

    public static final String isMeasuringLength = "com.pcg.scaleteacher.main.is_measuring_length";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure_option);
    }

    public void startTouchMeasure(View view) {
        Intent intent = new Intent(this, TouchMeasureActivity.class);
        startActivity(intent);
    }

    public void startMoveMeasure(View view) {
        Intent intent = new Intent(this, MoveMeasureActivity.class);
        intent.putExtra(isMeasuringLength, true);
        startActivity(intent);
    }

    public void startAngleMeasure(View view) {
        Intent intent = new Intent(this, MoveMeasureActivity.class);
        intent.putExtra(isMeasuringLength, false);
        startActivity(intent);
    }

    public void goBack(View view) {
        this.finish();
    }
}