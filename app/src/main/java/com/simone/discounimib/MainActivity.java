package com.simone.discounimib;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.simone.discounimib.activities.HomeActivity;
import com.simone.discounimib.activities.LoginActivity;
import com.simone.discounimib.utils.SharedPreferencesManager;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferencesManager prefManager = new SharedPreferencesManager(this);
            
            if (prefManager.getUserUid() != null && prefManager.isRememberMe()) {
                // Utente già loggato
                startActivity(new Intent(MainActivity.this, HomeActivity.class));
            } else {
                // Vai al login
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
            finish();
        }, SPLASH_TIME_OUT);
    }
}