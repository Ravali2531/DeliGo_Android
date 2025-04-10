package com.example.deligoandroid.Customer;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.databinding.ActivityEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference().child("customers").child(userId);
            
            // Load current user data
            loadUserData();

            // Save button click listener
            binding.saveButton.setOnClickListener(v -> saveUserData());
        } else {
            finish();
        }
    }

    private void loadUserData() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && !isFinishing()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);

                    if (binding != null) {
                        binding.fullNameInput.setText(fullName);
                        binding.phoneInput.setText(phone);
                        binding.addressInput.setText(address);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing()) {
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        };
        userRef.addValueEventListener(valueEventListener);
    }

    private void saveUserData() {
        if (binding == null) return;

        String fullName = binding.fullNameInput.getText().toString().trim();
        String phone = binding.phoneInput.getText().toString().trim();
        String address = binding.addressInput.getText().toString().trim();

        if (fullName.isEmpty()) {
            binding.fullNameInput.setError("Name is required");
            return;
        }

        if (phone.isEmpty()) {
            binding.phoneInput.setError("Phone number is required");
            return;
        }

        if (address.isEmpty()) {
            binding.addressInput.setError("Address is required");
            return;
        }

        if (userRef != null && !isFinishing()) {
            userRef.child("fullName").setValue(fullName);
            userRef.child("phone").setValue(phone);
            userRef.child("address").setValue(address)
                    .addOnSuccessListener(aVoid -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && valueEventListener != null) {
            userRef.removeEventListener(valueEventListener);
        }
        binding = null;
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