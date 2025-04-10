package com.example.deligoandroid.Authentication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput;
    private Button resetButton;
    private TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        resetButton = findViewById(R.id.resetButton);
        backToLogin = findViewById(R.id.backToLogin);

        // Reset button click
        resetButton.setOnClickListener(v -> handleResetPassword());

        // Back to login click
        backToLogin.setOnClickListener(v -> finish());
    }

    private void handleResetPassword() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }

        // Show loading state
        resetButton.setEnabled(false);
        resetButton.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent to your email",
                                Toast.LENGTH_LONG).show();
                        finish(); // Go back to login screen
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                    resetButton.setEnabled(true);
                    resetButton.setText("Send Reset Link");
                });
    }
} 