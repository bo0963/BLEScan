package com.example.blebeaconscan;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.blebeaconscan.SQL.BleBeaconDbHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mTimerTextView,mDeviceNameTextView;
//    private BleBeaconDbHelper mbleBeaconDbHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI參考
        mTimerTextView = findViewById(R.id.timerTextView);

        //實驗持續時間
        class TimerThread extends Thread {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void run() {
                final long initialTimeMillis = System.currentTimeMillis();
                {
                    while (!isDestroyed()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long currentTimeMillis = System.currentTimeMillis() - initialTimeMillis;
                                mTimerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d"
                                        , currentTimeMillis / 3600000, (currentTimeMillis % 3600000) / 60000
                                        , (currentTimeMillis % 3600000 % 60000) / 1000));
                            }
                        });
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        new TimerThread().start();
        findViewById(R.id.bleScanButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bleScanButton:
                startActivity(new Intent(this,BleScanActivity.class));
                break;
        }
    }
}
