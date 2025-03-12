package com.example.deligoandroid.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.Admin.AdminActivity;
import com.example.deligoandroid.Customer.Activities.CustomerHomeActivity;
import com.example.deligoandroid.Driver.DriverDocumentsActivity;
import com.example.deligoandroid.Driver.DriverHomeActivity;
import com.example.deligoandroid.MainActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.deligoandroid.Restaurant.RestaurantDocumentsActivity;
import com.example.deligoandroid.Restaurant.RestaurantHomeActivity;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPassword, signupText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        signupText = findViewById(R.id.signupText);

        // Login Button Click
        loginButton.setOnClickListener(v -> handleLogin());

        // Forgot Password Click
        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Signup Click
        signupText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading progress
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Get user role and redirect accordingly
                        String userId = mAuth.getCurrentUser().getUid();
                        checkUserRoleAndRedirect(userId);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + 
                                     task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    }
                });
    }

    private void checkUserRoleAndRedirect(String userId) {
        // First check if user is admin
        mDatabase.child("admins").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    redirectUser("Admin");
                    return;
                }
                // If not admin, check other roles
                checkInCustomers(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInCustomers(String userId) {
        mDatabase.child("customers").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    redirectUser("Customer");
                    return;
                }
                checkInDrivers(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInDrivers(String userId) {
        mDatabase.child("drivers").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Check if documents are submitted
                    Boolean documentsSubmitted = dataSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    if (documentsSubmitted == null || !documentsSubmitted) {
                        // Documents not submitted, redirect to upload page
                        Intent intent = new Intent(LoginActivity.this, DriverDocumentsActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Documents submitted, go to driver home
                        Intent intent = new Intent(LoginActivity.this, DriverHomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    return;
                }
                // Check in restaurants if not a driver
                checkInRestaurants(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInRestaurants(String userId) {
        mDatabase.child("restaurants").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Check if documents are submitted
                    Boolean documentsSubmitted = dataSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    if (documentsSubmitted == null || !documentsSubmitted) {
                        // Documents not submitted, redirect to upload page
                        Intent intent = new Intent(LoginActivity.this, RestaurantDocumentsActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Documents submitted, go to restaurant home
                        Intent intent = new Intent(LoginActivity.this, RestaurantHomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    return;
                }
                // User not found in any role
                Toast.makeText(LoginActivity.this, "User role not found", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                loginButton.setEnabled(true);
                loginButton.setText("Login");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void redirectUser(String role) {
        Toast.makeText(this, "Logged in successfully as " + role, Toast.LENGTH_SHORT).show();
        Intent intent;
        switch (role) {
            case "Admin":
                intent = new Intent(LoginActivity.this, AdminActivity.class);
                break;
            case "Customer":
                intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                break;
            case "Driver":
                intent = new Intent(LoginActivity.this, DriverHomeActivity.class);
                break;
            case "Restaurant":
                intent = new Intent(LoginActivity.this, RestaurantHomeActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
        finish();
    }

    private void handleDatabaseError(DatabaseError databaseError) {
        Toast.makeText(LoginActivity.this, 
            "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }
}
