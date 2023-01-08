package com.pcg.scaleteacher.main;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.pcg.scaleteacher.R;
import com.pcg.scaleteacher.base.ConstantBase;
import com.pcg.scaleteacher.helper.ARCoreCheckHelper;

public class StartOptionActivity extends ConstantBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_option);
        ARCoreCheckHelper.PreCheckARCore(this);
    }

    public void chooseStartOption(View view) {
        Intent intent = new Intent(this, IntroductionActivity.class);
        int viewId = view.getId();
        if (viewId == R.id.start_formal_teaching)
            intent.putExtra(basicModeTag, BasicMode.FORMAL_STYLE);
        else if (viewId == R.id.start_free_teaching)
            intent.putExtra(basicModeTag, BasicMode.FREE_STYLE);
        else if (viewId == R.id.start_test)
            intent.putExtra(basicModeTag, BasicMode.TEST);
        startActivity(intent);
    }
}