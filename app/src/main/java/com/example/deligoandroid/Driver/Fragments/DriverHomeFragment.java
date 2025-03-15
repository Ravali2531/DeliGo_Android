package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.Driver.Adapters.DriverOrdersAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;
import java.text.NumberFormat;
import java.util.Locale;

public class DriverHomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private DriverOrdersAdapter adapter;
    private TextView noOrdersText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView earningsText;
    private TextView deliveriesText;
    private Switch availabilitySwitch;
    private String currentDriverId;
    private DatabaseReference ordersRef;
    private DatabaseReference driversRef;
    private ValueEventListener ordersListener;
    private ValueEventListener earningsListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.ordersRecyclerView);
        noOrdersText = view.findViewById(R.id.noOrdersText);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        earningsText = view.findViewById(R.id.earningsText);
        deliveriesText = view.findViewById(R.id.deliveriesText);
        availabilitySwitch = view.findViewById(R.id.availabilitySwitch);

        // Get current driver ID
        currentDriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DriverOrdersAdapter(currentDriverId);
        recyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadOrders);

        // Initialize Firebase references
        ordersRef = FirebaseDatabase.getInstance().getReference().child("orders");
        driversRef = FirebaseDatabase.getInstance().getReference().child("drivers").child(currentDriverId);

        // Setup availability switch
        setupAvailabilitySwitch();

        // Load data
        loadOrders();
        loadEarnings();

        return view;
    }

    private void setupAvailabilitySwitch() {
        // Get initial availability state
        driversRef.child("isAvailable").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean isAvailable = dataSnapshot.getValue(Boolean.class);
                if (isAvailable != null) {
                    availabilitySwitch.setChecked(isAvailable);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load availability status", Toast.LENGTH_SHORT).show();
            }
        });

        // Setup switch listener
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            driversRef.child("isAvailable").setValue(isChecked)
                .addOnSuccessListener(aVoid -> {
                    String message = isChecked ? "You are now available for deliveries" : "You are now offline";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert switch state if update fails
                    availabilitySwitch.setChecked(!isChecked);
                    Toast.makeText(getContext(), "Failed to update availability", Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void loadEarnings() {
        if (earningsListener != null) {
            ordersRef.removeEventListener(earningsListener);
        }

        earningsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalEarnings = 0;
                int deliveryCount = 0;
                
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String driverId = orderSnapshot.child("driverId").getValue(String.class);
                    String status = orderSnapshot.child("status").getValue(String.class);
                    
                    if (currentDriverId.equals(driverId) && "delivered".equals(status)) {
                        Double amount = orderSnapshot.child("total").getValue(Double.class);
                        if (amount != null) {
                            totalEarnings += amount;
                            deliveryCount++;
                        }
                    }
                }

                // Format and display earnings
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
                earningsText.setText(currencyFormat.format(totalEarnings));
                deliveriesText.setText(deliveryCount + " Deliveries");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to load earnings", Toast.LENGTH_SHORT).show();
            }
        };

        ordersRef.addValueEventListener(earningsListener);
    }

    private void loadOrders() {
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Order> driverOrders = new ArrayList<>();
                
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String driverId = orderSnapshot.child("driverId").getValue(String.class);
                    if (driverId != null && driverId.equals(currentDriverId)) {
                        Order order = new Order();
                        
                        // Map the data from Firebase to our Order model
                        order.setOrderId(orderSnapshot.getKey());
                        order.setCustomerId(orderSnapshot.child("customerId").getValue(String.class));
                        order.setCustomerName(orderSnapshot.child("customerName").getValue(String.class));
                        order.setRestaurantId(orderSnapshot.child("restaurantId").getValue(String.class));
                        order.setRestaurantName(orderSnapshot.child("restaurantName").getValue(String.class));
                        
                        // Safely handle numeric values with null checks
                        Double totalAmount = orderSnapshot.child("total").getValue(Double.class);
                        order.setTotalAmount(totalAmount != null ? totalAmount : 0.0);
                        
                        order.setStatus(orderSnapshot.child("status").getValue(String.class));
                        order.setOrderStatus(orderSnapshot.child("order_status").getValue(String.class));
                        order.setDriverId(driverId);
                        order.setDriverName(orderSnapshot.child("driverName").getValue(String.class));
                        
                        // Safely handle boolean with null check
                        Boolean driverAccepted = orderSnapshot.child("driverAccepted").getValue(Boolean.class);
                        order.setDriverAccepted(driverAccepted != null ? driverAccepted : false);
                        
                        order.setDeliveryOption(orderSnapshot.child("deliveryOption").getValue(String.class));
                        order.setDeliveryAddress(orderSnapshot.child("deliveryAddress").getValue(String.class));
                        
                        // Handle location data with null checks
                        Double lat = orderSnapshot.child("deliveryLatitude").getValue(Double.class);
                        Double lng = orderSnapshot.child("deliveryLongitude").getValue(Double.class);
                        order.setDeliveryLatitude(lat != null ? lat : 0.0);
                        order.setDeliveryLongitude(lng != null ? lng : 0.0);
                        
                        // Handle timestamp with null check
                        Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                        order.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                        
                        // Handle items
                        DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                        if (itemsSnapshot.exists()) {
                            List<Map<String, Object>> items = new ArrayList<>();
                            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                items.add((Map<String, Object>) itemSnapshot.getValue());
                            }
                            order.setItems(items);
                        }
                        
                        driverOrders.add(order);
                    }
                }

                // Update UI
                if (driverOrders.isEmpty()) {
                    noOrdersText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noOrdersText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setOrders(driverOrders);
                }
                
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load orders: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        };

        ordersRef.addValueEventListener(ordersListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        if (earningsListener != null) {
            ordersRef.removeEventListener(earningsListener);
        }
    }
} 