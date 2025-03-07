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
import com.example.deligoandroid.Customer.Models.CustomizationOption;
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
    private MenuAdapter menuAdapter;
    private DatabaseReference menuRef;
    private DatabaseReference cartRef;
    private ValueEventListener menuListener;
    private View loadingLayout;
    private View contentLayout;
    private RecyclerView menuRecyclerView;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_menu);

        // Get restaurant data from intent
        @SuppressWarnings("unchecked")
        HashMap<String, Object> restaurantData = (HashMap<String, Object>) getIntent().getSerializableExtra("restaurant_data");
        if (restaurantData == null) {
            Log.e("RestaurantMenuActivity", "Restaurant data is null");
            Toast.makeText(this, "Error loading restaurant data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Log received data
        Log.d("RestaurantMenuActivity", "Received restaurant data: " + restaurantData.toString());

        // Create Restaurant object from HashMap
        restaurant = new Restaurant();
        restaurant.setId((String) restaurantData.get("id"));
        restaurant.setName((String) restaurantData.get("name"));
        restaurant.setEmail((String) restaurantData.get("email"));
        restaurant.setDescription((String) restaurantData.get("description"));
        restaurant.setOpen((Boolean) restaurantData.get("isOpen"));
        restaurant.setCuisine((String) restaurantData.get("cuisine"));
        restaurant.setPriceRange((String) restaurantData.get("priceRange"));
        restaurant.setRating(((Number) restaurantData.get("rating")).doubleValue());
        restaurant.setNumberOfRatings(((Number) restaurantData.get("numberOfRatings")).intValue());
        restaurant.setAddress((String) restaurantData.get("address"));
        restaurant.setImageURL((String) restaurantData.get("imageURL"));

        // Log restaurant object
        Log.d("RestaurantMenuActivity", "Created restaurant object with ID: " + restaurant.getId());
        Log.d("RestaurantMenuActivity", "Restaurant name: " + restaurant.getName());

        // Initialize views
        initializeViews();
        setupFirebaseReferences();
        loadMenuItems();
    }

    private void initializeViews() {
        loadingLayout = findViewById(R.id.loadingLayout);
        contentLayout = findViewById(R.id.contentLayout);
        menuRecyclerView = findViewById(R.id.menuRecyclerView);
        searchView = findViewById(R.id.searchView);

        // Setup header
        TextView restaurantName = findViewById(R.id.restaurantName);
        TextView restaurantDescription = findViewById(R.id.restaurantDescription);
        ImageView restaurantImage = findViewById(R.id.restaurantImage);

        restaurantName.setText(restaurant.getName());
        restaurantDescription.setText(restaurant.getDescription());

        if (restaurant.getImageURL() != null && !restaurant.getImageURL().isEmpty()) {
            Glide.with(this)
                .load(restaurant.getImageURL())
                .placeholder(R.drawable.ic_restaurant_placeholder)
                .error(R.drawable.ic_restaurant_placeholder)
                .into(restaurantImage);
        }

        // Setup RecyclerView
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuAdapter = new MenuAdapter(this, restaurant.getId());
        menuAdapter.setOnAddToCartListener(this);
        menuRecyclerView.setAdapter(menuAdapter);

        // Setup search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: Implement search functionality
                return true;
            }
        });
    }

    private void setupFirebaseReferences() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String restaurantId = restaurant.getId();
        
        Log.d("RestaurantMenuActivity", "Setting up Firebase references");
        Log.d("RestaurantMenuActivity", "Restaurant ID: " + restaurantId);
        Log.d("RestaurantMenuActivity", "User ID: " + userId);

        // Verify restaurant ID
        if (restaurantId == null || restaurantId.isEmpty()) {
            Log.e("RestaurantMenuActivity", "Invalid restaurant ID");
            Toast.makeText(this, "Error: Invalid restaurant ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // First check the restaurant root node
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(restaurantId);

        restaurantRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("RestaurantMenuActivity", "Restaurant reference does not exist");
                    return;
                }
                
                Log.d("RestaurantMenuActivity", "Restaurant data exists. Available children: ");
                for (DataSnapshot child : snapshot.getChildren()) {
                    Log.d("RestaurantMenuActivity", "Child key: " + child.getKey());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RestaurantMenuActivity", "Error checking restaurant reference: " + error.getMessage());
            }
        });

        // Setup menu reference
        menuRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(restaurantId)
            .child("menu_items");

        // Log the full path
        Log.d("RestaurantMenuActivity", "Menu reference path: " + menuRef.toString());

        // Test if the reference exists
        menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("RestaurantMenuActivity", "Menu reference does not exist");
                    Toast.makeText(RestaurantMenuActivity.this, 
                        "No menu found for this restaurant", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("RestaurantMenuActivity", "Menu reference exists with " + 
                        snapshot.getChildrenCount() + " items");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RestaurantMenuActivity", "Error checking menu reference: " + error.getMessage());
            }
        });

        // Setup cart reference
        cartRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("cart");
    }

    private void loadMenuItems() {
        showLoading(true);
        Log.d("RestaurantMenuActivity", "Starting to load menu items for restaurant: " + restaurant.getId());

        menuListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("RestaurantMenuActivity", "Menu data received. Number of items: " + snapshot.getChildrenCount());
                Map<String, List<MenuItem>> groupedItems = new TreeMap<>();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    try {
                        String itemId = itemSnapshot.getKey();
                        Log.d("RestaurantMenuActivity", "Processing menu item with ID: " + itemId);

                        // Validate required fields
                        String name = itemSnapshot.child("name").getValue(String.class);
                        Double price = itemSnapshot.child("price").getValue(Double.class);
                        String category = itemSnapshot.child("category").getValue(String.class);

                        if (name == null || price == null) {
                            Log.e("RestaurantMenuActivity", "Required field missing for item: " + itemId);
                            continue;
                        }

                        MenuItem menuItem = new MenuItem();
                        menuItem.setId(itemId);
                        menuItem.setName(name);
                        menuItem.setDescription(itemSnapshot.child("description").getValue(String.class));
                        menuItem.setPrice(price);
                        menuItem.setImageURL(itemSnapshot.child("imageURL").getValue(String.class));
                        menuItem.setCategory(category != null ? category : "Other");
                        
                        Boolean isAvailable = itemSnapshot.child("isAvailable").getValue(Boolean.class);
                        menuItem.setAvailable(isAvailable != null ? isAvailable : true);

                        // Load customization options
                        DataSnapshot customizationSnapshot = itemSnapshot.child("customizationOptions");
                        if (customizationSnapshot.exists() && customizationSnapshot.getChildrenCount() > 0) {
                            Log.d("RestaurantMenuActivity", "Found customization options for item: " + name);
                            menuItem.setHasCustomizations(true);
                            List<CustomizationOption> options = new ArrayList<>();
                            
                            for (DataSnapshot optionSnapshot : customizationSnapshot.getChildren()) {
                                try {
                                    CustomizationOption option = optionSnapshot.getValue(CustomizationOption.class);
                                    if (option != null) {
                                        options.add(option);
                                        Log.d("RestaurantMenuActivity", "Added customization option: " + option.getName());
                                    }
                                } catch (Exception e) {
                                    Log.e("RestaurantMenuActivity", "Error processing customization option", e);
                                }
                            }
                            menuItem.setCustomizationOptions(options);
                        }

                        // Group by category
                        String itemCategory = menuItem.getCategory();
                        if (!groupedItems.containsKey(itemCategory)) {
                            groupedItems.put(itemCategory, new ArrayList<>());
                        }
                        groupedItems.get(itemCategory).add(menuItem);
                        Log.d("RestaurantMenuActivity", "Added item: " + name + " to category: " + itemCategory);

                    } catch (Exception e) {
                        Log.e("RestaurantMenuActivity", "Error processing menu item: " + itemSnapshot.getKey(), e);
                    }
                }

                // Log the results
                Log.d("RestaurantMenuActivity", "Finished processing menu items. Categories found: " + groupedItems.size());
                for (Map.Entry<String, List<MenuItem>> entry : groupedItems.entrySet()) {
                    Log.d("RestaurantMenuActivity", "Category: " + entry.getKey() + ", Items: " + entry.getValue().size());
                }

                menuAdapter.setMenuItems(groupedItems);
                showLoading(false);

                // Show empty state if no items were loaded
                View emptyState = findViewById(R.id.emptyState);
                if (groupedItems.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    menuRecyclerView.setVisibility(View.GONE);
                    Log.d("RestaurantMenuActivity", "No menu items found, showing empty state");
                } else {
                    emptyState.setVisibility(View.GONE);
                    menuRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("RestaurantMenuActivity", "Error loading menu items", error.toException());
                Toast.makeText(RestaurantMenuActivity.this, "Error loading menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                showLoading(false);
                
                // Show empty state on error
                View emptyState = findViewById(R.id.emptyState);
                emptyState.setVisibility(View.VISIBLE);
                menuRecyclerView.setVisibility(View.GONE);
            }
        };

        menuRef.addValueEventListener(menuListener);
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (menuListener != null) {
            menuRef.removeEventListener(menuListener);
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