package com.example.deligoandroid.Customer.Adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
            String displayStatus = !orderStatus.isEmpty() ? orderStatus : status;
            holder.orderStatus.setText(displayStatus.substring(0, 1).toUpperCase() + displayStatus.substring(1).replace("_", " "));
            
            // Set status background color
            int backgroundColor;
            if (status.equals("delivered")) {
                backgroundColor = R.color.green;
            } else if (status.equals("in_progress")) {
                backgroundColor = R.color.blue;
            } else {
                backgroundColor = R.color.purple;
            }
            holder.orderStatus.getBackground().setTint(holder.itemView.getContext().getResources().getColor(backgroundColor, null));

            // Load restaurant name
            loadRestaurantName(order.getRestaurantId(), holder.restaurantName);

            // Set delivery type
            holder.deliveryType.setVisibility(View.VISIBLE);
            holder.deliveryType.setText("Delivery");

            // Set up order items
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
                Map<String, Object> customizations = order.getCustomizations();
                Log.d("CustomerOrdersAdapter", "Customizations from order: " + (customizations != null ? customizations.toString() : "null"));
                
                CustomerOrderItemsAdapter itemsAdapter = new CustomerOrderItemsAdapter(order.getItems(), customizations);
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
            .getReference("carts")
            .child(customerId);

        // Create a new cart item with the same specifications
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("restaurantId", order.getRestaurantId());
        cartData.put("items", order.getItems());
        cartData.put("customizations", order.getCustomizations());
        cartData.put("timestamp", System.currentTimeMillis());

        // Add address information
        Map<String, Object> addressData = new HashMap<>();
        if (order.getAddress() != null) {
            addressData.put("street", order.getAddress().getStreet());
            addressData.put("unit", order.getAddress().getUnit());
            // Add instructions if present
            if (order.getAddress().getInstructions() != null && !order.getAddress().getInstructions().isEmpty()) {
                addressData.put("instructions", order.getAddress().getInstructions());
            }
        } else {
            // If no address in order, check if there's address data directly in the order
            DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(order.getId())
                .child("address");
            
            orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String street = snapshot.child("street").getValue(String.class);
                        String unit = snapshot.child("unit").getValue(String.class);
                        String instructions = snapshot.child("instructions").getValue(String.class);
                        
                        if (street != null) addressData.put("street", street);
                        if (unit != null) addressData.put("unit", unit);
                        if (instructions != null && !instructions.isEmpty()) {
                            addressData.put("instructions", instructions);
                        }
                        
                        cartData.put("address", addressData);
                        cartRef.setValue(cartData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Items added to cart", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to add items to cart. Please try again.", Toast.LENGTH_SHORT).show();
                            });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error fetching address data", error.toException());
                    // Still save the cart data even if address fetch fails
                    cartRef.setValue(cartData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Items added to cart", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to add items to cart. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                }
            });
            return; // Return here as we're handling the cart save in the callback
        }

        cartData.put("address", addressData);
        cartRef.setValue(cartData)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, "Items added to cart", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to add items to cart. Please try again.", Toast.LENGTH_SHORT).show();
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, restaurantName, deliveryType, deliveryFee, totalAmount;
        RecyclerView orderItemsRecyclerView;
        LinearLayout deliveredOrderActions;
        MaterialButton rateOrderButton, reorderButton;

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
        }
    }
} 