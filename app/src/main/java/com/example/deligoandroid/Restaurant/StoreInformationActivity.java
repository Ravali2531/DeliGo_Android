package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class StoreInformationActivity extends AppCompatActivity {
    private TextInputEditText storeNameInput;
    private TextInputEditText locationInput;
    private TextInputEditText phoneInput;
    private TextInputEditText emailInput;
    private TextInputEditText aboutInput;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userId;
    
    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedLocationName = "";

    private final ActivityResultLauncher<Intent> locationPicker = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                selectedLocationName = data.getStringExtra("name");
                String address = data.getStringExtra("address");
                selectedLat = data.getDoubleExtra("latitude", 0);
                selectedLng = data.getDoubleExtra("longitude", 0);
                
                locationInput.setText(address);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_information);

        try {
            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            mDatabase = FirebaseDatabase.getInstance().getReference();
            userId = mAuth.getCurrentUser().getUid();

            // Initialize views
            initializeViews();

            // Load existing data
            loadStoreInformation();

            // Setup click listeners
            setupClickListeners();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            storeNameInput = findViewById(R.id.storeNameInput);
            locationInput = findViewById(R.id.locationInput);
            phoneInput = findViewById(R.id.phoneInput);
            emailInput = findViewById(R.id.emailInput);
            aboutInput = findViewById(R.id.aboutInput);
            saveButton = findViewById(R.id.saveButton);
        } catch (Exception e) {
            Toast.makeText(this, "Error finding views: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void setupClickListeners() {
        locationInput.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            locationPicker.launch(intent);
        });

        saveButton.setOnClickListener(v -> saveStoreInformation());
    }

    private void loadStoreInformation() {
        try {
            mDatabase.child("restaurants").child(userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                try {
                                    String fullName = dataSnapshot.hasChild("fullName") ? 
                                        String.valueOf(dataSnapshot.child("fullName").getValue()) : "";
                                    
                                    // Load location data
                                    if (dataSnapshot.hasChild("location")) {
                                        DataSnapshot locationSnapshot = dataSnapshot.child("location");
                                        String locationName = locationSnapshot.hasChild("name") ?
                                            String.valueOf(locationSnapshot.child("name").getValue()) : "";
                                        selectedLat = locationSnapshot.hasChild("latitude") ?
                                            (double) locationSnapshot.child("latitude").getValue() : 0;
                                        selectedLng = locationSnapshot.hasChild("longitude") ?
                                            (double) locationSnapshot.child("longitude").getValue() : 0;
                                        
                                        locationInput.setText(locationName);
                                        selectedLocationName = locationName;
                                    }
                                    
                                    String phone = dataSnapshot.hasChild("phone") ? 
                                        String.valueOf(dataSnapshot.child("phone").getValue()) : "";
                                    String email = dataSnapshot.hasChild("email") ? 
                                        String.valueOf(dataSnapshot.child("email").getValue()) : "";
                                    String about = dataSnapshot.hasChild("about") ? 
                                        String.valueOf(dataSnapshot.child("about").getValue()) : "";

                                    if (storeNameInput != null) storeNameInput.setText(fullName);
                                    if (phoneInput != null) phoneInput.setText(phone);
                                    if (emailInput != null) emailInput.setText(email);
                                    if (aboutInput != null) aboutInput.setText(about);

                                } catch (Exception e) {
                                    Toast.makeText(StoreInformationActivity.this,
                                        "Error loading data: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(StoreInformationActivity.this, 
                                "Failed to load store information: " + databaseError.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error loading information: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void saveStoreInformation() {
        try {
            // Create a map of store information
            Map<String, Object> storeUpdates = new HashMap<>();
            
            if (storeNameInput != null) {
                storeUpdates.put("fullName", storeNameInput.getText().toString().trim());
            }
            
            // Save location information
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("name", selectedLocationName);
            locationData.put("latitude", selectedLat);
            locationData.put("longitude", selectedLng);
            storeUpdates.put("location", locationData);
            
            if (phoneInput != null) {
                storeUpdates.put("phone", phoneInput.getText().toString().trim());
            }
            if (emailInput != null) {
                storeUpdates.put("email", emailInput.getText().toString().trim());
            }
            if (aboutInput != null) {
                storeUpdates.put("about", aboutInput.getText().toString().trim());
            }

            // Update the database
            mDatabase.child("restaurants").child(userId)
                    .updateChildren(storeUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(StoreInformationActivity.this, 
                                "Store information updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(StoreInformationActivity.this, 
                                "Failed to update store information: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error saving information: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
} 