package com.example.deligoandroid.Customer.Adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Activities.ChatActivity;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.Customer.Models.OrderItem;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Utils.PdfGenerator;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ServerValue;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.io.File;

public class CustomerOrdersAdapter extends RecyclerView.Adapter<CustomerOrdersAdapter.ViewHolder> {
    private static final String TAG = "CustomerOrdersAdapter";
    private List<Order> orders;
    private NumberFormat currencyFormat;
    private Context context;
    private OnReorderClickListener reorderClickListener;

    public interface OnReorderClickListener {
        void onReorderClick(Order order);
    }

    public CustomerOrdersAdapter(List<Order> orders, OnReorderClickListener listener) {
        this.orders = orders;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        this.reorderClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_customer_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        if (order == null) return;

        try {
            // Set order number and timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            String orderTime = sdf.format(new Date(order.getTimestamp()));
            String orderId = order.getId() != null ? order.getId() : "Unknown";
            holder.orderNumber.setText("Order #" + orderId + " • " + orderTime);

            // Set status with appropriate color
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
            String orderStatus = order.getOrderStatus() != null ? order.getOrderStatus().toLowerCase() : "";
            
            Log.d(TAG, "Order ID: " + orderId);
            Log.d(TAG, "Status: " + status);
            Log.d(TAG, "Order Status: " + orderStatus);
            
            // Set the displayed status text
            String displayStatus;
            if (orderStatus != null && !orderStatus.isEmpty()) {
                switch (orderStatus) {
                    case "accepted":
                        displayStatus = "Restaurant Accepted";
                        break;
                    case "driver_accepted":
                        displayStatus = "Driver Assigned";
                        break;
                    case "picked_up":
                        displayStatus = "Picked Up";
                        break;
                    case "ready_for_pickup":
                        displayStatus = "Ready for Pickup";
                        break;
                    default:
                        displayStatus = orderStatus.substring(0, 1).toUpperCase() + orderStatus.substring(1).replace("_", " ");
                        break;
                }
            } else {
                displayStatus = status.substring(0, 1).toUpperCase() + status.substring(1).replace("_", " ");
            }
            holder.orderStatus.setText(displayStatus);
            
            // Set status background color
            int backgroundColor;
            if (status.equals("delivered")) {
                backgroundColor = R.color.green;
            } else if (orderStatus.equals("driver_accepted") || orderStatus.equals("picked_up")) {
                backgroundColor = R.color.blue;
            } else if (orderStatus.equals("accepted") || orderStatus.equals("ready_for_pickup")) {
                backgroundColor = R.color.orange;
            } else {
                backgroundColor = R.color.purple;
            }
            holder.orderStatus.getBackground().setTint(holder.itemView.getContext().getResources().getColor(backgroundColor, null));

            // Load restaurant name
            loadRestaurantName(order.getRestaurantId(), holder.restaurantName);

            // Set delivery type
            holder.deliveryType.setVisibility(View.VISIBLE);
            holder.deliveryType.setText("Delivery");

            // Show chat button if driver has accepted the order
            if (orderStatus.equals("picked_up")) {
                holder.driverChatButton.setVisibility(View.VISIBLE);
                holder.driverChatButton.setOnClickListener(v -> showChatDialog(order, "driver"));
            } else {
                holder.driverChatButton.setVisibility(View.GONE);
            }

            // Set up order items
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
                
                // Convert items to OrderItem objects
                List<OrderItem> orderItems = new ArrayList<>();
                for (Map<String, Object> itemMap : order.getItems()) {
                    OrderItem item = new OrderItem();
                    item.setItemId((String) itemMap.get("itemId"));
                    item.setName((String) itemMap.get("name"));
                    item.setQuantity(itemMap.get("quantity") instanceof Long ? ((Long) itemMap.get("quantity")).intValue() : 1);
                    
                    Object priceObj = itemMap.get("price");
                    if (priceObj instanceof Double) {
                        item.setPrice((Double) priceObj);
                    } else if (priceObj instanceof Long) {
                        item.setPrice(((Long) priceObj).doubleValue());
                    }
                    
                    item.setSpecialInstructions((String) itemMap.get("specialInstructions"));

                    // Handle customizations
                    Object customizationsObj = itemMap.get("customizations");
                    Log.d(TAG, "Raw customizations object: " + (customizationsObj != null ? customizationsObj.toString() : "null"));
                    
                    if (customizationsObj instanceof Map) {
                        Map<String, Object> customizationsMap = (Map<String, Object>) customizationsObj;
                        List<OrderItem.CustomizationOption> customizationOptions = new ArrayList<>();
                        
                        Log.d(TAG, "Found customizations map with " + customizationsMap.size() + " entries");
                        
                        for (Map.Entry<String, Object> entry : customizationsMap.entrySet()) {
                            String customId = entry.getKey();
                            Object customValue = entry.getValue();
                            
                            Log.d(TAG, "Processing customization: " + customId + ", value type: " + (customValue != null ? customValue.getClass().getSimpleName() : "null"));
                            Log.d(TAG, "Customization value: " + customValue);
                            
                            try {
                                if (customValue instanceof ArrayList) {
                                    ArrayList<Map<String, Object>> customizationList = (ArrayList<Map<String, Object>>) customValue;
                                    if (!customizationList.isEmpty()) {
                                        Map<String, Object> optionData = customizationList.get(0);
                                        Log.d(TAG, "Option data: " + optionData);
                                        
                                        OrderItem.CustomizationOption option = new OrderItem.CustomizationOption();
                                        option.setOptionId((String) optionData.get("optionId"));
                                        option.setOptionName((String) optionData.get("optionName"));
                                        
                                        Log.d(TAG, "Found option: " + option.getOptionName() + " with ID: " + option.getOptionId());
                                        
                                        // Get selectedItems from the option data
                                        Object selectedItemsObj = optionData.get("selectedItems");
                                        if (selectedItemsObj instanceof List) {
                                            List<Map<String, Object>> selectedItemsData = (List<Map<String, Object>>) selectedItemsObj;
                                            List<OrderItem.SelectedItem> selectedItems = new ArrayList<>();
                                            
                                            for (Map<String, Object> selectedItemMap : selectedItemsData) {
                                                OrderItem.SelectedItem selectedItem = new OrderItem.SelectedItem();
                                                String itemId = (String) selectedItemMap.get("id");
                                                String itemName = (String) selectedItemMap.get("name");
                                                Object itemPrice = selectedItemMap.get("price");
                                                
                                                selectedItem.setId(itemId);
                                                selectedItem.setName(itemName);
                                                
                                                if (itemPrice instanceof Number) {
                                                    selectedItem.setPrice(((Number) itemPrice).doubleValue());
                                                }
                                                
                                                selectedItems.add(selectedItem);
                                                Log.d(TAG, "Added selected item: " + itemName + " with ID: " + itemId);
                                            }
                                            
                                            if (!selectedItems.isEmpty()) {
                                                option.setSelectedItems(selectedItems);
                                                customizationOptions.add(option);
                                                Log.d(TAG, "Added option " + option.getOptionName() + " with " + selectedItems.size() + " items");
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing customization " + customId + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        
                        if (!customizationOptions.isEmpty()) {
                            item.setCustomizations(customizationOptions);
                            Log.d(TAG, "Successfully set " + customizationOptions.size() + " customization options on item");
                        }
                    }
                    
                    orderItems.add(item);
                }
                
                CustomerOrderItemsAdapter itemsAdapter = new CustomerOrderItemsAdapter(orderItems);
                holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
                holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            } else {
                holder.orderItemsRecyclerView.setVisibility(View.GONE);
            }

            // Set delivery fee and total
            Double deliveryFee = order.getDeliveryFee();
            if (deliveryFee != null && deliveryFee > 0) {
                holder.deliveryFee.setText(currencyFormat.format(deliveryFee));
            } else {
                holder.deliveryFee.setText(currencyFormat.format(0));
            }

            // Get total directly from order data
            Object totalObj = order.getTotal();
            double total = 0.0;
            if (totalObj instanceof Number) {
                total = ((Number) totalObj).doubleValue();
            }
            holder.totalAmount.setText(currencyFormat.format(total));

            // Handle delivered order actions
            if (status.equals("delivered")) {
                holder.deliveredOrderActions.setVisibility(View.VISIBLE);
                
                // Handle rate order button click
                holder.rateOrderButton.setOnClickListener(v -> showRatingDialog(order));
                
                // Handle reorder button click
                holder.reorderButton.setOnClickListener(v -> showReorderConfirmationDialog(order));

                // Handle download receipt button click
                holder.downloadReceiptButton.setOnClickListener(v -> {
                    Map<String, Object> orderMap = order.toMap();
                    PdfGenerator.generateOrderReceipt(holder.itemView.getContext(), orderMap, file -> {
                        if (file != null) {
                            Toast.makeText(holder.itemView.getContext(), 
                                "Receipt saved!\n\nTo find it:\n1. Open Files app or File Manager\n2. Go to Downloads > DeliGo_Receipts", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(holder.itemView.getContext(), 
                                "Failed to generate receipt", 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                // Handle restaurant chat button click
                holder.restaurantChatButton.setOnClickListener(v -> showChatDialog(order, "restaurant"));
            } else {
                holder.deliveredOrderActions.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error binding order", e);
        }
    }

    private void showRatingDialog(Order order) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rate_order_and_driver);
        
        // Get views
        TextView restaurantTab = dialog.findViewById(R.id.restaurantTab);
        TextView driverTab = dialog.findViewById(R.id.driverTab);
        LinearLayout restaurantRatingLayout = dialog.findViewById(R.id.restaurantRatingLayout);
        LinearLayout driverRatingLayout = dialog.findViewById(R.id.driverRatingLayout);
        RatingBar restaurantRatingBar = dialog.findViewById(R.id.restaurantRatingBar);
        RatingBar driverRatingBar = dialog.findViewById(R.id.driverRatingBar);
        EditText restaurantCommentInput = dialog.findViewById(R.id.restaurantCommentInput);
        EditText driverCommentInput = dialog.findViewById(R.id.driverCommentInput);
        MaterialButton submitButton = dialog.findViewById(R.id.submitRatingButton);

        // Fetch existing restaurant rating and comment
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
            .getReference("restaurants")
            .child(order.getRestaurantId())
            .child("ratingsandcomments");

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get rating
                    DataSnapshot ratingSnapshot = snapshot.child("rating").child(customerId);
                    if (ratingSnapshot.exists()) {
                        Float rating = ratingSnapshot.getValue(Float.class);
                        if (rating != null) {
                            restaurantRatingBar.setRating(rating);
                        }
                    }

                    // Get comment
                    DataSnapshot commentSnapshot = snapshot.child("comment").child(customerId);
                    if (commentSnapshot.exists()) {
                        String comment = commentSnapshot.getValue(String.class);
                        if (comment != null) {
                            restaurantCommentInput.setText(comment);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching restaurant rating", error.toException());
            }
        });

        // Fetch existing driver rating and comment if driver exists
        if (order.getDriverId() != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference("drivers")
                .child(order.getDriverId())
                .child("ratingsandcomments");

            driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Get rating
                        DataSnapshot ratingSnapshot = snapshot.child("rating").child(customerId);
                        if (ratingSnapshot.exists()) {
                            Float rating = ratingSnapshot.getValue(Float.class);
                            if (rating != null) {
                                driverRatingBar.setRating(rating);
                            }
                        }

                        // Get comment
                        DataSnapshot commentSnapshot = snapshot.child("comment").child(customerId);
                        if (commentSnapshot.exists()) {
                            String comment = commentSnapshot.getValue(String.class);
                            if (comment != null) {
                                driverCommentInput.setText(comment);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching driver rating", error.toException());
                }
            });
        }

        // Set up tab click listeners
        restaurantTab.setOnClickListener(v -> {
            restaurantTab.setBackgroundColor(context.getResources().getColor(R.color.accent_orange, null));
            restaurantTab.setTextColor(Color.WHITE);
            driverTab.setBackgroundColor(Color.WHITE);
            driverTab.setTextColor(Color.BLACK);
            restaurantRatingLayout.setVisibility(View.VISIBLE);
            driverRatingLayout.setVisibility(View.GONE);
        });

        driverTab.setOnClickListener(v -> {
            driverTab.setBackgroundColor(context.getResources().getColor(R.color.accent_orange, null));
            driverTab.setTextColor(Color.WHITE);
            restaurantTab.setBackgroundColor(Color.WHITE);
            restaurantTab.setTextColor(Color.BLACK);
            driverRatingLayout.setVisibility(View.VISIBLE);
            restaurantRatingLayout.setVisibility(View.GONE);
        });

        // Set initial state
        restaurantTab.performClick();

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            float restaurantRating = restaurantRatingBar.getRating();
            float driverRating = driverRatingBar.getRating();
            String restaurantComment = restaurantCommentInput.getText().toString().trim();
            String driverComment = driverCommentInput.getText().toString().trim();

            // Save restaurant rating and comment
            if (restaurantRating > 0) {
                // Save rating
                restaurantRef.child("rating")
                    .child(customerId)
                    .setValue(restaurantRating);

                // Save comment if provided
                if (!restaurantComment.isEmpty()) {
                    restaurantRef.child("comment")
                        .child(customerId)
                        .setValue(restaurantComment);
                }
            }

            // Save driver rating and comment
            if (driverRating > 0 && order.getDriverId() != null) {
                DatabaseReference driverRef = FirebaseDatabase.getInstance()
                    .getReference("drivers")
                    .child(order.getDriverId())
                    .child("ratingsandcomments");

                // Save rating
                driverRef.child("rating")
                    .child(customerId)
                    .setValue(driverRating)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Thank you for your ratings!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to submit ratings. Please try again.", Toast.LENGTH_SHORT).show();
                    });

                // Save comment if provided
                if (!driverComment.isEmpty()) {
                    driverRef.child("comment")
                        .child(customerId)
                        .setValue(driverComment);
                }
            } else {
                Toast.makeText(context, "Thank you for your ratings!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Show dialog
        dialog.show();
        
        // Set dialog width to match parent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showReorderConfirmationDialog(Order order) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_reorder_confirmation);

        TextView cancelButton = dialog.findViewById(R.id.cancelButton);
        TextView addToCartButton = dialog.findViewById(R.id.addToCartButton);

        // Handle cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Handle add to cart button click
        addToCartButton.setOnClickListener(v -> {
            addOrderToCart(order);
            dialog.dismiss();
        });

        // Show dialog
        dialog.show();
        
        // Set dialog width to match parent
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void addOrderToCart(Order order) {
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
            .getReference("customers")
            .child(customerId)
            .child("cart");

        // Get the first item from the order's items list
        if (order.getItems() == null || order.getItems().isEmpty()) {
            Toast.makeText(context, "No items found in the order", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> firstItem = order.getItems().get(0);
        Log.d(TAG, "First item from order: " + firstItem.toString());
        
        // Generate a unique ID for the cart item
        String cartItemId = cartRef.push().getKey();
        if (cartItemId == null) {
            Toast.makeText(context, "Failed to generate cart item ID", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartItemRef = cartRef.child(cartItemId);

        // Create cart item data
        Map<String, Object> cartItemData = new HashMap<>();
        cartItemData.put("description", firstItem.get("description"));
        cartItemData.put("id", cartItemId);
        cartItemData.put("imageURL", firstItem.get("imageURL"));
        cartItemData.put("menuItemId", firstItem.get("menuItemId"));
        cartItemData.put("name", firstItem.get("name"));
        cartItemData.put("price", firstItem.get("price"));
        cartItemData.put("quantity", 1);
        cartItemData.put("restaurantId", order.getRestaurantId());
        cartItemData.put("specialInstructions", firstItem.get("specialInstructions"));
        cartItemData.put("timestamp", ServerValue.TIMESTAMP);

        // Calculate total price starting with base price
        double totalPrice = firstItem.get("price") != null ? ((Number) firstItem.get("price")).doubleValue() : 0.0;

        // Process customizations
        Object customizationsObj = firstItem.get("customizations");
        Log.d(TAG, "Raw customizations object: " + (customizationsObj != null ? customizationsObj.toString() : "null"));
        
        Map<String, Object> customizationsMap = new HashMap<>();
        
        if (customizationsObj instanceof Map) {
            Map<String, Object> itemCustomizations = (Map<String, Object>) customizationsObj;
            Log.d(TAG, "Found customizations map with " + itemCustomizations.size() + " entries");
            
            for (Map.Entry<String, Object> entry : itemCustomizations.entrySet()) {
                String customizationId = entry.getKey();
                Object customValue = entry.getValue();
                Log.d(TAG, "Processing customization ID: " + customizationId);
                
                if (customValue instanceof ArrayList) {
                    ArrayList<Map<String, Object>> customizationList = (ArrayList<Map<String, Object>>) customValue;
                    if (!customizationList.isEmpty()) {
                        Map<String, Object> optionData = customizationList.get(0);
                        
                        // Create customization data
                        Map<String, Object> newCustomizationData = new HashMap<>();
                        newCustomizationData.put("optionId", optionData.get("optionId"));
                        newCustomizationData.put("optionName", optionData.get("optionName"));
                        newCustomizationData.put("price", optionData.get("price") != null ? optionData.get("price") : 0);
                        
                        // Handle selected items
                        Object selectedItemsObj = optionData.get("selectedItems");
                        if (selectedItemsObj instanceof ArrayList) {
                            ArrayList<Map<String, Object>> selectedItemsList = (ArrayList<Map<String, Object>>) selectedItemsObj;
                            Map<String, Object> selectedItemsContainer = new HashMap<>();
                            
                            for (int i = 0; i < selectedItemsList.size(); i++) {
                                Map<String, Object> selectedItemMap = selectedItemsList.get(i);
                                Map<String, Object> selectedItem = new HashMap<>();
                                selectedItem.put("id", selectedItemMap.get("id"));
                                selectedItem.put("name", selectedItemMap.get("name"));
                                selectedItem.put("price", selectedItemMap.get("price"));
                                
                                Object selectedItemPrice = selectedItemMap.get("price");
                                if (selectedItemPrice instanceof Number) {
                                    totalPrice += ((Number) selectedItemPrice).doubleValue();
                                }
                                
                                selectedItemsContainer.put(String.valueOf(i), selectedItem);
                            }
                            
                            newCustomizationData.put("selectedItems", selectedItemsContainer);
                        }
                        
                        // Add the "0" level structure
                        Map<String, Object> customizationContainer = new HashMap<>();
                        customizationContainer.put("0", newCustomizationData);
                        customizationsMap.put(customizationId, customizationContainer);
                    }
                }
            }
        }
        
        // Add customizations to cartItemData
        cartItemData.put("customizations", customizationsMap);
        cartItemData.put("totalPrice", totalPrice);

        // Save the cart item data
        cartItemRef.setValue(cartItemData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Successfully saved cart item with ID: " + cartItemId);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to add item to cart. Please try again.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error adding item to cart", e);
            });
    }

    // Add a method to handle saving the order with delivery instructions
    private void saveOrderWithInstructions(Order order, String instructions) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance()
            .getReference("orders")
            .child(order.getId())
            .child("address");

        // Update or add instructions
        if (instructions != null && !instructions.isEmpty()) {
            orderRef.child("instructions").setValue(instructions);
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    private void loadRestaurantName(String restaurantId, TextView textView) {
        if (restaurantId == null || restaurantId.isEmpty()) {
            textView.setText("Unknown Restaurant");
            return;
        }

        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("store_info");

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                textView.setText(name != null ? name : "Unknown Restaurant");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textView.setText("Unknown Restaurant");
                Log.e(TAG, "Error loading restaurant name", error.toException());
            }
        });
    }

    private void showChatDialog(Order order, String chatType) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_chat);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        RecyclerView messagesRecyclerView = dialog.findViewById(R.id.messagesRecyclerView);
        EditText messageInput = dialog.findViewById(R.id.messageInput);
        Button sendButton = dialog.findViewById(R.id.sendButton);
        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);

        // Set appropriate title based on chat type
        dialogTitle.setText(chatType.equals("driver") ? "Chat with Driver" : "Chat with Restaurant");

        // Set up RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        List<ChatMessage> messages = new ArrayList<>();
        ChatAdapter chatAdapter = new ChatAdapter(messages);
        messagesRecyclerView.setAdapter(chatAdapter);

        // Get current customer info
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference("customers").child(customerId);
        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String customerName = snapshot.child("fullName").getValue(String.class);
                
                // Set up message listener with appropriate path based on chat type
                String chatPath = chatType.equals("driver") ? "driver_customer_messages" : "messages";
                DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(order.getId())
                    .child(chatPath);

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
                        messageData.put("senderId", customerId);
                        messageData.put("senderName", customerName);
                        messageData.put("senderType", "customer");
                        messageData.put("timestamp", System.currentTimeMillis());

                        // Save message
                        messagesRef.push().setValue(messageData);
                        messageInput.setText("");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting customer name", error.toException());
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
            if (message.getSenderType().equals("customer")) {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, restaurantName, deliveryType, deliveryFee, totalAmount;
        RecyclerView orderItemsRecyclerView;
        LinearLayout deliveredOrderActions;
        MaterialButton rateOrderButton, reorderButton, downloadReceiptButton, restaurantChatButton;
        Button driverChatButton;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            deliveryType = itemView.findViewById(R.id.deliveryType);
            deliveryFee = itemView.findViewById(R.id.deliveryFee);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
            deliveredOrderActions = itemView.findViewById(R.id.deliveredOrderActions);
            rateOrderButton = itemView.findViewById(R.id.rateOrderButton);
            reorderButton = itemView.findViewById(R.id.reorderButton);
            downloadReceiptButton = itemView.findViewById(R.id.downloadReceiptButton);
            driverChatButton = itemView.findViewById(R.id.driverChatButton);
            restaurantChatButton = itemView.findViewById(R.id.restaurantChatButton);
        }
    }
} 