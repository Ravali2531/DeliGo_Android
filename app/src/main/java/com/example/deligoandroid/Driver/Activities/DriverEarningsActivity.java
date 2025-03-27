package com.example.deligoandroid.Driver.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;

import com.example.deligoandroid.Driver.Adapters.DriverEarningsAdapter;
import com.example.deligoandroid.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class DriverEarningsActivity extends AppCompatActivity {
    private static final String TAG = "DriverEarningsActivity";
    private static final int TAB_ALL = 0;
    private static final int TAB_DAILY = 1;
    private static final int TAB_WEEKLY = 2;
    private static final int TAB_MONTHLY = 3;

    private TextView periodText;
    private TextView totalEarnings;
    private TextView deliveryFeesTotal;
    private TextView tipsTotal;
    private TextView deliveriesCount;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private DriverEarningsAdapter adapter;
    private DatabaseReference ordersRef;
    private String currentUserId;
    private SimpleDateFormat dateFormat;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_earnings);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        // Initialize views
        initializeViews();
        setupToolbar();
        setupTabLayout();
        setupRecyclerView();

        // Initialize date handling
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        calendar = Calendar.getInstance();

        // Load initial data (All by default)
        loadEarningsData(TAB_ALL);
    }

    private void initializeViews() {
        periodText = findViewById(R.id.periodText);
        totalEarnings = findViewById(R.id.totalEarnings);
        deliveryFeesTotal = findViewById(R.id.deliveryFeesTotal);
        tipsTotal = findViewById(R.id.tipsTotal);
        deliveriesCount = findViewById(R.id.deliveriesCount);
        recyclerView = findViewById(R.id.earningsRecyclerView);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Set status bar color to white with dark icons
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadEarningsData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DriverEarningsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    private void loadEarningsData(int tabPosition) {
        // For "All" tab, we don't need time constraints
        if (tabPosition == TAB_ALL) {
            loadAllEarnings();
            return;
        }

        long startTime = getStartTime(tabPosition);
        long endTime = System.currentTimeMillis();

        Log.d(TAG, "Loading earnings data from " + new Date(startTime) + " to " + new Date(endTime));

        // Query orders by driverId
        Query query = ordersRef.orderByChild("driverId").equalTo(currentUserId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                processEarningsData(dataSnapshot, startTime, endTime, tabPosition);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading earnings data", databaseError.toException());
                Toast.makeText(DriverEarningsActivity.this, 
                    "Failed to load earnings data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllEarnings() {
        Query query = ordersRef.orderByChild("driverId").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                processEarningsData(dataSnapshot, 0, Long.MAX_VALUE, TAB_ALL);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading all earnings data", databaseError.toException());
                Toast.makeText(DriverEarningsActivity.this, 
                    "Failed to load earnings data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processEarningsData(DataSnapshot dataSnapshot, long startTime, long endTime, int tabPosition) {
        List<Map<String, Object>> earningsList = new ArrayList<>();
        double totalDeliveryFees = 0;
        double totalTips = 0;
        int totalDeliveries = 0;

        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
            try {
                String status = orderSnapshot.child("status").getValue(String.class);
                Long updatedAt = orderSnapshot.child("updatedAt").getValue(Long.class);
                
                // Skip if not delivered or updatedAt is outside our time range
                if (!"delivered".equals(status) || updatedAt == null || 
                    updatedAt < startTime || updatedAt > endTime) {
                    continue;
                }
                
                Log.d(TAG, "Processing delivered order: " + orderSnapshot.getKey() + 
                      " | Updated at: " + new Date(updatedAt));

                Map<String, Object> earning = new HashMap<>();
                
                // Get order details
                earning.put("orderId", orderSnapshot.getKey());
                earning.put("timestamp", updatedAt);
                earning.put("restaurantName", orderSnapshot.child("restaurantName").getValue(String.class));
                earning.put("deliveryAddress", orderSnapshot.child("deliveryAddress").getValue(String.class));
                
                // Get earnings details with proper type handling
                double deliveryFee = getDoubleValue(orderSnapshot.child("deliveryFee").getValue());
                double tipAmount = getDoubleValue(orderSnapshot.child("tipAmount").getValue());
                
                Log.d(TAG, "Order " + orderSnapshot.getKey() + 
                      " | Delivery Fee: " + deliveryFee + 
                      " | Tip: " + tipAmount);
                
                earning.put("deliveryFee", deliveryFee);
                earning.put("tipAmount", tipAmount);
                earning.put("total", deliveryFee + tipAmount);
                
                earningsList.add(earning);
                
                // Update totals
                totalDeliveryFees += deliveryFee;
                totalTips += tipAmount;
                totalDeliveries++;
            } catch (Exception e) {
                Log.e(TAG, "Error processing order " + orderSnapshot.getKey(), e);
            }
        }

        Log.d(TAG, "Found " + totalDeliveries + " deliveries" +
              " | Total Fees: " + totalDeliveryFees +
              " | Total Tips: " + totalTips);

        // Update UI
        updateUI(earningsList, totalDeliveryFees, totalTips, totalDeliveries, tabPosition);
    }

    private long getStartTime(int tabPosition) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (tabPosition) {
            case TAB_DAILY:
                // Start of today
                break;
            case TAB_WEEKLY:
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                break;
            case TAB_MONTHLY:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }

        return cal.getTimeInMillis();
    }

    private void updateUI(List<Map<String, Object>> earningsList, double totalDeliveryFees,
                         double totalTips, int totalDeliveries, int tabPosition) {
        // Update period text
        String period = getPeriodText(tabPosition);
        periodText.setText(period);

        // Update summary
        totalEarnings.setText(String.format(Locale.getDefault(), "$%.2f", totalDeliveryFees + totalTips));
        deliveryFeesTotal.setText(String.format(Locale.getDefault(), "$%.2f", totalDeliveryFees));
        tipsTotal.setText(String.format(Locale.getDefault(), "$%.2f", totalTips));
        deliveriesCount.setText(String.valueOf(totalDeliveries));

        // Update list
        adapter.updateData(earningsList);
    }

    private String getPeriodText(int tabPosition) {
        switch (tabPosition) {
            case TAB_ALL:
                return "All Time";
            case TAB_DAILY:
                return "Today, " + dateFormat.format(new Date());
            case TAB_WEEKLY:
                return "This Week";
            case TAB_MONTHLY:
                return "This Month";
            default:
                return "";
        }
    }

    private double getDoubleValue(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return 0.0;
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