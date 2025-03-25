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
                        
                        order.setStatus(orderSnapshot.child("status").getValue(String.class));
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