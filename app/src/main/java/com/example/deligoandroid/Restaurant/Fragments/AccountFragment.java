package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.example.deligoandroid.Authentication.LoginActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class AccountFragment extends Fragment {
    private TextView restaurantNameText, emailText, phoneText;
    private Button logoutButton;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        initializeViews(view);
        loadRestaurantDetails();

        return view;
    }

    private void initializeViews(View view) {
        restaurantNameText = view.findViewById(R.id.restaurantNameText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        logoutButton = view.findViewById(R.id.logoutButton);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void loadRestaurantDetails() {
        databaseRef.child("restaurants").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        restaurantNameText.setText(dataSnapshot.child("restaurantName").getValue(String.class));
                        emailText.setText(dataSnapshot.child("email").getValue(String.class));
                        phoneText.setText(dataSnapshot.child("phone").getValue(String.class));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getContext(), LoginActivity.class));
        requireActivity().finish();
    }
} 