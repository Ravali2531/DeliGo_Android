package com.example.deligoandroid.Customer.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Adapters.MenuAdapter;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.Customer.Models.Restaurant;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RestaurantMenuActivity extends AppCompatActivity implements MenuAdapter.OnAddToCartListener {
    private Restaurant restaurant;
    private RecyclerView menuRecyclerView;
    private MenuAdapter menuAdapter;
    private List<MenuItem> allMenuItems = new ArrayList<>();
    private View emptyState, searchEmptyState;
    private DatabaseReference menuRef;
    private SearchView searchView;
    private DatabaseReference cartRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_menu);

        try {
            Log.d("RestaurantMenuActivity", "Starting onCreate...");

            // Get restaurant data from intent
            if (!getIntent().hasExtra("restaurant_data")) {
                Log.e("RestaurantMenuActivity", "No restaurant data in intent");
                showError("Error: Restaurant data not found");
                return;
            }

            HashMap<String, Object> restaurantData = (HashMap<String, Object>) getIntent().getSerializableExtra("restaurant_data");
            if (restaurantData == null) {
                Log.e("RestaurantMenuActivity", "Restaurant data is null");
                showError("Error: Invalid restaurant data");
                return;
            }

            // Create Restaurant object from HashMap
            restaurant = new Restaurant();
            restaurant.setId((String) restaurantData.get("id"));
            restaurant.setName((String) restaurantData.get("name"));
            restaurant.setEmail((String) restaurantData.get("email"));
            restaurant.setDescription((String) restaurantData.get("description"));
            restaurant.setOpen((Boolean) restaurantData.get("isOpen"));

            Log.d("RestaurantMenuActivity", "Created Restaurant object from data");
            Log.d("RestaurantMenuActivity", "Restaurant ID: " + restaurant.getId());
            Log.d("RestaurantMenuActivity", "Restaurant Name: " + restaurant.getName());

            // Initialize cart reference
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Log.e("RestaurantMenuActivity", "User is not logged in");
                showError("Error: Please log in to view menu");
                return;
            }

            String userId = auth.getCurrentUser().getUid();
            cartRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(userId)
                .child("cart");

            setupViews();
            loadMenuItems();

        } catch (Exception e) {
            Log.e("RestaurantMenuActivity", "Error in onCreate", e);
            showError("Error loading restaurant: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Log.e("RestaurantMenuActivity", message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void setupViews() {
        try {
            // Setup back button
            findViewById(R.id.backButton).setOnClickListener(v -> onBackPressed());

            // Setup restaurant info
            ImageView restaurantImage = findViewById(R.id.restaurantImage);
            TextView restaurantName = findViewById(R.id.restaurantName);
            TextView cuisineType = findViewById(R.id.cuisineType);
            TextView rating = findViewById(R.id.rating);
            TextView numberOfRatings = findViewById(R.id.numberOfRatings);
            TextView priceRange = findViewById(R.id.priceRange);

            // Load restaurant image
            String imageUrl = restaurant.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(restaurantImage);
            } else {
                restaurantImage.setImageResource(R.drawable.ic_food_placeholder);
            }

            // Set restaurant details with null checks
            restaurantName.setText(restaurant.getName());
            cuisineType.setText(restaurant.getCuisine());
            rating.setText(String.format("%.1f", restaurant.getRating()));
            numberOfRatings.setText(String.format("(%d)", restaurant.getNumberOfRatings()));
            priceRange.setText(restaurant.getPriceRange());

            // Setup RecyclerView
            menuRecyclerView = findViewById(R.id.menuRecyclerView);
            menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            menuAdapter = new MenuAdapter(this);
            menuAdapter.setOnAddToCartListener(this);
            menuRecyclerView.setAdapter(menuAdapter);

            // Setup empty states
            emptyState = findViewById(R.id.emptyState);
            searchEmptyState = findViewById(R.id.searchEmptyState);

            // Setup SearchView
            searchView = findViewById(R.id.searchView);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterMenuItems(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterMenuItems(newText);
                    return true;
                }
            });

            // Initialize Firebase reference for menu items
            menuRef = FirebaseDatabase.getInstance().getReference()
                .child("restaurants")
                .child(restaurant.getId())
                .child("menu_items");

            Log.d("RestaurantMenuActivity", "Views setup completed successfully");

        } catch (Exception e) {
            Log.e("RestaurantMenuActivity", "Error in setupViews", e);
            showError("Error setting up restaurant view: " + e.getMessage());
        }
    }

    private void loadMenuItems() {
        try {
            // Show loading state
            View loadingView = findViewById(R.id.loadingView);
            loadingView.setVisibility(View.VISIBLE);
            menuRecyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);

            menuRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        List<MenuItem> items = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            MenuItem item = snapshot.getValue(MenuItem.class);
                            if (item != null && item.getId() != null) {
                                item.setId(snapshot.getKey());
                                items.add(item);
                            }
                        }

                        loadingView.setVisibility(View.GONE);
                        if (items.isEmpty()) {
                            menuRecyclerView.setVisibility(View.GONE);
                            emptyState.setVisibility(View.VISIBLE);
                        } else {
                            menuRecyclerView.setVisibility(View.VISIBLE);
                            emptyState.setVisibility(View.GONE);
                            // Group items by category
                            Map<String, List<MenuItem>> groupedItems = new TreeMap<>();
                            for (MenuItem item : items) {
                                String category = item.getCategory() != null ? item.getCategory() : "Uncategorized";
                                if (!groupedItems.containsKey(category)) {
                                    groupedItems.put(category, new ArrayList<>());
                                }
                                groupedItems.get(category).add(item);
                            }
                            menuAdapter.setMenuItems(groupedItems);
                        }

                    } catch (Exception e) {
                        Log.e("RestaurantMenuActivity", "Error processing menu items", e);
                        showError("Error loading menu items: " + e.getMessage());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("RestaurantMenuActivity", "Database error: " + databaseError.getMessage());
                    loadingView.setVisibility(View.GONE);
                    showError("Failed to load menu: " + databaseError.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e("RestaurantMenuActivity", "Error in loadMenuItems", e);
            showError("Error loading menu: " + e.getMessage());
        }
    }

    private void filterMenuItems(String query) {
        if (query == null || query.isEmpty()) {
            updateUI(allMenuItems);
            return;
        }

        List<MenuItem> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (MenuItem item : allMenuItems) {
            if (item.getName().toLowerCase().contains(lowerCaseQuery) ||
                item.getDescription().toLowerCase().contains(lowerCaseQuery) ||
                item.getCategory().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(item);
            }
        }

        updateUI(filteredList);
    }

    private void updateUI(List<MenuItem> items) {
        if (items.isEmpty()) {
            menuRecyclerView.setVisibility(View.GONE);
            if (searchView.getQuery().length() > 0) {
                searchEmptyState.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.VISIBLE);
                searchEmptyState.setVisibility(View.GONE);
            }
        } else {
            menuRecyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            searchEmptyState.setVisibility(View.GONE);

            // Group items by category
            Map<String, List<MenuItem>> groupedItems = new TreeMap<>();
            for (MenuItem item : items) {
                groupedItems.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
            }

            menuAdapter.setMenuItems(groupedItems);
        }
    }

    @Override
    public void onAddToCart(CartItem cartItem) {
        try {
            // Set the restaurant ID before adding to cart
            cartItem.setRestaurantId(restaurant.getId());
            
            cartRef.child(cartItem.getId()).setValue(cartItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item added to cart", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("RestaurantMenuActivity", "Error adding item to cart", e);
                    Toast.makeText(this, "Failed to add item to cart: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        } catch (Exception e) {
            Log.e("RestaurantMenuActivity", "Error in onAddToCart", e);
            Toast.makeText(this, "Error adding to cart: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }
} 