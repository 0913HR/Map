package com.zzc.mapsassistant.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.zzc.mapsassistant.R;
import com.zzc.mapsassistant.utils.ThreadUtils;

public class StartActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        getToolColorStatus();
        ThreadUtils.runInThread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                Intent intent=new Intent(StartActivity.this, MainActivity .class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {

    }
}