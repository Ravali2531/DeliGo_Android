package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Adapters.OrdersAdapter;
import com.example.deligoandroid.Restaurant.Models.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ScheduledOrdersActivity extends AppCompatActivity {
    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView noOrdersText;
    private List<Order> ordersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduled_orders);

        initializeViews();
        setupRecyclerView();
        loadScheduledOrders();

        swipeRefreshLayout.setOnRefreshListener(this::loadScheduledOrders);
    }

    private void initializeViews() {
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        noOrdersText = findViewById(R.id.noOrdersText);
        
        // Set up toolbar
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        ordersList = new ArrayList<>();
        String restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersAdapter = new OrdersAdapter(restaurantId);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    private void loadScheduledOrders() {
        String restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("scheduled_orders");
        Query restaurantOrders = ordersRef.orderByChild("restaurantId").equalTo(restaurantId);

        restaurantOrders.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ordersList.clear();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Order order = orderSnapshot.getValue(Order.class);
                    if (order != null) {
                        order.setId(orderSnapshot.getKey());
                        ordersList.add(order);
                    }
                }

                if (ordersList.isEmpty()) {
                    noOrdersText.setVisibility(View.VISIBLE);
                    ordersRecyclerView.setVisibility(View.GONE);
                } else {
                    noOrdersText.setVisibility(View.GONE);
                    ordersRecyclerView.setVisibility(View.VISIBLE);
                }

                ordersAdapter.setOrders(ordersList);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ScheduledOrdersActivity.this, 
                    "Error loading orders: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
} 