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

import androidx.annotation.NonNull;
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
import java.util.Collections;
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

        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        ordersRef.addValueEventListener(new ValueEventListener() {
                @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ordersAdapter.clearOrders();
                Log.d(TAG, "Loading orders...");
                        List<Order> orders = new ArrayList<>();
                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderId = orderSnapshot.getKey();
                    String status = orderSnapshot.child("status").getValue(String.class);
                    String orderStatus = orderSnapshot.child("order_status").getValue(String.class);
                                String restaurantId = orderSnapshot.child("restaurantId").getValue(String.class);
                    String customerId = orderSnapshot.child("customerId").getValue(String.class);
                    String customerName = orderSnapshot.child("customerName").getValue(String.class);
                    String deliveryOption = orderSnapshot.child("deliveryOption").getValue(String.class);

                    Log.d(TAG, "Processing order: " + orderId + 
                        " status: " + status + 
                        " order_status: " + orderStatus +
                        " restaurantId: " + restaurantId);

                    // Skip if restaurantId is null or doesn't match
                    if (restaurantId == null || !restaurantId.equals(userId)) {
                        Log.d(TAG, "Skipping order - restaurant ID mismatch or null");
                                    continue;
                                }

                    // Filter orders based on status field
                    if (currentOrderStatus.equals("pending")) {
                        // For pending tab, show orders with status "pending"
                        if (!status.equals("pending")) {
                            Log.d(TAG, "Skipping order - not pending");
                                    continue;
                                }
                    } else if (currentOrderStatus.equals("in_progress")) {
                        // For in progress tab, show orders with status "in_progress"
                        if (!status.equals("in_progress")) {
                            Log.d(TAG, "Skipping order - not in progress");
                                    continue;
                        }
                    } else if (currentOrderStatus.equals("delivered")) {
                        // For delivered tab, show orders with status "delivered"
                        if (!status.equals("delivered")) {
                            Log.d(TAG, "Skipping order - not delivered");
                                    continue;
                        }
                                }

                                Order order = new Order();
                    order.setId(orderId);
                    order.setStatus(status);
                    order.setOrderStatus(orderStatus);
                                order.setRestaurantId(restaurantId);
                    order.setCustomerId(customerId);
                    order.setCustomerName(customerName);
                    order.setDeliveryOption(deliveryOption);

                    // Fetch customer phone from customers node only if customerId is not null
                    if (customerId != null) {
                        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                            .getReference("customers")
                            .child(customerId);
                        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot customerSnapshot) {
                                if (customerSnapshot.exists()) {
                                    String phone = customerSnapshot.child("phone").getValue(String.class);
                                    if (phone != null) {
                                        order.setCustomerPhone(phone);
                                        Log.d(TAG, "Customer phone fetched: " + phone);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error fetching customer phone: " + error.getMessage());
                            }
                        });
                    } else {
                        Log.d(TAG, "Skipping phone fetch - customerId is null");
                    }

                    // Handle createdAt timestamp
                    Object createdAtObj = orderSnapshot.child("createdAt").getValue();
                    if (createdAtObj != null) {
                        if (createdAtObj instanceof Long) {
                            order.setTimestamp((Long) createdAtObj);
                        } else if (createdAtObj instanceof Double) {
                            order.setTimestamp(((Double) createdAtObj).longValue());
                        }
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

                    // Load order items
                    List<OrderItem> items = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : orderSnapshot.child("items").getChildren()) {
                        OrderItem item = new OrderItem();
                        item.setName(itemSnapshot.child("name").getValue(String.class));
                        item.setQuantity(itemSnapshot.child("quantity").getValue(Integer.class));
                        
                        Object itemPriceObj = itemSnapshot.child("price").getValue();
                        if (itemPriceObj != null) {
                            if (itemPriceObj instanceof Long) {
                                item.setPrice(((Long) itemPriceObj).doubleValue());
                            } else if (itemPriceObj instanceof Double) {
                                item.setPrice((Double) itemPriceObj);
                            }
                        }

                        // Load customizations
                        List<OrderItem.CustomizationOption> customizationOptions = new ArrayList<>();
                        DataSnapshot customizationsSnapshot = itemSnapshot.child("customizations");
                        Log.d(TAG, "Loading customizations for item: " + item.getName());
                        if (customizationsSnapshot.exists()) {
                            Log.d(TAG, "Customizations snapshot exists");
                            // First iterate through customizationId level
                            for (DataSnapshot customizationIdSnapshot : customizationsSnapshot.getChildren()) {
                                String customizationId = customizationIdSnapshot.getKey();
                                Log.d(TAG, "Processing customization ID: " + customizationId);

                                // Then iterate through numeric indices (0, 1, 2, etc.)
                                for (DataSnapshot indexSnapshot : customizationIdSnapshot.getChildren()) {
                                    OrderItem.CustomizationOption option = new OrderItem.CustomizationOption();
                                    
                                    // Extract fields directly from the indexed snapshot
                                    String optionId = indexSnapshot.child("optionId").getValue(String.class);
                                    String optionName = indexSnapshot.child("optionName").getValue(String.class);
                                    Log.d(TAG, "Processing option at index " + indexSnapshot.getKey() + 
                                        ": " + optionName + " (ID: " + optionId + ")");
                                    
                                    option.setOptionId(optionId);
                                    option.setOptionName(optionName);

                                    List<OrderItem.SelectedItem> selectedItems = new ArrayList<>();
                                    DataSnapshot selectedItemsSnapshot = indexSnapshot.child("selectedItems");
                                    if (selectedItemsSnapshot.exists()) {
                                        Log.d(TAG, "Selected items exist for option: " + optionName);
                                        // Iterate through selected items array
                                        for (DataSnapshot selectedItemSnapshot : selectedItemsSnapshot.getChildren()) {
                                            OrderItem.SelectedItem selectedItem = new OrderItem.SelectedItem();
                                            String itemId = selectedItemSnapshot.child("id").getValue(String.class);
                                            String itemName = selectedItemSnapshot.child("name").getValue(String.class);
                                            Log.d(TAG, "Processing selected item: " + itemName + " (ID: " + itemId + ")");
                                            
                                            selectedItem.setId(itemId);
                                            selectedItem.setName(itemName);
                                            
                                            Object priceObj = selectedItemSnapshot.child("price").getValue();
                                            if (priceObj != null) {
                                                if (priceObj instanceof Long) {
                                                    selectedItem.setPrice(((Long) priceObj).doubleValue());
                                                } else if (priceObj instanceof Double) {
                                                    selectedItem.setPrice((Double) priceObj);
                                                }
                                                Log.d(TAG, "Selected item price: " + selectedItem.getPrice());
                                            }
                                            selectedItems.add(selectedItem);
                                        }
                                    } else {
                                        Log.d(TAG, "No selected items found for option: " + optionName);
                                    }
                                    option.setSelectedItems(selectedItems);
                                    customizationOptions.add(option);
                                }
                            }
                        } else {
                            Log.d(TAG, "No customizations found for item: " + item.getName());
                        }
                        item.setCustomizations(customizationOptions);
                        items.add(item);
                    }
                    order.setItems(items);
                    orders.add(order);
                    Log.d(TAG, "Added order to list: " + orderId);
                }

                // Sort orders by timestamp if needed
                Collections.sort(orders, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                
                // Update the adapter
                ordersAdapter.setOrders(orders);
                updateEmptyState(orders.isEmpty());
                Log.d(TAG, "Total orders loaded: " + orders.size());
                }

                @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading orders: " + error.getMessage());
                    if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading orders: " + error.getMessage(),
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
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
            .getReference("drivers")
            .child(driverId);

        // Update order fields
        Map<String, Object> orderUpdates = new HashMap<>();
        orderUpdates.put("driverId", driverId);
        orderUpdates.put("driverName", driverName);
        orderUpdates.put("status", "in_progress");
        orderUpdates.put("order_status", "assigned_driver");

        // First update the order
        orderRef.updateChildren(orderUpdates)
            .addOnSuccessListener(aVoid -> {
                // Then update driver availability
                driverRef.child("isAvailable").setValue(false)
                    .addOnSuccessListener(aVoid2 -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), 
                                "Order assigned to " + driverName, 
                                Toast.LENGTH_SHORT).show();
                        }
                        Log.d("OrdersFragment", "Successfully assigned order to driver and updated availability");
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), 
                                "Warning: Driver assigned but availability update failed", 
                                Toast.LENGTH_SHORT).show();
                        }
                        Log.e("OrdersFragment", "Failed to update driver availability", e);
                    });
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), 
                        "Failed to assign driver: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
                Log.e("OrdersFragment", "Failed to assign driver", e);
            });
    }
} 