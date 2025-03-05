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
        
        // Set order number and timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String orderTime = sdf.format(new Date(order.getTimestamp()));
        holder.orderNumber.setText("Order #" + order.getId() + " • " + orderTime);
        
        // Set status with appropriate color
        holder.orderStatus.setText(order.getStatus());
        switch (order.getStatus()) {
            case "PENDING":
                holder.orderStatus.setTextColor(context.getColor(R.color.orange));
                holder.actionButtons.setVisibility(View.VISIBLE);
                break;
            case "ACCEPTED":
            case "PREPARING":
            case "READY":
                holder.orderStatus.setTextColor(context.getColor(android.R.color.holo_green_dark));
                holder.actionButtons.setVisibility(View.GONE);
                break;
            case "CANCELLED":
                holder.orderStatus.setTextColor(context.getColor(android.R.color.holo_red_dark));
                holder.actionButtons.setVisibility(View.GONE);
                break;
        }

        // Set customer details
        holder.customerName.setText("Customer: " + order.getCustomerName());
        holder.deliveryAddress.setText("Delivery to: " + order.getDeliveryAddress());

        // Set total amount
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        holder.totalAmount.setText("Total: " + format.format(order.getTotalAmount()));

        // Setup order items recycler view
        OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(order.getItems());
        holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
        holder.orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Setup action buttons
        holder.acceptButton.setOnClickListener(v -> updateOrderStatus(order.getId(), "ACCEPTED"));
        holder.rejectButton.setOnClickListener(v -> updateOrderStatus(order.getId(), "CANCELLED"));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    private void updateOrderStatus(String orderId, String status) {
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(restaurantId)
            .child("orders")
            .child(orderId)
            .child("status");
            
        orderRef.setValue(status);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, orderStatus, customerName, deliveryAddress, totalAmount;
        RecyclerView orderItemsRecyclerView;
        Button acceptButton, rejectButton;
        LinearLayout actionButtons;

        ViewHolder(View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            orderStatus = itemView.findViewById(R.id.orderStatus);
            customerName = itemView.findViewById(R.id.customerName);
            deliveryAddress = itemView.findViewById(R.id.deliveryAddress);
            totalAmount = itemView.findViewById(R.id.totalAmount);
            orderItemsRecyclerView = itemView.findViewById(R.id.orderItemsRecyclerView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            actionButtons = itemView.findViewById(R.id.actionButtons);
        }
    }
} 