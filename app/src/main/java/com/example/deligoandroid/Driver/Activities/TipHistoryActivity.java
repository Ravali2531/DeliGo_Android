package com.example.deligoandroid.Driver.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Driver.Adapters.TipHistoryAdapter;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TipHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView totalTipsAmount;
    private TipHistoryAdapter adapter;
    private DatabaseReference ordersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_tip_history);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        }
        
        // Set status bar color to white with dark icons
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Initialize views
        recyclerView = findViewById(R.id.tipHistoryRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        totalTipsAmount = findViewById(R.id.totalTipsAmount);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TipHistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Load tip history
        loadTipHistory();
    }

    private void loadTipHistory() {
        Query query = ordersRef.orderByChild("driverId").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Map<String, Object>> tipHistory = new ArrayList<>();
                double totalTips = 0.0;

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String status = orderSnapshot.child("status").getValue(String.class);
                    if ("delivered".equals(status)) {
                        Map<String, Object> delivery = new HashMap<>();
                        
                        // Get order details
                        delivery.put("orderId", orderSnapshot.getKey());
                        delivery.put("timestamp", orderSnapshot.child("deliveryTime").getValue(Long.class));
                        delivery.put("restaurantName", orderSnapshot.child("restaurantName").getValue(String.class));
                        delivery.put("deliveryAddress", orderSnapshot.child("deliveryAddress").getValue(String.class));
                        
                        // Get tip amount
                        Object tipObj = orderSnapshot.child("tipAmount").getValue();
                        double tipAmount = 0.0;
                        if (tipObj instanceof Double) {
                            tipAmount = (Double) tipObj;
                        } else if (tipObj instanceof Long) {
                            tipAmount = ((Long) tipObj).doubleValue();
                        }
                        
                        delivery.put("tipAmount", tipAmount);
                        tipHistory.add(delivery);
                        totalTips += tipAmount;
                    }
                }

                // Update UI
                if (tipHistory.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.updateData(tipHistory);
                }

                // Update total tips amount
                totalTipsAmount.setText(String.format(Locale.getDefault(), "$%.2f", totalTips));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 