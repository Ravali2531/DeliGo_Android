package com.example.deligoandroid.Driver.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.Driver.DriverSupportActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import com.example.deligoandroid.Driver.EditDriverProfileActivity;

public class DriverAccountFragment extends Fragment {
    private TextView nameText;
    private Switch darkModeSwitch;
    private Button signOutButton;
    private LinearLayout supportSection;
    private LinearLayout profileSection;
    private DatabaseReference databaseRef;
    private String driverId;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_account, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            driverId = currentUser.getUid();
            databaseRef = FirebaseDatabase.getInstance().getReference().child("drivers").child(driverId);
        }

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initialize views
        initializeViews(view);
        setupClickListeners();
        loadDriverName();

        return view;
    }

    private void initializeViews(View view) {
        nameText = view.findViewById(R.id.nameText);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        signOutButton = view.findViewById(R.id.signOutButton);
        supportSection = view.findViewById(R.id.supportSection);
        profileSection = view.findViewById(R.id.profileSection);

        // Set dark mode switch state
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Set up profile section click listener
        profileSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditDriverProfileActivity.class);
            startActivity(intent);
        });
    }

    private void setupClickListeners() {
        signOutButton.setOnClickListener(v -> signOut());
        
        supportSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DriverSupportActivity.class);
            startActivity(intent);
        });
    }

    private void loadDriverName() {
        if (databaseRef == null || nameText == null) return;

        databaseRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null && nameText != null && isAdded()) {
                    nameText.setText(name);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void signOut() {
        mAuth.signOut();
        // Navigate to login activity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        nameText = null;
        darkModeSwitch = null;
        signOutButton = null;
        supportSection = null;
        profileSection = null;
    }
} 