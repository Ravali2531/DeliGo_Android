package com.example.deligoandroid.Driver.Adapters;

import static android.content.ContentValues.TAG;

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
import androidx.annotation.NonNull;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.app.Dialog;
import android.view.Window;
import android.widget.EditText;

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
        
        // Get the correct order ID, trying all possible sources
        String orderId = null;
        if (order.getId() != null && !order.getId().isEmpty()) {
            orderId = order.getId();
        } else if (order.getOrderId() != null && !order.getOrderId().isEmpty()) {
            orderId = order.getOrderId();
        }
        
        final String finalOrderId = orderId;
        holder.orderNumber.setText("Order #" + (orderId != null ? orderId : "Unknown") + " • " + orderTime);

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
        holder.chatButton.setVisibility(View.GONE);

        // Show appropriate buttons based on status
        if (status.equals("in_progress")) {
            if (orderStatus.equals("assigned_driver") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setVisibility(View.VISIBLE);
                holder.chatButton.setVisibility(View.GONE);
            } else if (orderStatus.equals("driver_accepted") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.markPickedUpButton.setVisibility(View.VISIBLE);
                holder.chatButton.setVisibility(View.GONE);
            } else if (orderStatus.equals("picked_up") && order.getDriverId() != null && order.getDriverId().equals(driverId)) {
                holder.markDeliveredButton.setVisibility(View.VISIBLE);
                holder.chatButton.setVisibility(View.VISIBLE);
                holder.chatButton.setOnClickListener(v -> showChatDialog(order));
            }
        }

        // Load restaurant and customer names from Firebase
        if (order.getRestaurantId() != null) {
            holder.restaurantName.setTag(holder); // Store ViewHolder reference for later use
            loadRestaurantName(order.getRestaurantId(), holder.restaurantName);
        } else {
            holder.restaurantName.setText("From: Unknown Restaurant");
        }

        if (order.getCustomerId() != null) {
            loadCustomerName(order.getCustomerId(), holder.customerName);
        } else {
            holder.customerName.setText("To: Unknown Customer");
        }

        // Load delivery address
        loadDeliveryAddress(finalOrderId, holder.deliveryAddress);

        // Load order items
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
                    
                    // Handle customizations
                    Object customizationsObj = itemMap.get("customizations");
                    if (customizationsObj instanceof Map) {
                        Map<String, Object> customizationsMap = (Map<String, Object>) customizationsObj;
                        List<OrderItem.CustomizationOption> customizationOptions = new ArrayList<>();
                        
                        for (Map.Entry<String, Object> entry : customizationsMap.entrySet()) {
                            String customId = entry.getKey();
                            Object customValue = entry.getValue();
                            
                            try {
                                if (customValue instanceof ArrayList) {
                                    ArrayList<Map<String, Object>> customizationList = (ArrayList<Map<String, Object>>) customValue;
                                    if (!customizationList.isEmpty()) {
                                        Map<String, Object> optionData = customizationList.get(0);
                                        
                                        OrderItem.CustomizationOption option = new OrderItem.CustomizationOption();
                                        option.setOptionId((String) optionData.get("optionId"));
                                        option.setOptionName((String) optionData.get("optionName"));
                                        
                                        Object selectedItemsObj = optionData.get("selectedItems");
                                        if (selectedItemsObj instanceof ArrayList) {
                                            ArrayList<Map<String, Object>> selectedItemsList = (ArrayList<Map<String, Object>>) selectedItemsObj;
                                            List<OrderItem.SelectedItem> selectedItems = new ArrayList<>();
                                            
                                            for (Map<String, Object> selectedItemMap : selectedItemsList) {
                                                OrderItem.SelectedItem selectedItem = new OrderItem.SelectedItem();
                                                
                                                String itemId = (String) selectedItemMap.get("id");
                                                String itemName = (String) selectedItemMap.get("name");
                                                Object customizationPriceObj = selectedItemMap.get("price");
                                                
                                                selectedItem.setId(itemId);
                                                selectedItem.setName(itemName);
                                                if (customizationPriceObj instanceof Number) {
                                                    selectedItem.setPrice(((Number) customizationPriceObj).doubleValue());
                                                }
                                                
                                                selectedItems.add(selectedItem);
                                            }
                                            
                                            if (!selectedItems.isEmpty()) {
                                                option.setSelectedItems(selectedItems);
                                                customizationOptions.add(option);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("DriverOrdersAdapter", "Error processing customization " + customId + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        
                        if (!customizationOptions.isEmpty()) {
                            orderItem.setCustomizations(customizationOptions);
                        }
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
            holder.deliveryFee.setVisibility(View.VISIBLE);
            holder.deliveryFee.setText("Delivery Fee: " + NumberFormat.getCurrencyInstance(Locale.US).format(deliveryFee));
        } else {
            holder.deliveryFee.setVisibility(View.GONE);
        }
        
        // Load total from Firebase
        if (finalOrderId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("orders")
                    .child(finalOrderId)
                    .child("total")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Object totalObj = dataSnapshot.getValue();
                            if (totalObj instanceof Number) {
                                double total = ((Number) totalObj).doubleValue();
                                holder.totalAmount.setText("Total Amount: " + NumberFormat.getCurrencyInstance(Locale.US).format(total));
                            } else {
                                holder.totalAmount.setText("Total Amount: " + NumberFormat.getCurrencyInstance(Locale.US).format(0.0));
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            holder.totalAmount.setText("Total Amount: " + NumberFormat.getCurrencyInstance(Locale.US).format(0.0));
                        }
                    });
        } else {
            holder.totalAmount.setText("Total Amount: " + NumberFormat.getCurrencyInstance(Locale.US).format(0.0));
        }

        // Setup button click listeners with null check
        if (finalOrderId != null && !finalOrderId.isEmpty()) {
            holder.acceptButton.setOnClickListener(v -> {
                Log.d("DriverOrdersAdapter", "Accept button clicked for order: " + finalOrderId);
                acceptOrder(finalOrderId);
            });
            holder.rejectButton.setOnClickListener(v -> rejectOrder(finalOrderId));
            holder.markPickedUpButton.setOnClickListener(v -> markOrderAsPickedUp(finalOrderId));
            holder.markDeliveredButton.setOnClickListener(v -> markOrderAsDelivered(finalOrderId));
        } else {
            Log.e("DriverOrdersAdapter", "Order ID is null or empty, disabling buttons");
            holder.acceptButton.setEnabled(false);
            holder.rejectButton.setEnabled(false);
            holder.markPickedUpButton.setEnabled(false);
            holder.markDeliveredButton.setEnabled(false);
        }
    }

    private void acceptOrder(String orderId) {
        Log.d("DriverOrdersAdapter", "Attempting to accept order: " + orderId);
        
        if (orderId == null || orderId.isEmpty()) {
            Log.e("DriverOrdersAdapter", "Cannot accept order - invalid order ID");
            if (context != null) {
                Toast.makeText(context, "Invalid order ID", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        
        // First check if the order exists and is still available
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("DriverOrdersAdapter", "Order not found: " + orderId);
                    if (context != null) {
                        Toast.makeText(context, "Order not found", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                String currentStatus = snapshot.child("order_status").getValue(String.class);
                String currentDriverId = snapshot.child("driverId").getValue(String.class);
                
                Log.d("DriverOrdersAdapter", "Current order status: " + currentStatus);
                Log.d("DriverOrdersAdapter", "Current driver ID: " + currentDriverId);

                if (currentStatus == null || !currentStatus.equals("assigned_driver")) {
                    Log.e("DriverOrdersAdapter", "Order is not in assigned_driver status. Current status: " + currentStatus);
                    if (context != null) {
                        Toast.makeText(context, "Order is no longer available", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // Proceed with accepting the order
                Map<String, Object> updates = new HashMap<>();
                updates.put("order_status", "driver_accepted");
                updates.put("driverAccepted", true);

                // First update the order status
                orderRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Then update driver availability
                        driverRef.child("isAvailable").setValue(false)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("DriverOrdersAdapter", "Successfully accepted order and updated availability: " + orderId);
                                if (context != null) {
                                    Toast.makeText(context, "Order accepted", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("DriverOrdersAdapter", "Failed to update driver availability: " + e.getMessage());
                                if (context != null) {
                                    Toast.makeText(context, "Warning: Order accepted but availability update failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DriverOrdersAdapter", "Failed to accept order: " + e.getMessage());
                        e.printStackTrace();
                        if (context != null) {
                            Toast.makeText(context, "Failed to accept order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DriverOrdersAdapter", "Error checking order status: " + error.getMessage());
                if (context != null) {
                    Toast.makeText(context, "Error checking order status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void rejectOrder(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        
        // First update the order status
        Map<String, Object> orderUpdates = new HashMap<>();
        orderUpdates.put("order_status", "ready_for_pickup");
        orderUpdates.put("driverId", null);
        orderUpdates.put("driverName", null);
        orderUpdates.put("driverAccepted", false);

        orderRef.updateChildren(orderUpdates)
            .addOnSuccessListener(aVoid -> {
                // After order is updated, increment the rejected orders count
                driverRef.child("rejectedOrdersCount").runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Integer currentCount = currentData.getValue(Integer.class);
                        if (currentCount == null) {
                            currentData.setValue(1);
                        } else {
                            currentData.setValue(currentCount + 1);
                        }
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                        if (error != null) {
                            Log.e("DriverOrdersAdapter", "Error updating rejected orders count", error.toException());
                            if (context != null) {
                                Toast.makeText(context, "Failed to update rejected orders count", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                if (context != null) {
                    Toast.makeText(context, "Order rejected", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                if (context != null) {
                    Toast.makeText(context, "Failed to reject order", Toast.LENGTH_SHORT).show();
                }
                Log.e("DriverOrdersAdapter", "Failed to reject order", e);
            });
    }

    private void markOrderAsPickedUp(String orderId) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("order_status", "picked_up");
        updates.put("status", "in_progress");

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
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);

        // First update the order status
        Map<String, Object> orderUpdates = new HashMap<>();
        orderUpdates.put("status", "delivered");
        orderUpdates.put("order_status", "delivered");

        orderRef.updateChildren(orderUpdates)
            .addOnSuccessListener(aVoid -> {
                // After order is updated, update driver availability
                driverRef.child("isAvailable").setValue(true)
                    .addOnSuccessListener(aVoid2 -> {
                        if (context != null) {
                            Toast.makeText(context, "Order marked as delivered", Toast.LENGTH_SHORT).show();
                        }
                        Log.d("DriverOrdersAdapter", "Successfully marked order as delivered and updated driver availability");
                    })
                    .addOnFailureListener(e -> {
                        if (context != null) {
                            Toast.makeText(context, "Warning: Order delivered but driver status update failed", Toast.LENGTH_SHORT).show();
                        }
                        Log.e("DriverOrdersAdapter", "Failed to update driver availability", e);
                    });
            })
            .addOnFailureListener(e -> {
                if (context != null) {
                    Toast.makeText(context, "Failed to update order status", Toast.LENGTH_SHORT).show();
                }
                Log.e("DriverOrdersAdapter", "Failed to mark order as delivered", e);
            });
    }

    private void loadRestaurantName(String restaurantId, TextView restaurantNameView) {
        if (restaurantId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("restaurants")
                    .child(restaurantId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String name = dataSnapshot.child("store_info").child("name").getValue(String.class);
                                String address = dataSnapshot.child("store_info").child("address").getValue(String.class);
                                restaurantNameView.setText("From: " + (name != null ? name : "Unknown Restaurant"));
                                
                                // Show restaurant address
                                TextView restaurantAddressView = ((ViewHolder) restaurantNameView.getTag()).restaurantAddress;
                                if (address != null && restaurantAddressView != null) {
                                    restaurantAddressView.setText("Restaurant Address: " + address);
                                    restaurantAddressView.setVisibility(View.VISIBLE);
                                }
                            } else {
                                restaurantNameView.setText("From: Unknown Restaurant");
                            }
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
                    .child("fullName")
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

    private void loadDeliveryAddress(String orderId, TextView addressView) {
        if (orderId != null) {
            FirebaseDatabase.getInstance().getReference()
                    .child("orders")
                    .child(orderId)
                    .child("address")
                    .child("street")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String address = dataSnapshot.getValue(String.class);
                            if (address != null) {
                                addressView.setText("Delivery Address: " + address);
                                addressView.setVisibility(View.VISIBLE);
                            } else {
                                addressView.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            addressView.setVisibility(View.GONE);
                        }
                    });
        } else {
            addressView.setVisibility(View.GONE);
        }
    }

    private void showChatDialog(Order order) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chat);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RecyclerView messagesRecyclerView = dialog.findViewById(R.id.messagesRecyclerView);
        EditText messageInput = dialog.findViewById(R.id.messageInput);
        Button sendButton = dialog.findViewById(R.id.sendButton);
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);

        dialogTitle.setText("Chat with Customer");

        // Set up RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        List<ChatMessage> messages = new ArrayList<>();
        ChatAdapter chatAdapter = new ChatAdapter(messages);
        messagesRecyclerView.setAdapter(chatAdapter);

        // Get current driver info
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId);
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String driverName = snapshot.child("name").getValue(String.class);
                
                // Set up message listener
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(order.getId())
                    .child("driver_customer_messages");

                messagesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        messages.clear();
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                            if (message != null) {
                                messages.add(message);
                            }
                        }
                        chatAdapter.notifyDataSetChanged();
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error loading messages", error.toException());
                    }
                });

                // Handle send button click
                sendButton.setOnClickListener(v -> {
                    String messageText = messageInput.getText().toString().trim();
                    if (!messageText.isEmpty()) {
                        // Create message object
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("message", messageText);
                        messageData.put("senderId", driverId);
                        messageData.put("senderName", driverName);
                        messageData.put("senderType", "driver");
                        messageData.put("timestamp", System.currentTimeMillis());

                        // Save message
                        messagesRef.push().setValue(messageData);
                        messageInput.setText("");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting driver name", error.toException());
            }
        });

        dialog.show();
        
        // Set dialog width to match parent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Chat Message class
    private static class ChatMessage {
        private String message;
        private String senderId;
        private String senderName;
        private String senderType;
        private long timestamp;

        public ChatMessage() {}

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    // Chat Adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getMessage());
            holder.senderName.setText(message.getSenderName());
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(message.getTimestamp()));
            holder.timestamp.setText(time);

            // Align messages based on sender type
            if (message.getSenderType().equals("driver")) {
                holder.messageLayout.setGravity(android.view.Gravity.END);
                holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_sent);
            } else {
                holder.messageLayout.setGravity(android.view.Gravity.START);
                holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_received);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText, senderName, timestamp;
            LinearLayout messageLayout;

            MessageViewHolder(View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.messageText);
                senderName = itemView.findViewById(R.id.senderName);
                timestamp = itemView.findViewById(R.id.timestamp);
                messageLayout = itemView.findViewById(R.id.messageLayout);
            }
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
        TextView orderNumber, orderStatus, restaurantName, restaurantAddress, customerName, totalAmount, deliveryFee, deliveryAddress;
        Button acceptButton, rejectButton, markDeliveredButton, markPickedUpButton, chatButton;
        RecyclerView orderItemsRecyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            restaurantAddress = itemView.findViewById(R.id.restaurantAddress);
            customerName = itemView.findViewById(R.id.customerName);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            deliveryAddress = itemView.findViewById(R.id.deliveryAddress);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            markDeliveredButton = itemView.findViewById(R.id.markDeliveredButton);
            markPickedUpButton = itemView.findViewById(R.id.markPickedUpButton);
            chatButton = itemView.findViewById(R.id.chatButton);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
        }
    }
} 