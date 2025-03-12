package com.example.deligoandroid.Driver.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverAccountFragment extends Fragment {
    private TextView nameText, emailText, phoneText, vehicleTypeText;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_account, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId);

        // Initialize views
        initializeViews(view);
        
        // Load driver data
        loadDriverData();

        // Setup logout button
        view.findViewById(R.id.logoutButton).setOnClickListener(v -> logout());

        return view;
    }

    private void initializeViews(View view) {
        nameText = view.findViewById(R.id.nameText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        vehicleTypeText = view.findViewById(R.id.vehicleTypeText);
    }

    private void loadDriverData() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    nameText.setText(snapshot.child("name").getValue(String.class));
                    emailText.setText(snapshot.child("email").getValue(String.class));
                    phoneText.setText(snapshot.child("phone").getValue(String.class));
                    vehicleTypeText.setText(snapshot.child("vehicleType").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }
} 