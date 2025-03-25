package com.example.deligoandroid.Customer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.CustomerOrdersAdapter;
import com.example.deligoandroid.Models.Order;
import com.example.deligoandroid.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrdersFragment extends Fragment {
    private static final String TAG = "OrdersFragment";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CustomerOrdersAdapter currentOrdersAdapter;
    private CustomerOrdersAdapter pastOrdersAdapter;
    private List<Order> currentOrders;
    private List<Order> pastOrders;
    private String customerId;
    private RecyclerView currentOrdersRecyclerView;
    private RecyclerView pastOrdersRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_orders, container, false);
        
        // Initialize views
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        currentOrdersRecyclerView = view.findViewById(R.id.currentOrdersRecyclerView);
        pastOrdersRecyclerView = view.findViewById(R.id.pastOrdersRecyclerView);
        
        // Initialize lists and adapters
        currentOrders = new ArrayList<>();
        pastOrders = new ArrayList<>();
        currentOrdersAdapter = new CustomerOrdersAdapter(currentOrders, null);
        pastOrdersAdapter = new CustomerOrdersAdapter(pastOrders, this::handleReorder);
        
        // Get current user ID
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Set up ViewPager2 with adapters
        OrdersPagerAdapter pagerAdapter = new OrdersPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        
        // Set up TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Current Orders" : "Past Orders");
        }).attach();
        
        // Load orders
        loadOrders();
        
        return view;
    }

    private void loadOrders() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        ordersRef.orderByChild("customerId").equalTo(customerId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentOrders.clear();
                        pastOrders.clear();
                        
                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);
                                if (order != null) {
                                    order.setId(orderSnapshot.getKey());
                                    
                                    // Categorize order based on status
                                    String status = order.getStatus();
                                    if (status != null) {
                                        if (status.equals("delivered")) {
                                            pastOrders.add(order);
                                        } else {
                                            currentOrders.add(order);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing order", e);
                            }
                        }
                        
                        // Update adapters
                        currentOrdersAdapter.notifyDataSetChanged();
                        pastOrdersAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error loading orders", databaseError.toException());
                    }
                });
    }

    private void setupRecyclerViews() {
        // Set up current orders
        currentOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        currentOrdersAdapter = new CustomerOrdersAdapter(currentOrders, null);
        currentOrdersRecyclerView.setAdapter(currentOrdersAdapter);

        // Set up past orders with reorder functionality
        pastOrdersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        pastOrdersAdapter = new CustomerOrdersAdapter(pastOrders, this::handleReorder);
        pastOrdersRecyclerView.setAdapter(pastOrdersAdapter);
    }

    private void handleReorder(Order order) {
        // Create a new order with the same items
        Order newOrder = new Order();
        newOrder.setCustomerId(customerId);
        newOrder.setRestaurantId(order.getRestaurantId());
        newOrder.setItems(order.getItems());
        newOrder.setCustomizations(order.getCustomizations());
        newOrder.setDeliveryFee(order.getDeliveryFee());
        newOrder.setStatus("pending");
        newOrder.setTimestamp(System.currentTimeMillis());
        
        // Calculate total
        double total = 0.0;
        if (order.getItems() != null) {
            for (Map<String, Object> item : order.getItems()) {
                Number price = (Number) item.get("price");
                Number quantity = (Number) item.get("quantity");
                if (price != null && quantity != null) {
                    total += price.doubleValue() * quantity.doubleValue();
                }
            }
        }
        if (order.getDeliveryFee() != null) {
            total += order.getDeliveryFee();
        }
        newOrder.setTotal(total);

        // Save the new order to Firebase
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        String newOrderId = ordersRef.push().getKey();
        if (newOrderId != null) {
            ordersRef.child(newOrderId).setValue(newOrder)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to place order. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private class OrdersPagerAdapter extends RecyclerView.Adapter<OrdersPagerAdapter.OrdersViewHolder> {
        
        @NonNull
        @Override
        public OrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView recyclerView = new RecyclerView(parent.getContext());
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            return new OrdersViewHolder(recyclerView);
        }

        @Override
        public void onBindViewHolder(@NonNull OrdersViewHolder holder, int position) {
            holder.recyclerView.setAdapter(position == 0 ? currentOrdersAdapter : pastOrdersAdapter);
        }

        @Override
        public int getItemCount() {
            return 2; // Current and Past tabs
        }

        class OrdersViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;

            OrdersViewHolder(@NonNull View itemView) {
                super(itemView);
                recyclerView = (RecyclerView) itemView;
            }
        }
    }
} 