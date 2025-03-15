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
    private OnAssignDriverClickListener onAssignDriverClickListener;

    public interface OnAssignDriverClickListener {
        void onAssignDriverClick(Order order);
    }

    public void setOnAssignDriverClickListener(OnAssignDriverClickListener listener) {
        this.onAssignDriverClickListener = listener;
    }

    public OrdersAdapter(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant_order, parent, false);
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
        
        // Set the displayed status text
        String displayStatus = !orderStatus.isEmpty() ? orderStatus : status;
        holder.orderStatus.setText(displayStatus.substring(0, 1).toUpperCase() + displayStatus.substring(1));
        
        // Hide all buttons initially
        holder.acceptButton.setVisibility(View.GONE);
        holder.readyForPickupButton.setVisibility(View.GONE);
        holder.assignDriverButton.setVisibility(View.GONE);
        holder.markDeliveredButton.setVisibility(View.GONE);
        
        // Show appropriate buttons based on status
        if (status.equals("new") || status.equals("pending")) {
            // New or pending order - show accept button
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.orderStatus.setBackgroundResource(R.color.orange);
        } else if (status.equals("in_progress")) {
            if (orderStatus.equals("accepted")) {
                // Order accepted - show ready for pickup button
                holder.readyForPickupButton.setVisibility(View.VISIBLE);
                holder.orderStatus.setBackgroundResource(R.color.green);
            } else if (orderStatus.equals("ready_for_pickup")) {
                // Ready for pickup - show appropriate button based on delivery option
                String deliveryOption = order.getDeliveryOption();
                if ("delivery".equalsIgnoreCase(deliveryOption)) {
                    // For delivery orders, show assign driver button
                    holder.assignDriverButton.setVisibility(View.VISIBLE);
                    holder.orderStatus.setBackgroundResource(R.color.blue);
                } else {
                    // For pickup orders, show mark as delivered button
                    holder.markDeliveredButton.setVisibility(View.VISIBLE);
                    holder.orderStatus.setBackgroundResource(R.color.purple);
                }
            } else if (orderStatus.equals("assigned_driver") || 
                      orderStatus.equals("driver_accepted") || 
                      orderStatus.equals("out_for_delivery")) {
                // Driver assigned/accepted or out for delivery - show mark as delivered button
                holder.markDeliveredButton.setVisibility(View.VISIBLE);
                holder.orderStatus.setBackgroundResource(R.color.purple);
            }
        } else if (status.equals("delivered")) {
            holder.orderStatus.setBackgroundResource(R.color.green);
        }

        // Set customer name and delivery option
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Unknown Customer";
        String deliveryOption = order.getDeliveryOption() != null ? order.getDeliveryOption() : "Unknown";
        holder.customerName.setText(customerName);
        holder.deliveryOption.setText(deliveryOption);

        // Set total amount with proper formatting
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        double totalAmount = order.getTotalAmount();
        if (totalAmount <= 0 && order.getItems() != null) {
            // Calculate total from items if total amount is not set
            totalAmount = 0;
            for (OrderItem item : order.getItems()) {
                totalAmount += item.getPrice() * item.getQuantity();
            }
        }
        holder.totalAmount.setText(format.format(totalAmount));

        // Setup order items recycler view
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
            OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(order.getItems());
            holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
            holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            holder.orderItemsRecyclerView.setVisibility(View.GONE);
        }

        // Setup button click listeners
        holder.acceptButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "accepted");
        });

        holder.readyForPickupButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "ready_for_pickup");
        });

        holder.assignDriverButton.setOnClickListener(v -> {
            if (onAssignDriverClickListener != null) {
                onAssignDriverClickListener.onAssignDriverClick(order);
            }
        });

        holder.markDeliveredButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "delivered");
        });
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
            .child("orders")
            .child(orderId);

        Map<String, Object> updates = new HashMap<>();
        final String message;

        switch (newStatus) {
            case "accepted":
                updates.put("status", "in_progress");
                updates.put("order_status", "accepted");
                message = "Order accepted";
                break;
            case "ready_for_pickup":
                updates.put("order_status", "ready_for_pickup");
                message = "Order marked as ready for pickup";
                break;
            case "delivered":
                updates.put("status", "delivered");
                updates.put("order_status", "delivered");
                message = "Order marked as delivered";
                break;
            default:
                message = "Status updated";
                break;
        }

        orderRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to update order: " + e.getMessage(),
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
        TextView orderNumber, orderStatus, customerName, totalAmount, deliveryOption;
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
            deliveryOption = itemView.findViewById(R.id.deliveryOption);
        }
    }
} 