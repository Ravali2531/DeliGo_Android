package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;

public class AdminActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CardView cardUserManagement, cardChatManagement;
    private ImageButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mAuth = FirebaseAuth.getInstance();
        
        // Initialize views
        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardChatManagement = findViewById(R.id.cardChatManagement);
        btnLogout = findViewById(R.id.btnLogout);

        // Set click listeners
        cardUserManagement.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        });

        cardChatManagement.setOnClickListener(v -> {
            Toast.makeText(this, "Chat Management coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
} 