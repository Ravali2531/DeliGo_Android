package com.example.deligoandroid.Restaurant;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivitySalesReportsBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SalesReportsActivity extends AppCompatActivity {
    private static final String TAG = "SalesReportsActivity";
    private ActivitySalesReportsBinding binding;
    private DatabaseReference ordersRef;
    private String currentUserId;
    private SimpleDateFormat dateFormat;
    private List<String> timeLabels;
    private List<Entry> revenueEntries;
    private int currentTimeFilter = 0; // 0 = daily, 1 = weekly, 2 = monthly

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupTabs();
        initializeFirebase();
        setupChart();
        loadDailyData(); // Load daily data by default
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.sales_reports);
        }
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Daily"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Weekly"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Monthly"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTimeFilter = tab.getPosition();
                switch (tab.getPosition()) {
                    case 0:
                        loadDailyData();
                        break;
                    case 1:
                        loadWeeklyData();
                        break;
                    case 2:
                        loadMonthlyData();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void initializeFirebase() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    private void setupChart() {
        binding.revenueChart.getDescription().setEnabled(false);
        binding.revenueChart.setTouchEnabled(true);
        binding.revenueChart.setDragEnabled(true);
        binding.revenueChart.setScaleEnabled(true);
        binding.revenueChart.setPinchZoom(true);
        binding.revenueChart.setDrawGridBackground(false);

        XAxis xAxis = binding.revenueChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
    }

    private void loadDailyData() {
        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        fetchOrders();
    }

    private void loadWeeklyData() {
        dateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        fetchOrders();
    }

    private void loadMonthlyData() {
        dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
        fetchOrders();
    }

    private boolean isOrderInTimeRange(long orderTimestamp) {
        Calendar orderCal = Calendar.getInstance();
        orderCal.setTimeInMillis(orderTimestamp);

        Calendar now = Calendar.getInstance();

        switch (currentTimeFilter) {
            case 0: // Daily - same day
                return orderCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        orderCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

            case 1: // Weekly - all orders
            case 2: // Monthly - all orders
                return true;

            default:
                return false;
        }
    }

    private void fetchOrders() {
        Query query = ordersRef
                .orderByChild("restaurantId")
                .equalTo(currentUserId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                processOrders(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void processOrders(DataSnapshot dataSnapshot) {
        int totalOrders = 0;
        double totalRevenue = 0;
        TreeMap<Long, Double> timeRevenueMap = new TreeMap<>();

        Log.d(TAG, "Total orders in snapshot: " + dataSnapshot.getChildrenCount());

        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
            String status = orderSnapshot.child("status").getValue(String.class);
            Long updatedAt = orderSnapshot.child("updatedAt").getValue(Long.class);
            Double total = orderSnapshot.child("total").getValue(Double.class);

            Log.d(TAG, "Order ID: " + orderSnapshot.getKey());
            Log.d(TAG, "Status: " + status);
            Log.d(TAG, "Total: " + total);
            Log.d(TAG, "UpdatedAt: " + updatedAt);

            if (status != null && status.equals("delivered") && updatedAt != null && total != null) {
                if (isOrderInTimeRange(updatedAt)) {
                    totalOrders++;
                    totalRevenue += total;
                    timeRevenueMap.merge(updatedAt, total, Double::sum);
                    Log.d(TAG, "Added order to map. Current total orders: " + totalOrders);
                }
            }
        }

        Log.d(TAG, "Final total orders: " + totalOrders);
        Log.d(TAG, "Final total revenue: " + totalRevenue);

        timeLabels = new ArrayList<>();
        revenueEntries = new ArrayList<>();
        int index = 0;

        for (Map.Entry<Long, Double> entry : timeRevenueMap.entrySet()) {
            String timeLabel = dateFormat.format(new Date(entry.getKey()));
            timeLabels.add(timeLabel);
            revenueEntries.add(new Entry(index++, entry.getValue().floatValue()));
            Log.d(TAG, "Added chart entry - Time: " + timeLabel + ", Revenue: " + entry.getValue());
        }

        updateUI(totalOrders, totalRevenue);
        updateChart();
    }

    private void updateUI(int totalOrders, double totalRevenue) {
        binding.totalOrdersText.setText(String.valueOf(totalOrders));
        binding.totalRevenueText.setText(String.format(Locale.getDefault(), "$%.2f", totalRevenue));

        double averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;
        binding.averageOrderValueText.setText(String.format(Locale.getDefault(), "$%.2f", averageOrderValue));
    }

    private void updateChart() {
        if (revenueEntries.isEmpty()) {
            binding.revenueChart.clear();
            binding.revenueChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(revenueEntries, "Revenue");
        dataSet.setColor(Color.parseColor("#6200EE"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#6200EE"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#6200EE"));
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        binding.revenueChart.setData(lineData);

        XAxis xAxis = binding.revenueChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        xAxis.setLabelRotationAngle(-45);
        xAxis.setLabelCount(Math.min(timeLabels.size(), 8), true);

        binding.revenueChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 