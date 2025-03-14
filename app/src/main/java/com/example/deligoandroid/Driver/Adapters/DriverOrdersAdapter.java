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

        // Show/hide action buttons based on status
        holder.acceptButton.setVisibility(View.GONE);
        holder.rejectButton.setVisibility(View.GONE);
        holder.startDeliveryButton.setVisibility(View.GONE);
        holder.deliveredButton.setVisibility(View.GONE);

        if (orderStatus.equalsIgnoreCase("assigned_driver") && !order.isDriverAccepted()) {
            // Show accept/reject buttons if driver hasn't accepted yet
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.rejectButton.setVisibility(View.VISIBLE);
            holder.orderStatus.setBackgroundResource(R.color.orange);
        } else if (orderStatus.equalsIgnoreCase("assigned_driver") && order.isDriverAccepted()) {
            // Show start delivery button if driver has accepted
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.startDeliveryButton.setVisibility(View.VISIBLE);
            holder.orderStatus.setBackgroundResource(R.color.green);
        } else if (orderStatus.equalsIgnoreCase("out_for_delivery")) {
            // Show delivered button when out for delivery
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.deliveredButton.setVisibility(View.VISIBLE);
            holder.orderStatus.setBackgroundResource(R.color.green);
        } else {
            holder.actionButtons.setVisibility(View.GONE);
            holder.orderStatus.setBackgroundResource(
                status.equalsIgnoreCase("delivered") ? R.color.blue : android.R.color.darker_gray
            );
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
        holder.acceptButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "driver_accepted");
        });

        holder.rejectButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "driver_rejected");
        });

        holder.startDeliveryButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "out_for_delivery");
        });

        holder.deliveredButton.setOnClickListener(v -> {
            updateOrderStatus(order.getId(), "delivered");
        });
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
            .child("orders")
            .child(orderId);

        Map<String, Object> updates = new HashMap<>();
        String message = "";

        switch (newStatus) {
            case "driver_accepted":
                updates.put("driverAccepted", true);
                updates.put("order_status", "assigned_driver");
                message = "Order accepted";
                break;
            case "driver_rejected":
                updates.put("driverId", null);
                updates.put("driverName", null);
                updates.put("order_status", "in_progress");
                message = "Order rejected";
                // Make driver available again
                updateDriverAvailability(true, null);
                break;
            case "out_for_delivery":
                updates.put("order_status", "out_for_delivery");
                message = "Started delivery";
                break;
            case "delivered":
                updates.put("status", "delivered");
                updates.put("order_status", "delivered");
                message = "Order delivered";
                // Make driver available again
                updateDriverAvailability(true, null);
                break;
        }

        orderRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
//                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to update order: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void updateDriverAvailability(boolean isAvailable, String currentOrderId) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
            .child("drivers")
            .child(driverId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isAvailable", isAvailable);
        updates.put("currentOrderId", currentOrderId);

        driverRef.updateChildren(updates)
            .addOnFailureListener(e -> {
                Toast.makeText(context, "Failed to update driver status: " + e.getMessage(),
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
        TextView orderNumber, orderStatus, restaurantName, customerName, totalAmount;
        RecyclerView orderItemsRecyclerView;
        Button acceptButton, rejectButton, startDeliveryButton, deliveredButton;
        LinearLayout actionButtons;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            customerName = itemView.findViewById(R.id.customerName);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            startDeliveryButton = itemView.findViewById(R.id.startDeliveryButton);
            deliveredButton = itemView.findViewById(R.id.deliveredButton);
            actionButtons = itemView.findViewById(R.id.actionButtons);
        }
    }
} 