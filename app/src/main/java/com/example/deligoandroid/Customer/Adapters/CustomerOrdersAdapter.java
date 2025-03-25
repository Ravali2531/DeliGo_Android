package com.example.deligoandroid.Customer.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerOrdersAdapter extends RecyclerView.Adapter<CustomerOrdersAdapter.ViewHolder> {
    private static final String TAG = "CustomerOrdersAdapter";
    private List<Order> orders;
    private NumberFormat currencyFormat;

    public CustomerOrdersAdapter(List<Order> orders) {
        this.orders = orders;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
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

        } catch (Exception e) {
            Log.e(TAG, "Error binding order", e);
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
        TextView orderNumber, orderStatus, restaurantName, deliveryType;
        TextView deliveryFee, totalAmount;
        RecyclerView orderItemsRecyclerView;

        ViewHolder(View view) {
            super(view);
            orderNumber = view.findViewById(R.id.orderNumber);
            orderStatus = view.findViewById(R.id.orderStatus);
            restaurantName = view.findViewById(R.id.restaurantName);
            deliveryType = view.findViewById(R.id.deliveryType);
            deliveryFee = view.findViewById(R.id.deliveryFee);
            totalAmount = view.findViewById(R.id.totalAmount);
            orderItemsRecyclerView = view.findViewById(R.id.orderItemsRecyclerView);
        }
    }
} 