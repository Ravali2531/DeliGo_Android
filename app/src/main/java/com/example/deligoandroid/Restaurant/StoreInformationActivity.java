package com.example.deligoandroid.Restaurant;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreInformationActivity extends AppCompatActivity {
    private TextInputEditText storeNameInput, phoneInput, emailInput, aboutInput;
    private AutoCompleteTextView locationInput;
    private Button saveButton;
    private DatabaseReference storeInfoRef;
    private DatabaseReference locationRef;
    private PlacesClient placesClient;
    private ArrayAdapter<String> placesAdapter;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    private double selectedLat = 0;
    private double selectedLng = 0;
    private String selectedLocationName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_information);

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Initialize views
        initializeViews();

        // Initialize Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference()
                .child("restaurants").child(userId);
        storeInfoRef = restaurantRef.child("store_info");
        locationRef = restaurantRef.child("location");

        // Setup location autocomplete
        setupLocationAutocomplete();

        // Load existing store information
        loadStoreInformation();

        // Setup save button
        saveButton.setOnClickListener(v -> saveStoreInformation());
    }

    private void initializeViews() {
        storeNameInput = findViewById(R.id.storeNameInput);
        locationInput = findViewById(R.id.locationInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        aboutInput = findViewById(R.id.aboutInput);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupLocationAutocomplete() {
        placesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);
        locationInput.setAdapter(placesAdapter);
        locationInput.setThreshold(3);

        locationInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position < predictions.size()) {
                AutocompletePrediction prediction = predictions.get(position);
                fetchPlaceDetails(prediction.getPlaceId());
            }
        });

        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 3) {
                    getPlacePredictions(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            if (place.getLatLng() != null) {
                selectedLat = place.getLatLng().latitude;
                selectedLng = place.getLatLng().longitude;
                selectedLocationName = place.getName();
            }
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                Toast.makeText(this, "Error fetching place details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStoreInformation() {
        storeInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Load store name
                    if (dataSnapshot.hasChild("name")) {
                        storeNameInput.setText(dataSnapshot.child("name").getValue(String.class));
                    }

                    // Load address
                    if (dataSnapshot.hasChild("address")) {
                        locationInput.setText(dataSnapshot.child("address").getValue(String.class));
                    }

                    // Load phone
                    if (dataSnapshot.hasChild("phone")) {
                        phoneInput.setText(dataSnapshot.child("phone").getValue(String.class));
                    }

                    // Load email
                    if (dataSnapshot.hasChild("email")) {
                        emailInput.setText(dataSnapshot.child("email").getValue(String.class));
                    }

                    // Load description
                    if (dataSnapshot.hasChild("description")) {
                        aboutInput.setText(dataSnapshot.child("description").getValue(String.class));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(StoreInformationActivity.this, 
                    "Error loading store information: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Load location data
        locationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    selectedLat = dataSnapshot.child("latitude").getValue(Double.class);
                    selectedLng = dataSnapshot.child("longitude").getValue(Double.class);
                    selectedLocationName = dataSnapshot.child("name").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(StoreInformationActivity.this,
                    "Error loading location data: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getPlacePredictions(String query) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            predictions = response.getAutocompletePredictions();
            List<String> addresses = new ArrayList<>();
            for (AutocompletePrediction prediction : predictions) {
                addresses.add(prediction.getFullText(null).toString());
            }
            placesAdapter.clear();
            placesAdapter.addAll(addresses);
            placesAdapter.notifyDataSetChanged();
        }).addOnFailureListener(exception -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Toast.makeText(this, "Error: " + apiException.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStoreInformation() {
        String storeName = storeNameInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String about = aboutInput.getText().toString().trim();

        if (storeName.isEmpty() || location.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save store info
        Map<String, Object> storeUpdates = new HashMap<>();
        storeUpdates.put("name", storeName);
        storeUpdates.put("address", location);
        storeUpdates.put("phone", phone);
        storeUpdates.put("email", email);
        storeUpdates.put("description", about);

        // Save location data
        Map<String, Object> locationUpdates = new HashMap<>();
        locationUpdates.put("latitude", selectedLat);
        locationUpdates.put("longitude", selectedLng);
        locationUpdates.put("name", selectedLocationName);

        // Perform both updates
        Map<String, Object> allUpdates = new HashMap<>();
        allUpdates.put("store_info", storeUpdates);
        allUpdates.put("location", locationUpdates);

        // Update both nodes
        locationRef.getParent().updateChildren(allUpdates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Store information saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Failed to save information: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show());
    }
} 