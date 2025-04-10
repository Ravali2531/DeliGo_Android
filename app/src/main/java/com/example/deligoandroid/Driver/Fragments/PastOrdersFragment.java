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

public class PastOrdersFragment extends Fragment {
    private RecyclerView pastOrdersRecyclerView;
    private TextView noPastOrdersText;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_past_orders, container, false);

        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers").child(userId).child("orders");

        // Initialize views
        pastOrdersRecyclerView = view.findViewById(R.id.pastOrdersRecyclerView);
        noPastOrdersText = view.findViewById(R.id.noPastOrdersText);

        // Load past orders
        loadPastOrders();

        return view;
    }

    private void loadPastOrders() {
        databaseRef.orderByChild("status").equalTo("completed").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    // No orders available
                    pastOrdersRecyclerView.setVisibility(View.GONE);
                    noPastOrdersText.setVisibility(View.VISIBLE);
                } else {
                    // Orders available
                    pastOrdersRecyclerView.setVisibility(View.VISIBLE);
                    noPastOrdersText.setVisibility(View.GONE);
                    // TODO: Set up RecyclerView adapter with the orders
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
                pastOrdersRecyclerView.setVisibility(View.GONE);
                noPastOrdersText.setVisibility(View.VISIBLE);
            }
        });
    }
} 