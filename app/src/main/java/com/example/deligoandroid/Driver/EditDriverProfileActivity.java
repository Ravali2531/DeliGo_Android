package com.example.deligoandroid.Driver;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditDriverProfileActivity extends AppCompatActivity {
    private EditText fullNameInput;
    private EditText phoneNumberInput;
    private Button saveButton;
    private DatabaseReference driverRef;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_driver_profile);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        // Initialize Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            driverId = currentUser.getUid();
            driverRef = FirebaseDatabase.getInstance().getReference()
                    .child("drivers")
                    .child(driverId);
        }

        // Initialize views
        fullNameInput = findViewById(R.id.fullNameInput);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        saveButton = findViewById(R.id.saveButton);

        // Load current profile data
        loadProfileData();

        // Set up save button
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadProfileData() {
        if (driverRef == null) return;

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fullName = snapshot.child("fullName").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);

                if (fullName != null) {
                    fullNameInput.setText(fullName);
                }
                if (phone != null) {
                    phoneNumberInput.setText(phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditDriverProfileActivity.this, 
                    "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String fullName = fullNameInput.getText().toString().trim();
        String phone = phoneNumberInput.getText().toString().trim();

        if (fullName.isEmpty()) {
            fullNameInput.setError("Name is required");
            return;
        }

        if (phone.isEmpty()) {
            phoneNumberInput.setError("Phone number is required");
            return;
        }

        if (driverRef == null) return;

        // Update profile data
        driverRef.child("fullName").setValue(fullName);
        driverRef.child("phone").setValue(phone)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 