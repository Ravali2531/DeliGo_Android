package com.example.deligoandroid.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import android.widget.ArrayAdapter;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    private Spinner roleSpinner;
    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput, phoneInput;
    private Button signupButton;
    private TextView loginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews();

        // Setup signup button
        signupButton.setOnClickListener(v -> handleSignup());

        // Setup login link
        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initializeViews() {
        roleSpinner = findViewById(R.id.roleSpinner);
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        phoneInput = findViewById(R.id.phoneInput);
        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLink);

        // Setup role spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.user_roles, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);
    }

    private void handleSignup() {
        String role = roleSpinner.getSelectedItem() != null ? 
            roleSpinner.getSelectedItem().toString() : "";
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();

        // Validation
        if (role.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty() || 
            confirmPassword.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success, save user data
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserData(userId, role, fullName, email, phone);
                    } else {
                        // If sign up fails, display a message to the user.
                        Toast.makeText(SignupActivity.this, "Authentication failed: " + 
                                     task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });


    }

//    private void saveUserData(String userId, String role, String fullName, String email, String phone) {
//        // Create user data map
//        Map<String, Object> userData = new HashMap<>();
//        userData.put("name", fullName);
//        userData.put("email", email);
//        userData.put("phone", phone);
//        userData.put("role", role);
//
//        // Save user data in the appropriate collection based on role
//        mDatabase.child(role.toLowerCase() + "s").child(userId).child("store_info")
//                .setValue(userData)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(SignupActivity.this, "Signup successful!",
//                                     Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
//                        finish();
//                    } else {
//                        Toast.makeText(SignupActivity.this, "Failed to save user data: " +
//                                     task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

//    private void saveUserData(String userId, String role, String fullName, String email, String phone) {
//        // Create user data map for store_info
//        Map<String, Object> storeInfo = new HashMap<>();
//        storeInfo.put("name", fullName);
//        storeInfo.put("email", email);
//        storeInfo.put("phone", phone);
//
//        // Save role separately under restaurants/userId
//        mDatabase.child(role.toLowerCase() + "s").child(userId).child("role")
//                .setValue(role)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        // Save store info under restaurants/userId/store_info
//                        mDatabase.child(role.toLowerCase() + "s").child(userId).child("store_info")
//                                .setValue(storeInfo)
//                                .addOnCompleteListener(storeTask -> {
//                                    if (storeTask.isSuccessful()) {
//                                        Toast.makeText(SignupActivity.this, "Signup successful!",
//                                                Toast.LENGTH_SHORT).show();
//                                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
//                                        finish();
//                                    } else {
//                                        Toast.makeText(SignupActivity.this, "Failed to save store info: " +
//                                                storeTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                    } else {
//                        Toast.makeText(SignupActivity.this, "Failed to save role: " +
//                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

    private void saveUserData(String userId, String role, String fullName, String email, String phone) {
        // Create user data map for store_info if the role is 'restaurant'
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", fullName);
        userInfo.put("email", email);
        userInfo.put("phone", phone);
        userInfo.put("role", role);

        // Save role separately under roles/userId
        mDatabase.child(role.toLowerCase() + "s").child(userId).child("role")
                .setValue(role)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // For restaurants, save store info under restaurants/userId/store_info
                        if (role.equalsIgnoreCase("restaurant")) {
                            mDatabase.child("restaurants").child(userId).child("store_info")
                                    .setValue(userInfo)
                                    .addOnCompleteListener(storeTask -> {
                                        if (storeTask.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this, "Signup successful!",
                                                    Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to save store info: " +
                                                    storeTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // For other roles, save under a common 'stores' node
                            mDatabase.child(role.toLowerCase() + "s").child(userId)
                                    .setValue(userInfo)
                                    .addOnCompleteListener(storeTask -> {
                                        if (storeTask.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this, "Signup successful!",
                                                    Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to save stores info: " +
                                                    storeTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(SignupActivity.this, "Failed to save role: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}