package com.notificationcapture.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.notificationcapture.app.utils.SecurityPreferencesManager;

public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 3000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Aplicar tema guardado antes de inflar el layout para evitar flash
        SecurityPreferencesManager prefsManager = new SecurityPreferencesManager(this);
        int nightMode = prefsManager.getNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);

        setContentView(R.layout.activity_splash);

        LinearLayout splashContent = findViewById(R.id.splashContent);
        Animation heartbeat = AnimationUtils.loadAnimation(this, R.anim.heartbeat);
        splashContent.startAnimation(heartbeat);

        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToMain, MIN_SPLASH_DURATION_MS);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
