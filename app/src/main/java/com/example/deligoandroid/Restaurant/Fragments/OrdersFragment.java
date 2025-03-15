package com.example.deligoandroid.Restaurant.Fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Adapters.OrdersAdapter;
import com.example.deligoandroid.Restaurant.Models.Order;
import com.example.deligoandroid.Restaurant.Models.OrderItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {
    private RecyclerView ordersRecyclerView;
    private LinearLayout noNewOrdersLayout, noInProgressOrdersLayout, noDeliveredOrdersLayout;
    private Button newOrdersTab, inProgressTab, deliveredTab;
    private OrdersAdapter ordersAdapter;
    private DatabaseReference databaseRef;
    private String userId;
    private String currentOrderStatus = "pending";
    private static final String TAG = "OrdersFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        
        initializeViews(view);
        setupClickListeners();
        loadOrders();
        
        return view;
    }

    private void initializeViews(View view) {
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
        noNewOrdersLayout = view.findViewById(R.id.noNewOrdersLayout);
        noInProgressOrdersLayout = view.findViewById(R.id.noInProgressOrdersLayout);
        noDeliveredOrdersLayout = view.findViewById(R.id.noDeliveredOrdersLayout);
        newOrdersTab = view.findViewById(R.id.newOrdersTab);
        inProgressTab = view.findViewById(R.id.inProgressTab);
        deliveredTab = view.findViewById(R.id.deliveredTab);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersAdapter = new OrdersAdapter(userId);
        ordersAdapter.setOnAssignDriverClickListener(this::showDriverAssignmentDialog);
        ordersRecyclerView.setAdapter(ordersAdapter);

        // Set initial tab colors
        updateTabColors("new");
    }

    private void setupClickListeners() {
        newOrdersTab.setOnClickListener(v -> {
            currentOrderStatus = "pending";
            updateTabColors("new");
            loadOrders();
        });
        
        inProgressTab.setOnClickListener(v -> {
            currentOrderStatus = "in_progress";
            updateTabColors("in_progress");
            loadOrders();
        });
        
        deliveredTab.setOnClickListener(v -> {
            currentOrderStatus = "delivered";
            updateTabColors("delivered");
            loadOrders();
        });
    }

    private void updateTabColors(String activeTab) {
        int activeColor = getResources().getColor(R.color.accent_orange);
        int inactiveColor = getResources().getColor(android.R.color.darker_gray);

        newOrdersTab.setBackgroundTintList(ColorStateList.valueOf(
            activeTab.equals("new") ? activeColor : inactiveColor));
        inProgressTab.setBackgroundTintList(ColorStateList.valueOf(
            activeTab.equals("in_progress") ? activeColor : inactiveColor));
        deliveredTab.setBackgroundTintList(ColorStateList.valueOf(
            activeTab.equals("delivered") ? activeColor : inactiveColor));
    }

    private void updateEmptyState(boolean isEmpty) {
        if (getContext() == null) return;
        
        ordersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        noNewOrdersLayout.setVisibility(
            currentOrderStatus.equals("new") && isEmpty ? View.VISIBLE : View.GONE);
        noInProgressOrdersLayout.setVisibility(
            currentOrderStatus.equals("in_progress") && isEmpty ? View.VISIBLE : View.GONE);
        noDeliveredOrdersLayout.setVisibility(
            currentOrderStatus.equals("delivered") && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void loadOrders() {
        if (userId == null) {
            Log.e(TAG, "User ID is null");
            return;
        }

        Log.d(TAG, "Loading orders for restaurant ID: " + userId + ", status filter: " + currentOrderStatus);

        databaseRef.child("orders")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    try {
                        Log.d(TAG, "Orders snapshot received. Total orders: " + dataSnapshot.getChildrenCount());
                        List<Order> orders = new ArrayList<>();

                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            try {
                                String restaurantId = orderSnapshot.child("restaurantId").getValue(String.class);
                                String status = orderSnapshot.child("status").getValue(String.class);

                                if (restaurantId == null || status == null) {
                                    Log.w(TAG, "Skipping order with null restaurantId or status");
                                    continue;
                                }

                                if (!restaurantId.equals(userId)) {
                                    continue;
                                }

                                if (currentOrderStatus.equals("pending") && !status.equals("pending") && !status.equals("new")) {
                                    continue;
                                } else if (!currentOrderStatus.equals("pending") && !status.equalsIgnoreCase(currentOrderStatus)) {
                                    continue;
                                }

                                Order order = new Order();
                                order.setId(orderSnapshot.getKey());
                                order.setRestaurantId(restaurantId);
                                order.setStatus(status);
                                order.setOrderStatus(orderSnapshot.child("order_status").getValue(String.class));
                                order.setTimestamp(orderSnapshot.child("timestamp").getValue(Long.class));
                                order.setTotalAmount(orderSnapshot.child("total").getValue(Double.class));
                                order.setDeliveryOption(orderSnapshot.child("deliveryOption").getValue(String.class));

                                // Get items
                                DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                                if (itemsSnapshot.exists()) {
                                    List<OrderItem> items = new ArrayList<>();
                                    for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                        OrderItem item = new OrderItem();
                                        item.setName(itemSnapshot.child("name").getValue(String.class));
                                        item.setQuantity(itemSnapshot.child("quantity").getValue(Integer.class));
                                        item.setPrice(itemSnapshot.child("price").getValue(Double.class));
                                        items.add(item);
                                    }
                                    order.setItems(items);
                                }

                                orders.add(order);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing order: " + e.getMessage());
                            }
                        }

                        ordersAdapter.setOrders(orders);
                        updateEmptyState(orders.isEmpty());

                    } catch (Exception e) {
                        Log.e(TAG, "Error in onDataChange: " + e.getMessage(), e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading orders", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error loading orders: " + databaseError.getMessage());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading orders: " + databaseError.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void fetchCustomerName(Order order, String customerId) {
        databaseRef.child("customers").child(customerId).child("fullName")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String customerName = dataSnapshot.getValue(String.class);
                    if (customerName != null) {
                        order.setCustomerName(customerName);
                        ordersAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error fetching customer name: " + databaseError.getMessage());
                }
            });
    }

    private void showDriverAssignmentDialog(Order order) {
        if (getContext() == null) return;

        // Query for available drivers
        DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference("drivers");
        driversRef.orderByChild("isAvailable").equalTo(true)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        Toast.makeText(getContext(), "No available drivers found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create list of driver names and IDs
                    List<String> driverNames = new ArrayList<>();
                    List<String> driverIds = new ArrayList<>();
                    
                    for (DataSnapshot driverSnapshot : dataSnapshot.getChildren()) {
                        String driverId = driverSnapshot.getKey();
                        String driverName = driverSnapshot.child("fullName").getValue(String.class);
                        if (driverName != null) {
                            driverNames.add(driverName);
                            driverIds.add(driverId);
                        }
                    }

                    if (driverNames.isEmpty()) {
                        Toast.makeText(getContext(), "No available drivers found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create and show dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Select Driver");
                    
                    String[] driverNamesArray = driverNames.toArray(new String[0]);
                    builder.setItems(driverNamesArray, (dialog, which) -> {
                        String selectedDriverId = driverIds.get(which);
                        String selectedDriverName = driverNames.get(which);
                        assignDriverToOrder(order, selectedDriverId, selectedDriverName);
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), 
                        "Error loading drivers: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void assignDriverToOrder(Order order, String driverId, String driverName) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance()
            .getReference("orders")
            .child(order.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("driverId", driverId);
        updates.put("driverName", driverName);
        updates.put("order_status", "assigned_driver");

        orderRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), 
                    "Order assigned to " + driverName, 
                    Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), 
                    "Failed to assign driver: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }
} 