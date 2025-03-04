package com.example.deligoandroid.Restaurant.Fragments;

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
import com.example.deligoandroid.Utils.PreferencesManager;
import com.example.deligoandroid.databinding.FragmentAccountBinding;
import com.google.firebase.auth.FirebaseAuth;

public class AccountFragment extends Fragment {
    private FragmentAccountBinding binding;
    private FirebaseAuth auth;
    private PreferencesManager preferencesManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        preferencesManager = new PreferencesManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupClickListeners();
    }

    private void setupUI() {
        // Set email from Firebase
        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            binding.emailText.setText(email);
            binding.emailSubText.setText(email);
        }

        // Set dark mode switch state
        binding.darkModeSwitch.setChecked(preferencesManager.isDarkMode());
    }

    private void setupClickListeners() {
        // Store Hours
        binding.storeHoursLayout.setOnClickListener(v -> {
            // TODO: Implement store hours screen
            Toast.makeText(requireContext(), "Store Hours - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        // Store Information
        binding.storeInfoLayout.setOnClickListener(v -> {
            // TODO: Implement store information screen
            Toast.makeText(requireContext(), "Store Information - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        // Dark Mode Switch
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Language Switch
        binding.languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Implement language change
            Toast.makeText(requireContext(), "Language Change - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        // Sign Out
        binding.signOutButton.setOnClickListener(v -> signOut());
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