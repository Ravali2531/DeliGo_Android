package com.example.deligoandroid.Driver.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import android.util.Log;

public class DriverHomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private DriverOrdersAdapter adapter;
    private TextView noOrdersText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView earningsText;
    private TextView deliveriesText;
    private TextView rejectedOrdersText;
    private Switch availabilitySwitch;
    private String currentDriverId;
    private DatabaseReference ordersRef;
    private DatabaseReference driversRef;
    private ValueEventListener ordersListener;
    private ValueEventListener earningsListener;
    private ValueEventListener availabilityListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.ordersRecyclerView);
        noOrdersText = view.findViewById(R.id.noOrdersText);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        earningsText = view.findViewById(R.id.earningsText);
        deliveriesText = view.findViewById(R.id.deliveriesText);
        rejectedOrdersText = view.findViewById(R.id.rejectedOrdersText);
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
        loadRejectedOrders();

        return view;
    }

    private void setupAvailabilitySwitch() {
        // Disable switch until we get initial state
        availabilitySwitch.setEnabled(false);

        // Remove any existing listener
        if (availabilityListener != null) {
            driversRef.child("isAvailable").removeEventListener(availabilityListener);
        }

        // Create and add the continuous listener
        availabilityListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (getContext() == null || !isAdded()) return;  // Fragment not attached
                
                Boolean isAvailable = dataSnapshot.getValue(Boolean.class);
                availabilitySwitch.setChecked(isAvailable != null ? isAvailable : false);
                availabilitySwitch.setEnabled(true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (getContext() == null || !isAdded()) return;  // Fragment not attached
                
                availabilitySwitch.setEnabled(true);
                Toast.makeText(getContext(), "Failed to load availability status: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                
                Log.e("DriverHomeFragment", "Failed to load availability status", databaseError.toException());
            }
        };

        // Add the continuous listener
        driversRef.child("isAvailable").addValueEventListener(availabilityListener);

        // Setup switch click listener
        availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getContext() == null) return;  // Fragment not attached
            
            // Disable switch while updating
            availabilitySwitch.setEnabled(false);
            
            driversRef.child("isAvailable").setValue(isChecked)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() == null) return;  // Fragment not attached
                    
                    availabilitySwitch.setEnabled(true);
                    String message = isChecked ? "You are now available for deliveries" : "You are now offline";
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;  // Fragment not attached
                    
                    // Revert switch state if update fails
                    availabilitySwitch.setEnabled(true);
                    availabilitySwitch.setChecked(!isChecked);
                    Toast.makeText(getContext(), "Failed to update availability: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    
                    Log.e("DriverHomeFragment", "Failed to update availability", e);
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
                
                // Get today's start timestamp (midnight)
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                long todayStart = calendar.getTimeInMillis();
                
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String driverId = orderSnapshot.child("driverId").getValue(String.class);
                    String status = orderSnapshot.child("status").getValue(String.class);
                    Long updatedAt = orderSnapshot.child("updatedAt").getValue(Long.class);
                    
                    // Only count orders delivered today
                    if (currentDriverId.equals(driverId) && 
                        "delivered".equals(status) && 
                        updatedAt != null && 
                        updatedAt >= todayStart) {
                        
                        // Get delivery fee and tip amount
                        Double deliveryFee = orderSnapshot.child("deliveryFee").getValue(Double.class);
                        Double tipAmount = orderSnapshot.child("tipAmount").getValue(Double.class);
                        
                        // Add to total earnings
                        if (deliveryFee != null) totalEarnings += deliveryFee;
                        if (tipAmount != null) totalEarnings += tipAmount;
                        
                        deliveryCount++;
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
                    String status = orderSnapshot.child("status").getValue(String.class);
                    
                    // Only show orders that are assigned to this driver and are not delivered/cancelled
                    if (driverId != null && driverId.equals(currentDriverId) && 
                        status != null && !status.equals("delivered") && !status.equals("cancelled")) {
                        Order order = new Order();
                        
                        // Map the data from Firebase to our Order model
                        order.setOrderId(orderSnapshot.getKey());
                        order.setCustomerId(orderSnapshot.child("customerId").getValue(String.class));
                        order.setCustomerName(orderSnapshot.child("customerName").getValue(String.class));
                        order.setRestaurantId(orderSnapshot.child("restaurantId").getValue(String.class));
                        
                        // Fetch restaurant name if not present
                        String restaurantName = orderSnapshot.child("restaurantName").getValue(String.class);
                        if (restaurantName == null || restaurantName.isEmpty()) {
                            String restaurantId = order.getRestaurantId();
                            if (restaurantId != null) {
                                DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                                    .getReference("restaurants")
                                    .child(restaurantId);
                                restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot restaurantSnapshot) {
                                        if (restaurantSnapshot.exists()) {
                                            String name = restaurantSnapshot.child("name").getValue(String.class);
                                            if (name != null) {
                                                order.setRestaurantName(name);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("DriverHomeFragment", "Error fetching restaurant name: " + error.getMessage());
                                    }
                                });
                            }
                        } else {
                            order.setRestaurantName(restaurantName);
                        }
                        
                        // Handle total amount
                        Object totalAmountObj = orderSnapshot.child("totalAmount").getValue();
                        if (totalAmountObj != null) {
                            if (totalAmountObj instanceof Long) {
                                order.setTotalAmount(((Long) totalAmountObj).doubleValue());
                            } else if (totalAmountObj instanceof Double) {
                                order.setTotalAmount((Double) totalAmountObj);
                            }
                        }
                        
                        order.setStatus(status);
                        order.setOrderStatus(orderSnapshot.child("order_status").getValue(String.class));
                        order.setDriverId(driverId);
                        order.setDriverName(orderSnapshot.child("driverName").getValue(String.class));
                        
                        Boolean driverAccepted = orderSnapshot.child("driverAccepted").getValue(Boolean.class);
                        order.setDriverAccepted(driverAccepted != null ? driverAccepted : false);
                        
                        order.setDeliveryOption(orderSnapshot.child("deliveryOption").getValue(String.class));
                        order.setDeliveryAddress(orderSnapshot.child("deliveryAddress").getValue(String.class));
                        
                        Double lat = orderSnapshot.child("deliveryLatitude").getValue(Double.class);
                        Double lng = orderSnapshot.child("deliveryLongitude").getValue(Double.class);
                        order.setDeliveryLatitude(lat != null ? lat : 0.0);
                        order.setDeliveryLongitude(lng != null ? lng : 0.0);
                        
                        Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                        order.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                        
                        // Handle items with customizations
                        DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                        if (itemsSnapshot.exists()) {
                            List<Map<String, Object>> items = new ArrayList<>();
                            for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                Map<String, Object> item = new HashMap<>();
                                item.put("name", itemSnapshot.child("name").getValue(String.class));
                                item.put("quantity", itemSnapshot.child("quantity").getValue(Integer.class));
                                item.put("price", itemSnapshot.child("price").getValue(Double.class));
                                
                                // Handle customizations
                                DataSnapshot customizationsSnapshot = itemSnapshot.child("customizations");
                                if (customizationsSnapshot.exists()) {
                                    Map<String, Object> customizations = new HashMap<>();
                                    for (DataSnapshot customizationIdSnapshot : customizationsSnapshot.getChildren()) {
                                        String customizationId = customizationIdSnapshot.getKey();
                                        List<Map<String, Object>> options = new ArrayList<>();
                                        
                                        for (DataSnapshot indexSnapshot : customizationIdSnapshot.getChildren()) {
                                            Map<String, Object> option = new HashMap<>();
                                            option.put("optionId", indexSnapshot.child("optionId").getValue(String.class));
                                            option.put("optionName", indexSnapshot.child("optionName").getValue(String.class));
                                            
                                            List<Map<String, Object>> selectedItems = new ArrayList<>();
                                            DataSnapshot selectedItemsSnapshot = indexSnapshot.child("selectedItems");
                                            if (selectedItemsSnapshot.exists()) {
                                                for (DataSnapshot selectedItemSnapshot : selectedItemsSnapshot.getChildren()) {
                                                    Map<String, Object> selectedItem = new HashMap<>();
                                                    selectedItem.put("id", selectedItemSnapshot.child("id").getValue(String.class));
                                                    selectedItem.put("name", selectedItemSnapshot.child("name").getValue(String.class));
                                                    selectedItem.put("price", selectedItemSnapshot.child("price").getValue(Double.class));
                                                    selectedItems.add(selectedItem);
                                                }
                                            }
                                            option.put("selectedItems", selectedItems);
                                            options.add(option);
                                        }
                                        customizations.put(customizationId, options);
                                    }
                                    item.put("customizations", customizations);
                                }
                                items.add(item);
                            }
                            order.setItems(items);
                        }
                        
                        driverOrders.add(order);
                    }
                }

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

    private void loadRejectedOrders() {
        driversRef.child("rejectedOrdersCount")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Integer rejectedCount = dataSnapshot.getValue(Integer.class);
                    if (rejectedOrdersText != null) {
                        rejectedOrdersText.setText((rejectedCount != null ? rejectedCount : 0) + " Rejected Orders");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("DriverHomeFragment", "Error loading rejected orders count", databaseError.toException());
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove all listeners
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
        if (earningsListener != null) {
            ordersRef.removeEventListener(earningsListener);
        }
        if (availabilityListener != null) {
            driversRef.child("isAvailable").removeEventListener(availabilityListener);
        }
    }
} 