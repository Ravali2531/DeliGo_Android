package com.example.deligoandroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            // Check if user is already logged in
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            
            if (currentUser != null) {
                // User is logged in, go to MainActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else {
                // User is not logged in, go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
} 