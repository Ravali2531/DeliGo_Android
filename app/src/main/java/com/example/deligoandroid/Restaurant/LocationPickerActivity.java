package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private TextView selectedLocationText;
    private TextView locationDetailsText;
    private Place selectedPlace;
    private Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Initialize Geocoder
        geocoder = new Geocoder(this, Locale.getDefault());

        // Initialize views
        selectedLocationText = findViewById(R.id.selectedLocationText);
        locationDetailsText = findViewById(R.id.locationDetailsText);
        ImageButton backButton = findViewById(R.id.backButton);
        
        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup Places Autocomplete
        setupPlacesAutocomplete();

        // Setup click listeners
        backButton.setOnClickListener(v -> finish());
        
        findViewById(R.id.confirmButton).setOnClickListener(v -> {
            if (selectedPlace != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", selectedPlace.getName());
                resultIntent.putExtra("address", selectedPlace.getAddress());
                resultIntent.putExtra("latitude", selectedPlace.getLatLng().latitude);
                resultIntent.putExtra("longitude", selectedPlace.getLatLng().longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPlacesAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            // Specify the types of place data to return
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS
            ));

            // Set up a PlaceSelectionListener to handle the response
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    selectedPlace = place;
                    if (place.getLatLng() != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
                        updateLocationTexts(place);
                    }
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(LocationPickerActivity.this,
                            "Error: " + status.getStatusMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Set default location (e.g., city center)
        LatLng defaultLocation = new LatLng(0, 0); // Replace with your default location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

        // Setup map click listener
        mMap.setOnCameraIdleListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            updateSelectedLocation(center);
        });

        // Enable my location button if permission is granted
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            // Handle permission not granted
        }
    }

    private void updateSelectedLocation(LatLng latLng) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressText = address.getAddressLine(0);
                String locationName = address.getFeatureName();
                
                selectedLocationText.setText(locationName != null ? locationName : "Selected Location");
                locationDetailsText.setText(addressText);
                
                // Create a Place object from the geocoded address
                selectedPlace = Place.builder()
                    .setName(locationName)
                    .setAddress(addressText)
                    .setLatLng(latLng)
                    .build();
            } else {
                selectedLocationText.setText("Selected Location");
                locationDetailsText.setText(String.format("Lat: %f, Lng: %f",
                        latLng.latitude, latLng.longitude));
            }
        } catch (IOException e) {
            selectedLocationText.setText("Selected Location");
            locationDetailsText.setText(String.format("Lat: %f, Lng: %f",
                    latLng.latitude, latLng.longitude));
        }
    }

    private void updateLocationTexts(Place place) {
        selectedLocationText.setText(place.getName());
        locationDetailsText.setText(place.getAddress());
    }
} 