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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class DriverAccountFragment extends Fragment {
    private TextView nameText;
    private Switch darkModeSwitch;
    private DatabaseReference databaseRef;
    private String userId;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_account, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId);

        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initialize views
        initializeViews(view);
        setupClickListeners(view);

        // Load driver name
        loadDriverName();

        return view;
    }

    private void initializeViews(View view) {
//        nameText = view.findViewById(R.id.nameText);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);

        // Set dark mode switch state
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.profileSection).setOnClickListener(v -> {
            // TODO: Navigate to profile details
            Toast.makeText(getContext(), "Profile clicked", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.orderHistorySection).setOnClickListener(v -> {
            // TODO: Navigate to order history
            Toast.makeText(getContext(), "Order History clicked", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.earningsSection).setOnClickListener(v -> {
            // TODO: Navigate to earnings
            Toast.makeText(getContext(), "Earnings clicked", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.helpSection).setOnClickListener(v -> {
            // TODO: Navigate to help
            Toast.makeText(getContext(), "Help clicked", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.signOutButton).setOnClickListener(v -> logout());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void loadDriverName() {
        databaseRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.getValue(String.class);
                if (name != null) {
                    nameText.setText(name);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 