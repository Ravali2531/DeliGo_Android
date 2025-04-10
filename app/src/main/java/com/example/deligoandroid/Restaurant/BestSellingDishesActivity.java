package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BestSellingDishesActivity extends AppCompatActivity {
    private static final String TAG = "BestSellingDishes";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    
    private BestSellingDishesAdapter adapter;
    private String restaurantId;
    private DatabaseReference ordersRef;
    private DatabaseReference menuRef;
    
    private Map<String, Integer> menuItemCounts = new HashMap<>();
    private Map<String, MenuItem> menuItems = new HashMap<>();
    private List<BestSellingDish> bestSellingDishes = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_best_selling_dishes);
        
        setupToolbar();
        initViews();
        initFirebase();
        setupRecyclerView();
        fetchData();
    }
    
    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Best Selling Dishes");
        }
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.bestSellingDishesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
    }
    
    private void initFirebase() {
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        menuRef = FirebaseDatabase.getInstance().getReference("restaurants")
                .child(restaurantId)
                .child("menu_items");
    }
    
    private void setupRecyclerView() {
        adapter = new BestSellingDishesAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void fetchData() {
        showLoading(true);
        
        // First get all menu items
        menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot menuItemSnapshot : snapshot.getChildren()) {
                    try {
                        String menuItemId = menuItemSnapshot.getKey();
                        MenuItem menuItem = new MenuItem();
                        menuItem.setId(menuItemId);
                        menuItem.setName(menuItemSnapshot.child("name").getValue(String.class));
                        menuItem.setCategory(menuItemSnapshot.child("category").getValue(String.class));
                        menuItem.setImageURL(menuItemSnapshot.child("imageURL").getValue(String.class));
                        
                        // Get price as double
                        Object priceObj = menuItemSnapshot.child("price").getValue();
                        double price = 0.0;
                        if (priceObj instanceof Double) {
                            price = (Double) priceObj;
                        } else if (priceObj instanceof Long) {
                            price = ((Long) priceObj).doubleValue();
                        } else if (priceObj instanceof Integer) {
                            price = ((Integer) priceObj).doubleValue();
                        }
                        menuItem.setPrice(price);
                        
                        menuItems.put(menuItemId, menuItem);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing menu item", e);
                    }
                }
                
                // Now fetch orders
                fetchOrders();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                showLoading(false);
                showEmpty(true);
            }
        });
    }
    
    private void fetchOrders() {
        ordersRef.orderByChild("restaurantId").equalTo(restaurantId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        menuItemCounts.clear();
                        
                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                DataSnapshot itemsSnapshot = orderSnapshot.child("items");
                                for (DataSnapshot itemSnapshot : itemsSnapshot.getChildren()) {
                                    String menuItemId = itemSnapshot.child("menuItemId").getValue(String.class);
                                    if (menuItemId != null) {
                                        // Increment count for this menu item
                                        Integer currentCount = menuItemCounts.getOrDefault(menuItemId, 0);
                                        Integer quantity = itemSnapshot.child("quantity").getValue(Integer.class);
                                        if (quantity == null) quantity = 1;
                                        
                                        menuItemCounts.put(menuItemId, currentCount + quantity);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing order", e);
                            }
                        }
                        
                        // Create list of best selling dishes
                        createBestSellingList();
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                        showLoading(false);
                        showEmpty(true);
                    }
                });
    }
    
    private void createBestSellingList() {
        bestSellingDishes.clear();
        
        for (Map.Entry<String, Integer> entry : menuItemCounts.entrySet()) {
            String menuItemId = entry.getKey();
            int count = entry.getValue();
            
            MenuItem menuItem = menuItems.get(menuItemId);
            if (menuItem != null) {
                BestSellingDish dish = new BestSellingDish();
                dish.setMenuItem(menuItem);
                dish.setSalesCount(count);
                bestSellingDishes.add(dish);
            }
        }
        
        // Sort by count (highest first)
        Collections.sort(bestSellingDishes, (dish1, dish2) -> 
                Integer.compare(dish2.getSalesCount(), dish1.getSalesCount()));
        
        // Update UI
        showLoading(false);
        if (bestSellingDishes.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            adapter.setItems(bestSellingDishes);
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    // Model class for best selling dish
    public static class BestSellingDish {
        private MenuItem menuItem;
        private int salesCount;
        
        public MenuItem getMenuItem() {
            return menuItem;
        }
        
        public void setMenuItem(MenuItem menuItem) {
            this.menuItem = menuItem;
        }
        
        public int getSalesCount() {
            return salesCount;
        }
        
        public void setSalesCount(int salesCount) {
            this.salesCount = salesCount;
        }
    }
} 