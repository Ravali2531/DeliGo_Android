package com.example.deligoandroid.Restaurant.Adapters;

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
import com.example.deligoandroid.Restaurant.Models.Order;
import com.example.deligoandroid.Restaurant.Models.OrderItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.app.AlertDialog;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.Query;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
    private List<Order> orders = new ArrayList<>();
    private Context context;
    private String restaurantId;

    public OrdersAdapter(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Order order = orders.get(position);
        if (order == null) return;
        
        // Set order number and timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String orderTime = sdf.format(new Date(order.getTimestamp()));
        String orderId = order.getId() != null ? order.getId() : "";
        holder.orderNumber.setText("Order #" + orderId + " • " + orderTime);
        
        // Set status with appropriate color
        String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";
        String orderStatus = order.getOrderStatus() != null ? order.getOrderStatus().toLowerCase() : "";
        
        // If status is ready_for_pickup, treat it as in_progress
        if (status.equalsIgnoreCase("ready_for_pickup")) {
            status = "in_progress";
        }
        
        // If orderStatus is empty but status is in_progress, set orderStatus to in_progress
        if (orderStatus.isEmpty() && status.equalsIgnoreCase("in_progress")) {
            orderStatus = "in_progress";
            // Update the order_status in Firebase
            DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
                .child("orders")
                .child(order.getId());
            orderRef.child("order_status").setValue("in_progress");
        }
        
        // Set the displayed status text based on orderStatus if it exists, otherwise use status
        String displayStatus = !orderStatus.isEmpty() ? orderStatus : status;
        holder.orderStatus.setText(displayStatus.substring(0, 1).toUpperCase() + displayStatus.substring(1));
        
        // Show/hide action buttons based on status and delivery option
        String deliveryOption = order.getDeliveryOption();
        boolean isDelivery = "delivery".equalsIgnoreCase(deliveryOption);
        
        // Debug logging
        System.out.println("Order ID: " + order.getId());
        System.out.println("Status: " + status);
        System.out.println("Order Status: " + orderStatus);
        System.out.println("Delivery Option: " + deliveryOption);
        
        // Hide all buttons first
        holder.acceptButton.setVisibility(View.GONE);
        holder.rejectButton.setVisibility(View.GONE);
        holder.assignDriverButton.setVisibility(View.GONE);
        holder.markDeliveredButton.setVisibility(View.GONE);
        holder.readyForPickupButton.setVisibility(View.GONE);
        
        if (status.equalsIgnoreCase("pending")) {
            // Show accept/reject for pending orders
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.VISIBLE);
            holder.orderStatus.setBackgroundResource(R.color.orange);
        } else if (status.equalsIgnoreCase("in_progress") || status.equalsIgnoreCase("ready_for_pickup")) {
            // Show appropriate button based on delivery option and order status
            holder.actionButtons.setVisibility(View.VISIBLE);
            if (isDelivery) {
                if (orderStatus.equalsIgnoreCase("assigned_driver")) {
                    holder.markDeliveredButton.setVisibility(View.VISIBLE);
                } else {
                    holder.assignDriverButton.setVisibility(View.VISIBLE);
                }
            } else {
                // For pickup orders
                System.out.println("Pickup order in progress - Order Status: " + orderStatus);
                if (orderStatus.equalsIgnoreCase("ready_for_pickup") || status.equalsIgnoreCase("ready_for_pickup")) {
                    System.out.println("Should show Mark Delivered button");
                    holder.markDeliveredButton.setVisibility(View.VISIBLE);
                } else {
                    System.out.println("Should show Ready for Pickup button");
                    holder.readyForPickupButton.setVisibility(View.VISIBLE);
                }
            }
            holder.orderStatus.setBackgroundResource(R.color.green);
        } else {
            holder.actionButtons.setVisibility(View.GONE);
            holder.orderStatus.setBackgroundResource(
                status.equalsIgnoreCase("delivered") ? R.color.blue : android.R.color.darker_gray
            );
        }

        // Set customer details
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Unknown";
        holder.customerName.setText("Customer: " + customerName);

        // Set total amount
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        holder.totalAmount.setText(format.format(order.getTotalAmount()));

        // Setup order items recycler view if items exist
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
            OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(order.getItems());
            holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
            holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            holder.orderItemsRecyclerView.setVisibility(View.GONE);
        }

        // Setup action buttons
        holder.acceptButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "in_progress");
            notifyItemChanged(position);
        });
        
        holder.rejectButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "cancelled");
            notifyItemChanged(position);
        });

        holder.readyForPickupButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "ready");
            notifyItemChanged(position);
        });

        holder.markDeliveredButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "delivered");
            notifyItemChanged(position);
        });

        holder.assignDriverButton.setOnClickListener(v -> {
            // Show dialog to assign driver
            showAssignDriverDialog(order);
        });
    }

    private void showAssignDriverDialog(Order order) {
        // Get available drivers from Firebase
        DatabaseReference driversRef = FirebaseDatabase.getInstance().getReference()
            .child("drivers");
            
        Query availableDriversQuery = driversRef.orderByChild("isAvailable").equalTo(true);

        availableDriversQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> driverNames = new ArrayList<>();
                List<String> driverIds = new ArrayList<>();
                
                for (DataSnapshot driverSnapshot : dataSnapshot.getChildren()) {
                    String driverId = driverSnapshot.getKey();
                    String driverName = driverSnapshot.child("fullName").getValue(String.class);
                    Boolean isAvailable = driverSnapshot.child("isAvailable").getValue(Boolean.class);
                    
                    if (driverName != null && isAvailable != null && isAvailable) {
                        driverNames.add(driverName);
                        driverIds.add(driverId);
                    }
                }

                if (driverNames.isEmpty()) {
                    Toast.makeText(context, "No available drivers found", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create and show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Select Available Driver");
                
                String[] driverNamesArray = driverNames.toArray(new String[0]);
                builder.setItems(driverNamesArray, (dialog, which) -> {
                    String selectedDriverId = driverIds.get(which);
                    String selectedDriverName = driverNames.get(which);
                    assignDriver(order.getId(), selectedDriverId, selectedDriverName);
                });

                builder.show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(context, "Error loading drivers: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOrderStatus(String orderId, String status) {
        if (orderId == null || status == null) return;
        
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
            .child("orders")
            .child(orderId);
            
        Map<String, Object> updates = new HashMap<>();
        
        // Add order_status based on the status and delivery option
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
            .child("orders")
            .child(orderId);
            
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String deliveryOption = dataSnapshot.child("deliveryOption").getValue(String.class);
                String orderStatus = "";
                String message = "";
                
                if ("delivery".equalsIgnoreCase(deliveryOption)) {
                    switch (status) {
                        case "pending":
                            orderStatus = "pending";
                            message = "Order is pending";
                            updates.put("status", "pending");
                            break;
                        case "in_progress":
                            orderStatus = "in_progress";
                            message = "Order accepted";
                            updates.put("status", "in_progress");
                            break;
                        case "assigned_to_driver":
                            orderStatus = "assigned_driver";
                            message = "Driver assigned to order";
                            updates.put("status", "in_progress");
                            break;
                        case "delivered":
                            orderStatus = "delivered";
                            message = "Order marked as delivered";
                            updates.put("status", "delivered");
                            break;
                    }
                } else {
                    // For pickup option
                    switch (status) {
                        case "pending":
                            orderStatus = "pending";
                            message = "Order is pending";
                            updates.put("status", "pending");
                            break;
                        case "in_progress":
                            orderStatus = "in_progress";
                            message = "Order accepted";
                            updates.put("status", "in_progress");
                            break;
                        case "ready":
                            orderStatus = "ready_for_pickup";
                            message = "Order is ready for pickup";
                            updates.put("status", "ready_for_pickup");
                            break;
                        case "delivered":
                            orderStatus = "delivered";
                            message = "Order marked as delivered";
                            updates.put("status", "delivered");
                            break;
                        case "cancelled":
                            orderStatus = "cancelled";
                            message = "Order rejected";
                            updates.put("status", "cancelled");
                            break;
                    }
                }
                
                // Always ensure order_status is set
                updates.put("order_status", orderStatus);

                // Debug logging for updates
                System.out.println("Updating order: " + orderId);
                System.out.println("Updates to be applied: " + updates.toString());

                final String finalMessage = message;
                final String finalOrderStatus = orderStatus;

                orderRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (context != null) {
                            System.out.println("Order update successful");
                            System.out.println("New status: " + status);
                            System.out.println("New order_status: " + finalOrderStatus);
                            Toast.makeText(context, finalMessage, Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();  // Refresh the entire list to ensure UI is updated
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (context != null) {
                            Toast.makeText(context, 
                                "Failed to update order status: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(context, "Error updating order status: " + databaseError.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignDriver(String orderId, String driverId, String driverName) {
        // First update the driver details
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
            .child("orders")
            .child(orderId);

        // Update driver details only
        Map<String, Object> driverDetails = new HashMap<>();
        driverDetails.put("driverId", driverId);
        driverDetails.put("driverName", driverName);

        // Update driver availability
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
            .child("drivers")
            .child(driverId);
            
        Map<String, Object> driverUpdates = new HashMap<>();
        driverUpdates.put("isAvailable", false);
        driverUpdates.put("currentOrderId", orderId);

        // First update driver details, then update status
        orderRef.updateChildren(driverDetails)
            .addOnSuccessListener(aVoid -> {
                // After driver details are updated, update the status
                updateOrderStatus(orderId, "assigned_to_driver");
                
                // Update driver availability
                driverRef.updateChildren(driverUpdates)
                    .addOnSuccessListener(aVoid2 -> {
                        Toast.makeText(context, "Driver assigned successfully", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to update driver status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to assign driver: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, customerName, totalAmount;
        RecyclerView orderItemsRecyclerView;
        Button acceptButton, rejectButton, assignDriverButton, markDeliveredButton, readyForPickupButton;
        LinearLayout actionButtons;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            customerName = itemView.findViewById(R.id.customerName);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            assignDriverButton = itemView.findViewById(R.id.assignDriverButton);
            markDeliveredButton = itemView.findViewById(R.id.markDeliveredButton);
            readyForPickupButton = itemView.findViewById(R.id.readyForPickupButton);
            actionButtons = itemView.findViewById(R.id.actionButtons);
        }
    }
} 