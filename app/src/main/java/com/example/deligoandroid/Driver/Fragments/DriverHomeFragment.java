package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverHomeFragment extends Fragment {
    private TextView statusText;
    private Switch availabilitySwitch;
    private RecyclerView availableOrdersRecyclerView;
    private TextView noOrdersText;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId);

        // Setup header
        View header = view.findViewById(R.id.header);
        ((TextView) header.findViewById(R.id.headerTitle)).setText("DeliGo Driver");

        // Initialize views
        statusText = view.findViewById(R.id.statusText);
        availabilitySwitch = view.findViewById(R.id.availabilitySwitch);
        availableOrdersRecyclerView = view.findViewById(R.id.availableOrdersRecyclerView);
        noOrdersText = view.findViewById(R.id.noOrdersText);

        // Setup availability switch
        setupAvailabilitySwitch();

        // Load available orders
        loadAvailableOrders();

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

    private void loadAvailableOrders() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference().child("orders");
        ordersRef.orderByChild("status").equalTo("pending").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    // No orders available
                    availableOrdersRecyclerView.setVisibility(View.GONE);
                    noOrdersText.setVisibility(View.VISIBLE);
                } else {
                    // Orders available
                    availableOrdersRecyclerView.setVisibility(View.VISIBLE);
                    noOrdersText.setVisibility(View.GONE);
                    // TODO: Set up RecyclerView adapter with the orders
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
                availableOrdersRecyclerView.setVisibility(View.GONE);
                noOrdersText.setVisibility(View.VISIBLE);
            }
        });
    }
} 