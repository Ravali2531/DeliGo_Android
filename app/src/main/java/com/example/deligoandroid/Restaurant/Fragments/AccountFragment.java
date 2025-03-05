package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Utils.PreferencesManager;
import com.example.deligoandroid.databinding.FragmentAccountBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class AccountFragment extends Fragment {
    private FragmentAccountBinding binding;
    private FirebaseAuth auth;
    private PreferencesManager preferencesManager;
    private boolean isCurrentlyFrench = false;
    private Handler mainHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        preferencesManager = new PreferencesManager(requireContext());
        mainHandler = new Handler(Looper.getMainLooper());
        // Load saved language preference
        isCurrentlyFrench = preferencesManager.getLanguage().equals("fr");
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
        }

        // Set dark mode switch state
        binding.darkModeSwitch.setChecked(preferencesManager.isDarkMode());
        
        // Set language switch state
        binding.languageSwitch.setChecked(isCurrentlyFrench);
    }

    private void setupClickListeners() {
        // Store Hours
        binding.storeHoursLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), getString(R.string.store_hours_coming_soon), Toast.LENGTH_SHORT).show();
        });

        // Store Information
        binding.storeInfoLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), getString(R.string.store_info_coming_soon), Toast.LENGTH_SHORT).show();
        });

        // Dark Mode Switch
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                // Save current tab and dark mode preference
                int selectedItemId = getBottomNavSelectedItemId();
                preferencesManager.setSelectedTabId(selectedItemId);
                preferencesManager.setDarkMode(isChecked);

                // Delay the theme change slightly to allow preferences to be saved
                mainHandler.postDelayed(() -> {
                    if (isAdded() && getActivity() != null) {
                        AppCompatDelegate.setDefaultNightMode(
                            isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                        );
                    }
                }, 100);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to change theme", Toast.LENGTH_SHORT).show();
            }
        });

        // Language Switch
        binding.languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                isCurrentlyFrench = isChecked;
                // Save current tab
                int selectedItemId = getBottomNavSelectedItemId();
                preferencesManager.setSelectedTabId(selectedItemId);
                
                // Save language preference
                String languageCode = isCurrentlyFrench ? "fr" : "en";
                preferencesManager.setLanguage(languageCode);

                // Delay the locale change slightly
                mainHandler.postDelayed(() -> {
                    if (isAdded() && getActivity() != null) {
                        updateLocale(languageCode);
                    }
                }, 100);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to change language", Toast.LENGTH_SHORT).show();
            }
        });

        // Sign Out
        binding.signOutButton.setOnClickListener(v -> signOut());
    }

    private int getBottomNavSelectedItemId() {
        try {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
            return bottomNav != null ? bottomNav.getSelectedItemId() : R.id.navigation_account;
        } catch (Exception e) {
            return R.id.navigation_account;
        }
    }

    private void updateLocale(String languageCode) {
        try {
            // Update locale
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            // Update configuration
            Resources resources = requireContext().getResources();
            Configuration config = new Configuration(resources.getConfiguration());
            config.setLocale(locale);
            resources.updateConfiguration(config, resources.getDisplayMetrics());

            // Recreate activity
            if (getActivity() != null) {
                getActivity().recreate();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to update language", Toast.LENGTH_SHORT).show();
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