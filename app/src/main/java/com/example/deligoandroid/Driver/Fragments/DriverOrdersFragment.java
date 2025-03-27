package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Driver.Adapters.DriverOrdersAdapter;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DriverOrdersFragment extends Fragment {
    private RecyclerView ordersRecyclerView;
    private TextView noOrdersMessage;
    private DriverOrdersAdapter adapter;
    private DatabaseReference ordersRef;
    private ValueEventListener ordersListener;
    private String driverId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_orders, container, false);
        
        // Initialize views
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
        noOrdersMessage = view.findViewById(R.id.noOrdersMessage);
        
        // Get current driver ID
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Setup RecyclerView
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DriverOrdersAdapter(driverId);
        ordersRecyclerView.setAdapter(adapter);
        
        // Initialize Firebase
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        
        // Load orders
        loadOrders();
        
        return view;
    }

    private void loadOrders() {
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Order> ordersList = new ArrayList<>();
                
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    try {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            // Set the order ID
                            order.setId(orderSnapshot.getKey());
                            
                            // Check if this order is assigned to the current driver
                            String orderDriverId = order.getDriverId();
                            if (orderDriverId != null && orderDriverId.equals(driverId)) {
                                ordersList.add(order);
                            }
                        }
                    } catch (Exception e) {
                        // Log the error but continue processing other orders
                        e.printStackTrace();
                    }
                }
                
                // Update UI based on whether there are orders
                if (ordersList.isEmpty()) {
                    ordersRecyclerView.setVisibility(View.GONE);
                    noOrdersMessage.setVisibility(View.VISIBLE);
                } else {
                    ordersRecyclerView.setVisibility(View.VISIBLE);
                    noOrdersMessage.setVisibility(View.GONE);
                    adapter.setOrders(ordersList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error
                noOrdersMessage.setText("Error loading orders");
                noOrdersMessage.setVisibility(View.VISIBLE);
                ordersRecyclerView.setVisibility(View.GONE);
            }
        };
        
        ordersRef.addValueEventListener(ordersListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the listener when the fragment is destroyed
        if (ordersRef != null && ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
    }
} 