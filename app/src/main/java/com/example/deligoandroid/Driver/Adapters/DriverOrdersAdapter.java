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
import com.example.deligoandroid.Restaurant.Models.OrderItem;
import com.example.deligoandroid.Restaurant.Adapters.OrderItemsAdapter;
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
        holder.rejectButton.setVisibility(View.GONE);
        holder.markPickedUpButton.setVisibility(View.GONE);
        holder.markDeliveredButton.setVisibility(View.GONE);

        // Show appropriate buttons based on status
        if (status.equals("in_progress")) {
            if (orderStatus.equals("assigned_driver") && order.getDriverId().equals(driverId)) {
                // Show accept/reject buttons when order is first assigned
                holder.acceptButton.setVisibility(View.VISIBLE);
                holder.rejectButton.setVisibility(View.VISIBLE);
                holder.orderStatus.setBackgroundResource(R.color.orange);
            } else if (orderStatus.equals("driver_accepted") && order.getDriverId().equals(driverId)) {
                // Show mark as picked up button after driver accepts
                holder.markPickedUpButton.setVisibility(View.VISIBLE);
                holder.orderStatus.setBackgroundResource(R.color.blue);
            } else if (orderStatus.equals("picked_up") && order.getDriverId().equals(driverId)) {
                // Show mark as delivered button after driver picks up
                holder.markDeliveredButton.setVisibility(View.VISIBLE);
                holder.orderStatus.setBackgroundResource(R.color.blue);
            }
        } else if (status.equals("delivered")) {
            holder.orderStatus.setBackgroundResource(R.color.green);
        }

        // Set restaurant and customer details
        String restaurantName = order.getRestaurantName() != null ? order.getRestaurantName() : "Unknown Restaurant";
        String customerName = order.getCustomerName() != null ? order.getCustomerName() : "Unknown Customer";
        holder.restaurantName.setText("From: " + restaurantName);
        holder.customerName.setText("To: " + customerName);

        // Set total amount
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        holder.totalAmount.setText(format.format(order.getTotalAmount()));

        // Setup order items recycler view
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);
            List<OrderItem> orderItems = new ArrayList<>();
            for (Map<String, Object> item : order.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setName((String) item.get("name"));
                orderItem.setQuantity(((Number) item.get("quantity")).intValue());
                orderItem.setPrice(((Number) item.get("price")).doubleValue());
                orderItems.add(orderItem);
            }
            OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(orderItems);
            holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
            holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            holder.orderItemsRecyclerView.setVisibility(View.GONE);
        }

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

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, restaurantName, customerName, totalAmount;
        Button acceptButton, rejectButton, markDeliveredButton, markPickedUpButton;
        RecyclerView orderItemsRecyclerView;

        public ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            customerName = itemView.findViewById(R.id.customerName);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            markDeliveredButton = itemView.findViewById(R.id.markDeliveredButton);
            markPickedUpButton = itemView.findViewById(R.id.markPickedUpButton);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
        }
    }
} 