package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.Restaurant.ScheduledOrdersActivity;
import com.example.deligoandroid.Restaurant.StoreHoursActivity;
import com.example.deligoandroid.Restaurant.StoreInformationActivity;
import com.example.deligoandroid.Restaurant.SalesReportsActivity;
import com.example.deligoandroid.Restaurant.BestSellingDishesActivity;
import com.example.deligoandroid.Restaurant.SpecialDiscountsActivity;
import com.example.deligoandroid.Restaurant.RestaurantSupportActivity;
import com.example.deligoandroid.databinding.FragmentRestaurantAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountFragment extends Fragment {
    private FragmentRestaurantAccountBinding binding;
    private DatabaseReference restaurantRef;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRestaurantAccountBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        setupClickListeners();
        loadRestaurantData();
        return binding.getRoot();
    }

    private void setupClickListeners() {
        binding.storeHoursSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StoreHoursActivity.class);
            startActivity(intent);
        });

        binding.storeInfoSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StoreInformationActivity.class);
            startActivity(intent);
        });

        binding.salesReportsSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SalesReportsActivity.class);
            startActivity(intent);
        });

        binding.bestSellingDishesSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BestSellingDishesActivity.class);
            startActivity(intent);
        });

        binding.scheduledOrdersLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ScheduledOrdersActivity.class);
            startActivity(intent);
        });

        binding.specialDiscountsSection.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SpecialDiscountsActivity.class);
            startActivity(intent);
        });

        binding.supportLayout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RestaurantSupportActivity.class);
            startActivity(intent);
        });

        binding.signOutButton.setOnClickListener(v -> signOut());
    }

    private void signOut() {
        auth.signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadRestaurantData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(userId)
                .child("store_info");

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                    binding.restaurantNameText.setText(name);
                    binding.restaurantEmailText.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading restaurant data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 