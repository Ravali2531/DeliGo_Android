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
import java.util.List;

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
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    private void setupClickListeners() {
        newOrdersTab.setOnClickListener(v -> showNewOrders());
        inProgressTab.setOnClickListener(v -> showInProgressOrders());
        deliveredTab.setOnClickListener(v -> showDeliveredOrders());
    }

    private void showNewOrders() {
        currentOrderStatus = "pending";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void showInProgressOrders() {
        currentOrderStatus = "in_progress";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void showDeliveredOrders() {
        currentOrderStatus = "delivered";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void updateOrderStatusTabs() {
        newOrdersTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        inProgressTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        deliveredTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));

        switch (currentOrderStatus.toLowerCase()) {
            case "pending":
                newOrdersTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
            case "in_progress":
                inProgressTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
            case "delivered":
                deliveredTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (getContext() == null) return;
        
        ordersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        // Update the visibility of empty state layouts based on current tab
        noNewOrdersLayout.setVisibility(currentOrderStatus.equalsIgnoreCase("pending") && isEmpty ? View.VISIBLE : View.GONE);
        noInProgressOrdersLayout.setVisibility(currentOrderStatus.equalsIgnoreCase("in_progress") && isEmpty ? View.VISIBLE : View.GONE);
        noDeliveredOrdersLayout.setVisibility(currentOrderStatus.equalsIgnoreCase("delivered") && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void loadOrders() {
        if (userId == null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        Log.d(TAG, "Loading orders for restaurant ID: " + userId + ", status filter: " + currentOrderStatus);

        // Get all orders and filter by restaurantId
        databaseRef.child("orders")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Orders snapshot received. Total orders in Firebase: " + dataSnapshot.getChildrenCount());
                    
                    List<Order> orders = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        String orderId = orderSnapshot.getKey();
                        Log.d(TAG, "Processing order ID: " + orderId);
                        
                        // Get restaurantId for this order
                        String restaurantId = orderSnapshot.child("restaurantId").getValue(String.class);
                        Log.d(TAG, "Order " + orderId + " - Restaurant ID: " + restaurantId + 
                              " (Current restaurant: " + userId + ")");
                        
                        // Check if this order belongs to the current restaurant
                        if (restaurantId != null && restaurantId.equals(userId)) {
                            String status = orderSnapshot.child("status").getValue(String.class);
                            Log.d(TAG, "Order " + orderId + " - Status: " + status + 
                                  " (Current filter: " + currentOrderStatus + ")");
                            
                            // Only process orders with matching status (case-insensitive)
                            if (status != null && status.equalsIgnoreCase(currentOrderStatus)) {
                                try {
                                    Order order = new Order();
                                    order.setId(orderId);
                                    order.setRestaurantId(restaurantId);
                                    order.setStatus(status);
                                    
                                    // Get customer details
                                    String customerId = orderSnapshot.child("userId").getValue(String.class);
                                    order.setCustomerId(customerId);
                                    
                                    // Fetch customer name from customers node
                                    if (customerId != null) {
                                        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference()
                                            .child("customers")
                                            .child(customerId)
                                            .child("fullName");
                                            
                                        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot customerSnapshot) {
                                                String customerName = customerSnapshot.getValue(String.class);
                                                if (customerName != null) {
                                                    order.setCustomerName(customerName);
                                                    Log.d(TAG, "Order " + orderId + " - Customer name fetched: " + customerName);
                                                    ordersAdapter.notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.e(TAG, "Error fetching customer name: " + databaseError.getMessage());
                                            }
                                        });
                                    }
                                    
                                    String deliveryAddress = orderSnapshot.child("deliveryAddress").getValue(String.class);
                                    Log.d(TAG, "Order " + orderId + " - Address: " + deliveryAddress);
                                    
                                    order.setDeliveryAddress(deliveryAddress);
                                    
                                    // Get order details
                                    Double totalAmount = orderSnapshot.child("total").getValue(Double.class);
                                    Double deliveryFee = orderSnapshot.child("deliveryFee").getValue(Double.class);
                                    String deliveryOption = orderSnapshot.child("deliveryOption").getValue(String.class);
                                    Log.d(TAG, "Order " + orderId + " - Total: " + totalAmount + 
                                          ", Delivery Fee: " + deliveryFee + 
                                          ", Option: " + deliveryOption);
                                    
                                    if (totalAmount != null) order.setTotalAmount(totalAmount);
                                    if (deliveryFee != null) order.setDeliveryFee(deliveryFee);
                                    order.setDeliveryOption(deliveryOption);
                                    
                                    // Get timestamp
                                    Long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
                                    if (timestamp != null) {
                                        order.setTimestamp(timestamp);
                                        Log.d(TAG, "Order " + orderId + " - Timestamp: " + timestamp);
                                    }
                                    
                                    // Get items
                                    List<OrderItem> items = new ArrayList<>();
                                    DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                                    Log.d(TAG, "Order " + orderId + " - Number of items: " + 
                                          itemsSnapshot.getChildrenCount());
                                    
                                    for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                        try {
                                            OrderItem item = new OrderItem();
                                            String itemName = itemSnapshot.child("name").getValue(String.class);
                                            String description = itemSnapshot.child("description").getValue(String.class);
                                            Double price = itemSnapshot.child("price").getValue(Double.class);
                                            Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                            
                                            Log.d(TAG, "Order " + orderId + " - Item: " + itemName + 
                                                  ", Qty: " + quantity + ", Price: " + price);
                                            
                                            item.setName(itemName);
                                            item.setDescription(description);
                                            if (price != null) item.setPrice(price);
                                            if (quantity != null) item.setQuantity(quantity);
                                            
                                            // Get customizations
                                            List<OrderItem.Customization> customizations = new ArrayList<>();
                                            DataSnapshot customizationsSnapshot = itemSnapshot.child("customizations");
                                            Log.d(TAG, "Order " + orderId + " - Item " + itemName + 
                                                  " - Number of customizations: " + 
                                                  customizationsSnapshot.getChildrenCount());
                                            
                                            for (DataSnapshot customizationSnapshot : customizationsSnapshot.getChildren()) {
                                                try {
                                                    OrderItem.Customization customization = new OrderItem.Customization();
                                                    String choice = customizationSnapshot.child("choice").getValue(String.class);
                                                    String name = customizationSnapshot.child("name").getValue(String.class);
                                                    Double customPrice = customizationSnapshot.child("price").getValue(Double.class);
                                                    
                                                    Log.d(TAG, "Order " + orderId + " - Item " + itemName + 
                                                          " - Customization: " + choice + 
                                                          " (" + name + "), Price: " + customPrice);
                                                    
                                                    customization.setChoice(choice);
                                                    customization.setName(name);
                                                    if (customPrice != null) customization.setPrice(customPrice);
                                                    customizations.add(customization);
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing customization for order " + 
                                                          orderId + ", item " + itemName, e);
                                                }
                                            }
                                            item.setCustomizations(customizations);
                                            items.add(item);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing item for order " + orderId, e);
                                        }
                                    }
                                    order.setItems(items);
                                    orders.add(order);
                                    Log.d(TAG, "Successfully added order " + orderId + " to list");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing order " + orderId, e);
                                }
                            }
                        }
                    }

                    Log.d(TAG, "Final filtered orders count: " + orders.size());
                    updateEmptyState(orders.isEmpty());
                    ordersAdapter.setOrders(orders);
                    ordersAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error loading orders: " + databaseError.getMessage());
                    Toast.makeText(getContext(),
                        "Error loading orders: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
} 