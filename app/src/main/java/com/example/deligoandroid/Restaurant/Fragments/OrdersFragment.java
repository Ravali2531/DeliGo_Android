package com.example.deligoandroid.Restaurant.Fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Adapters.OrdersAdapter;
import com.example.deligoandroid.Restaurant.Models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {
    private RecyclerView ordersRecyclerView;
    private LinearLayout noNewOrdersLayout, noInProgressOrdersLayout, noDeliveredOrdersLayout;
    private Button newOrdersTab, inProgressTab, deliveredTab;
    private OrdersAdapter ordersAdapter;
    private DatabaseReference databaseRef;
    private String userId;
    private String currentOrderStatus = "NEW";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);
        
        initializeViews(view);
        setupClickListeners();
        loadOrders();
        
        return view;
    }

    private void initializeViews(View view) {
        ordersRecyclerView = view.findViewById(R.id.ordersRecyclerView);
        noNewOrdersLayout = view.findViewById(R.id.noNewOrdersLayout);
        noInProgressOrdersLayout = view.findViewById(R.id.noInProgressOrdersLayout);
        noDeliveredOrdersLayout = view.findViewById(R.id.noDeliveredOrdersLayout);
        newOrdersTab = view.findViewById(R.id.newOrdersTab);
        inProgressTab = view.findViewById(R.id.inProgressTab);
        deliveredTab = view.findViewById(R.id.deliveredTab);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersAdapter = new OrdersAdapter(userId);
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    private void setupClickListeners() {
        newOrdersTab.setOnClickListener(v -> showNewOrders());
        inProgressTab.setOnClickListener(v -> showInProgressOrders());
        deliveredTab.setOnClickListener(v -> showDeliveredOrders());
    }

    private void showNewOrders() {
        currentOrderStatus = "NEW";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void showInProgressOrders() {
        currentOrderStatus = "IN_PROGRESS";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void showDeliveredOrders() {
        currentOrderStatus = "DELIVERED";
        updateOrderStatusTabs();
        loadOrders();
    }

    private void updateOrderStatusTabs() {
        newOrdersTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        inProgressTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        deliveredTab.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));

        switch (currentOrderStatus) {
            case "NEW":
                newOrdersTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
            case "IN_PROGRESS":
                inProgressTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
            case "DELIVERED":
                deliveredTab.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.orange)));
                break;
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        ordersRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        noNewOrdersLayout.setVisibility(currentOrderStatus.equals("NEW") && isEmpty ? View.VISIBLE : View.GONE);
        noInProgressOrdersLayout.setVisibility(currentOrderStatus.equals("IN_PROGRESS") && isEmpty ? View.VISIBLE : View.GONE);
        noDeliveredOrdersLayout.setVisibility(currentOrderStatus.equals("DELIVERED") && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void loadOrders() {
        databaseRef.child("restaurants").child(userId).child("orders")
            .orderByChild("status").equalTo(currentOrderStatus)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Order> orders = new ArrayList<>();
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        Order order = orderSnapshot.getValue(Order.class);
                        if (order != null) {
                            order.setId(orderSnapshot.getKey());
                            orders.add(order);
                        }
                    }

                    updateEmptyState(orders.isEmpty());
                    if (!orders.isEmpty()) {
                        ordersAdapter.setOrders(orders);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(),
                        "Error loading orders: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
} 