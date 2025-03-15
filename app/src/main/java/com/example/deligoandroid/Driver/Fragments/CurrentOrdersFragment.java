package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CurrentOrdersFragment extends Fragment {
    private RecyclerView currentOrdersRecyclerView;
    private TextView noCurrentOrdersText;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_orders, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId).child("orders");

        // Initialize views
        currentOrdersRecyclerView = view.findViewById(R.id.currentOrdersRecyclerView);
        noCurrentOrdersText = view.findViewById(R.id.noCurrentOrdersText);

        // Load current orders
        loadCurrentOrders();

        return view;
    }

    private void loadCurrentOrders() {
        databaseRef.orderByChild("status").equalTo("active").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    // No orders available
                    currentOrdersRecyclerView.setVisibility(View.GONE);
                    noCurrentOrdersText.setVisibility(View.VISIBLE);
                } else {
                    // Orders available
                    currentOrdersRecyclerView.setVisibility(View.VISIBLE);
                    noCurrentOrdersText.setVisibility(View.GONE);
                    // TODO: Set up RecyclerView adapter with the orders
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
                currentOrdersRecyclerView.setVisibility(View.GONE);
                noCurrentOrdersText.setVisibility(View.VISIBLE);
            }
        });
    }
} 