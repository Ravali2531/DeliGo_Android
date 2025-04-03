package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityOrderManagementBinding;
import com.example.deligoandroid.databinding.ItemOrderManagementBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderManagementActivity extends AppCompatActivity {
    private static final String TAG = "OrderManagementActivity";
    private ActivityOrderManagementBinding binding;
    private OrderAdapter adapter;
    private List<Order> orders;
    private DatabaseReference ordersRef, customersRef, restaurantsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        initializeFirebaseRefs();
        loadOrders();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Order Management");
        }
    }

    private void setupRecyclerView() {
        orders = new ArrayList<>();
        adapter = new OrderAdapter(orders);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void initializeFirebaseRefs() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        ordersRef = db.getReference("orders");
        customersRef = db.getReference("customers");
        restaurantsRef = db.getReference("restaurants");
    }

    private void loadOrders() {
        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orders.clear();
                for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setOrderId(orderSnapshot.getKey());
                        fetchCustomerAndRestaurantNames(order);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });
    }

    private void fetchCustomerAndRestaurantNames(Order order) {
        // Fetch customer name
        customersRef.child(order.getCustomerId()).child("fullName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String customerName = snapshot.getValue(String.class);
                        if (customerName != null) {
                            order.setCustomerName(customerName);
                            // After getting customer name, fetch restaurant name
                            fetchRestaurantName(order);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching customer name: " + error.getMessage());
                    }
                });
    }

    private void fetchRestaurantName(Order order) {
        restaurantsRef.child(order.getRestaurantId()).child("store_info").child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String restaurantName = snapshot.getValue(String.class);
                        if (restaurantName != null) {
                            order.setRestaurantName(restaurantName);
                            // Add order to list only after both names are fetched
                            if (!orders.contains(order)) {
                                orders.add(order);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching restaurant name: " + error.getMessage());
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private final List<Order> orders;

        public OrderAdapter(List<Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemOrderManagementBinding binding = ItemOrderManagementBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new OrderViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orders.get(position);
            holder.bind(order);
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        static class OrderViewHolder extends RecyclerView.ViewHolder {
            private final ItemOrderManagementBinding binding;

            public OrderViewHolder(ItemOrderManagementBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Order order) {
                binding.orderId.setText("Order #" + order.getOrderId());
                binding.restaurantName.setText("Restaurant: " + order.getRestaurantName());
                binding.customerName.setText("Customer: " + order.getCustomerName());
                binding.driverName.setText("Driver: " + (order.getDriverName() != null ? order.getDriverName() : "Not Assigned"));
                
                // Set items
                StringBuilder items = new StringBuilder();
                for (OrderItem item : order.getItems()) {
                    items.append(item.getQuantity()).append("x ").append(item.getName())
                         .append(" $").append(String.format(Locale.US, "%.2f", item.getPrice()))
                         .append("\n");
                    if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isEmpty()) {
                        items.append("Special Instructions: ").append(item.getSpecialInstructions()).append("\n");
                    }
                }
                binding.itemsList.setText(items.toString().trim());
                
                binding.totalAmount.setText(String.format(Locale.US, "Total: $%.2f", order.getTotal()));
                
                // Format and set date
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US);
                binding.orderDate.setText(sdf.format(new Date(order.getTimestamp())));
                
                // Set status
                binding.orderStatus.setText(order.getStatus());
                int statusColor;
                switch (order.getStatus().toLowerCase()) {
                    case "delivered":
                        statusColor = R.color.green;
                        break;
                    case "ready for pickup":
                        statusColor = R.color.purple;
                        break;
                    default:
                        statusColor = R.color.gray_600;
                        break;
                }
                binding.orderStatus.setBackgroundResource(statusColor);
            }
        }
    }

    public static class Order {
        private String orderId;
        private String restaurantId;
        private String customerId;
        private String restaurantName;
        private String customerName;
        private String driverName;
        private List<OrderItem> items;
        private double total;
        private long timestamp;
        private String status;

        // Required empty constructor for Firebase
        public Order() {}

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getRestaurantId() { return restaurantId; }
        public String getCustomerId() { return customerId; }
        public String getRestaurantName() { return restaurantName; }
        public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getDriverName() { return driverName; }
        public List<OrderItem> getItems() { return items; }
        public double getTotal() { return total; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
    }

    public static class OrderItem {
        private int quantity;
        private String name;
        private double price;
        private String specialInstructions;

        // Required empty constructor for Firebase
        public OrderItem() {}

        // Getters
        public int getQuantity() { return quantity; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getSpecialInstructions() { return specialInstructions; }
    }
} 