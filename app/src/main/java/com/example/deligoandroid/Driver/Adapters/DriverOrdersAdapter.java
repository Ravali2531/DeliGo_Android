package com.example.deligoandroid.Driver.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.Models.OrderItem;
import com.example.deligoandroid.Driver.Adapters.OrderItemsAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;

public class DriverOrdersAdapter extends RecyclerView.Adapter<DriverOrdersAdapter.ViewHolder> {
    private List<Order> orders = new ArrayList<>();
    private Context context;
    private String driverId;

    public DriverOrdersAdapter(String driverId) {
        this.driverId = driverId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_driver_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Order order = orders.get(position);
        if (order == null) return;

        // Set order number and timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String orderTime = sdf.format(new Date(order.getTimestamp()));
        String orderId = order.getOrderId(); // Try getOrderId() first
        if (orderId == null || orderId.isEmpty()) {
            orderId = order.getId(); // Fallback to getId()
        }
        if (orderId == null || orderId.isEmpty()) {
            orderId = "Unknown";
        }
        holder.orderNumber.setText("Order #" + orderId + " • " + orderTime);

        // Set status with appropriate color
        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        String orderStatus = order.getOrderStatus() != null ? order.getOrderStatus().toLowerCase() : "";
        
        // Set the displayed status text and background color
        String displayStatus = !orderStatus.isEmpty() ? orderStatus : status;
        if (!displayStatus.isEmpty()) {
            holder.orderStatus.setVisibility(View.VISIBLE);
            holder.orderStatus.setText(displayStatus.substring(0, 1).toUpperCase() + displayStatus.substring(1).replace("_", " "));
            
            // Set status background color
            int backgroundColor;
            if (status.equals("delivered")) {
                backgroundColor = R.color.green;
            } else if (orderStatus.equals("assigned_driver")) {
                backgroundColor = R.color.purple;
            } else {
                backgroundColor = R.color.blue;
            }
            holder.orderStatus.getBackground().setTint(context.getResources().getColor(backgroundColor, null));
        } else {
            holder.orderStatus.setVisibility(View.GONE);
        }

        // Hide all buttons initially
        holder.acceptButton.setVisibility(View.GONE);
        holder.rejectButton.setVisibility(View.GONE);
        holder.markPickedUpButton.setVisibility(View.GONE);
        holder.markDeliveredButton.setVisibility(View.GONE);

        // Show appropriate buttons based on status
        if (status.equals("in_progress")) {
            if (orderStatus.equals("assigned_driver") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setVisibility(View.VISIBLE);
            } else if (orderStatus.equals("driver_accepted") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.markPickedUpButton.setVisibility(View.VISIBLE);
            } else if (orderStatus.equals("picked_up") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.markDeliveredButton.setVisibility(View.VISIBLE);
            }
        }

        // Load restaurant and customer names from Firebase
        if (order.getRestaurantId() != null) {
            loadRestaurantName(order.getRestaurantId(), holder.restaurantName);
        } else {
            holder.restaurantName.setText("From: Unknown Restaurant");
        }

        if (order.getCustomerId() != null) {
            loadCustomerName(order.getCustomerId(), holder.customerName);
        } else {
            holder.customerName.setText("To: Unknown Customer");
        }

        // Calculate total amount including items and delivery fee
        double totalAmount = 0.0;
        List<Map<String, Object>> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
            List<OrderItem> orderItems = new ArrayList<>();
            
            for (Map<String, Object> itemMap : items) {
                try {
                    OrderItem orderItem = new OrderItem();
                    String name = (String) itemMap.get("name");
                    if (name == null) continue; // Skip items without a name
                    orderItem.setName(name);
                    
                    Object quantityObj = itemMap.get("quantity");
                    orderItem.setQuantity(quantityObj instanceof Number ? ((Number) quantityObj).intValue() : 1);
                    
                    Object priceObj = itemMap.get("price");
                    double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0;
                    orderItem.setPrice(price);
                    
                    // Calculate item total
                    double itemTotal = price * orderItem.getQuantity();
                    totalAmount += itemTotal;
                    
                    // Handle customizations
                    Object customizationsObj = itemMap.get("customizations");
                    if (customizationsObj instanceof Map) {
                        Map<String, Object> customizationsMap = (Map<String, Object>) customizationsObj;
                        List<OrderItem.CustomizationOption> customizationOptions = new ArrayList<>();
                        
                        Log.d("DriverOrdersAdapter", "Found customizations map with " + customizationsMap.size() + " entries");
                        
                        for (Map.Entry<String, Object> entry : customizationsMap.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                Map<String, Object> optionData = (Map<String, Object>) entry.getValue();
                                
                                OrderItem.CustomizationOption option = new OrderItem.CustomizationOption();
                                option.setOptionId(entry.getKey());
                                
                                // Get the selected items from the option data
                                Object selectedItemsObj = optionData.get("selectedItems");
                                if (selectedItemsObj instanceof List) {
                                    List<Map<String, Object>> selectedItemsList = (List<Map<String, Object>>) selectedItemsObj;
                                    List<OrderItem.SelectedItem> selectedItems = new ArrayList<>();
                                    
                                    for (Map<String, Object> selectedItemMap : selectedItemsList) {
                                        OrderItem.SelectedItem selectedItem = new OrderItem.SelectedItem();
                                        selectedItem.setId((String) selectedItemMap.get("id"));
                                        selectedItem.setName((String) selectedItemMap.get("name"));
                                        
                                        Object customizationPriceObj = selectedItemMap.get("price");
                                        double itemPrice = customizationPriceObj instanceof Number ? ((Number) customizationPriceObj).doubleValue() : 0.0;
                                        selectedItem.setPrice(itemPrice);
                                        
                                        selectedItems.add(selectedItem);
                                        Log.d("DriverOrdersAdapter", "Added selected item: " + selectedItem.getName());
                                    }
                                    
                                    option.setSelectedItems(selectedItems);
                                    if (!selectedItems.isEmpty()) {
                                        customizationOptions.add(option);
                                    }
                                }
                            }
                        }
                        
                        if (!customizationOptions.isEmpty()) {
                            orderItem.setCustomizations(customizationOptions);
                            Log.d("DriverOrdersAdapter", "Set " + customizationOptions.size() + " customization options on item");
                        }
                    } else {
                        Log.d("DriverOrdersAdapter", "No customizations found or invalid format");
                    }
                    
                    orderItems.add(orderItem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            if (!orderItems.isEmpty()) {
                OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(orderItems);
                holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
                holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                holder.orderItemsRecyclerView.setVisibility(View.GONE);
            }
        } else {
            holder.orderItemsRecyclerView.setVisibility(View.GONE);
        }

        // Add delivery fee to total if present
        Double deliveryFee = order.getDeliveryFee();
        if (deliveryFee != null && deliveryFee > 0) {
            totalAmount += deliveryFee;
            holder.deliveryFee.setVisibility(View.VISIBLE);
            holder.deliveryFee.setText(NumberFormat.getCurrencyInstance(Locale.US).format(deliveryFee));
        } else {
            holder.deliveryFee.setVisibility(View.GONE);
        }
        
        // Set total amount
        holder.totalAmount.setText(NumberFormat.getCurrencyInstance(Locale.US).format(totalAmount));

        // Setup button click listeners
        holder.acceptButton.setOnClickListener(v -> acceptOrder(order.getId()));
        holder.rejectButton.setOnClickListener(v -> rejectOrder(order.getId()));
        holder.markPickedUpButton.setOnClickListener(v -> markOrderAsPickedUp(order.getId()));
        holder.markDeliveredButton.setOnClickListener(v -> markOrderAsDelivered(order.getId()));
    }

    private void acceptOrder(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("order_status", "driver_accepted");
        updates.put("driverAccepted", true);

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                        Toast.makeText(context, "Order accepted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Failed to accept order", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectOrder(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("order_status", "pending");
        updates.put("driverId", null);
        updates.put("driverName", null);
        updates.put("driverAccepted", false);

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                        Toast.makeText(context, "Order rejected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Failed to reject order", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markOrderAsPickedUp(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("order_status", "picked_up");

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                        Toast.makeText(context, "Order marked as picked up", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Failed to update order status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markOrderAsDelivered(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "delivered");
        updates.put("order_status", "delivered");

        orderRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                        Toast.makeText(context, "Order marked as delivered", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Failed to update order status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRestaurantName(String restaurantId, TextView restaurantNameView) {
        if (restaurantId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("restaurants")
                    .child(restaurantId)
                    .child("store_info")
                    .child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = dataSnapshot.getValue(String.class);
                            restaurantNameView.setText("From: " + (name != null ? name : "Unknown Restaurant"));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            restaurantNameView.setText("From: Unknown Restaurant");
                        }
                    });
        } else {
            restaurantNameView.setText("From: Unknown Restaurant");
        }
    }

    private void loadCustomerName(String customerId, TextView customerNameView) {
        if (customerId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("customers")
                    .child(customerId)
                    .child("name")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = dataSnapshot.getValue(String.class);
                            customerNameView.setText("To: " + (name != null ? name : "Unknown Customer"));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            customerNameView.setText("To: Unknown Customer");
                        }
                    });
        } else {
            customerNameView.setText("To: Unknown Customer");
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, restaurantName, customerName, totalAmount, deliveryFee;
        Button acceptButton, rejectButton, markDeliveredButton, markPickedUpButton;
        RecyclerView orderItemsRecyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            customerName = itemView.findViewById(R.id.customerName);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            markDeliveredButton = itemView.findViewById(R.id.markDeliveredButton);
            markPickedUpButton = itemView.findViewById(R.id.markPickedUpButton);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
        }
    }
} 