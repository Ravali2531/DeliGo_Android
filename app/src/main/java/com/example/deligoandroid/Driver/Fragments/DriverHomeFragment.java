package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Switch;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverHomeFragment extends Fragment {
    private TextView statusText;
    private Switch availabilitySwitch;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId);

        // Initialize views
        statusText = view.findViewById(R.id.statusText);
        availabilitySwitch = view.findViewById(R.id.availabilitySwitch);

        // Setup availability switch
        setupAvailabilitySwitch();

        return view;
    }

    private void setupAvailabilitySwitch() {
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateDriverStatus(isChecked);
        });

        // Load current status
        databaseRef.child("isAvailable").get().addOnSuccessListener(snapshot -> {
            Boolean isAvailable = snapshot.getValue(Boolean.class);
            if (isAvailable != null) {
                availabilitySwitch.setChecked(isAvailable);
                updateStatusText(isAvailable);
            }
        });
    }

    private void updateDriverStatus(boolean isAvailable) {
        databaseRef.child("isAvailable").setValue(isAvailable)
                .addOnSuccessListener(aVoid -> updateStatusText(isAvailable));
    }

    private void updateStatusText(boolean isAvailable) {
        statusText.setText(isAvailable ? "You are available for orders" : "You are currently offline");
        statusText.setTextColor(getResources().getColor(
                isAvailable ? android.R.color.holo_green_dark : android.R.color.darker_gray));
    }
} 