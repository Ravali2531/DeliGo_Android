package com.example.deligoandroid.Customer.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.Customer.CustomerSupportActivity;
import com.example.deligoandroid.Customer.EditProfileActivity;
import com.example.deligoandroid.Utils.ThemeManager;
import com.example.deligoandroid.databinding.FragmentCustomerAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerAccountFragment extends Fragment {
    private FragmentCustomerAccountBinding binding;
    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference().child("customers").child(userId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomerAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        loadUserData();
        setupDarkModeToggle();
    }

    private void setupUI() {
        // Edit Profile Button
        binding.editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        // Support Section
        binding.supportSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerSupportActivity.class);
            startActivity(intent);
        });

        // Sign Out Button
        binding.signOutButton.setOnClickListener(v -> signOut());
    }

    private void setupDarkModeToggle() {
        ThemeManager themeManager = ThemeManager.getInstance(requireContext());
        
        // Set initial state based on current theme
        boolean isDarkMode = themeManager.isDarkMode();
        binding.darkModeSwitch.setChecked(isDarkMode);
        
        // Handle toggle changes
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getActivity() == null) return;
            
            // First update the saved preference
            themeManager.setDarkMode(isChecked);
            
            // Force dark mode application directly
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            
            // Force recreation of the activity to apply theme changes
            getActivity().recreate();
        });
    }

    private void loadUserData() {
        if (auth.getCurrentUser() != null) {
            // Set email
            binding.emailText.setText(auth.getCurrentUser().getEmail());

            // Load other user data from Firebase
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fullName = snapshot.child("fullName").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);

                        binding.nameText.setText(fullName);
                        binding.phoneText.setText(phone);
                        binding.addressText.setText(address);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void signOut() {
        auth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 