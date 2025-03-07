package com.example.deligoandroid.Customer.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.MenuAdapter;
import com.example.deligoandroid.Customer.Adapters.RestaurantAdapter;
import com.example.deligoandroid.Customer.Fragments.AccountFragment;
import com.example.deligoandroid.Customer.Fragments.CartFragment;
import com.example.deligoandroid.Customer.Fragments.FavoritesFragment;
import com.example.deligoandroid.Customer.Fragments.OrdersFragment;
import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.Customer.Models.Restaurant;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityCustomerHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.database.ServerValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomerHomeActivity extends AppCompatActivity 
    implements RestaurantAdapter.OnRestaurantClickListener,
               MenuAdapter.OnFavoriteClickListener {
    
    private ActivityCustomerHomeBinding binding;
    private DatabaseReference restaurantsRef;
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter menuAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
        setupBottomNavigation();
        loadRestaurants();
    }

    private void setupViews() {
        // Setup RecyclerView
        binding.restaurantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        restaurantAdapter = new RestaurantAdapter(this);
        restaurantAdapter.setOnRestaurantClickListener(this);
        binding.restaurantsRecyclerView.setAdapter(restaurantAdapter);

        // Initialize MenuAdapter
        menuAdapter = new MenuAdapter(this, "all");
        menuAdapter.setOnFavoriteClickListener(this);
        loadFavoriteItemIds(menuAdapter);

        // Setup SearchView
        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterRestaurants(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRestaurants(newText);
                return true;
            }
        });

        // Initialize Firebase Reference
        restaurantsRef = FirebaseDatabase.getInstance().getReference().child("restaurants");
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showMainContent();
                return true;
            } else if (itemId == R.id.nav_favorites) {
                showFragment(new FavoritesFragment());
                return true;
            } else if (itemId == R.id.nav_cart) {
                showFragment(new CartFragment());
                return true;
            } else if (itemId == R.id.nav_orders) {
                showFragment(new OrdersFragment());
                return true;
            } else if (itemId == R.id.nav_account) {
                showFragment(new AccountFragment());
                return true;
            }
            return false;
        });
    }

    private void showMainContent() {
        binding.fragmentContainer.setVisibility(View.GONE);
        binding.mainContent.setVisibility(View.VISIBLE);
    }

    private void showFragment(Fragment fragment) {
        binding.mainContent.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    private void loadRestaurants() {
        // Show loading state
        binding.loadingProgressBar.setVisibility(View.VISIBLE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.restaurantsRecyclerView.setVisibility(View.GONE);

        restaurantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    allRestaurants.clear();
                    boolean hasRestaurants = false;

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            String restaurantId = snapshot.getKey();
                            if (restaurantId == null) continue;

                            // Check if restaurant is approved
                            DataSnapshot documentsSnapshot = snapshot.child("documents");
                            String status = documentsSnapshot.child("status").getValue(String.class);
                            if (!"approved".equals(status)) {
                                Log.d("CustomerHomeActivity", "Skipping unapproved restaurant: " + restaurantId);
                                continue;
                            }

                            Restaurant restaurant = new Restaurant();
                            restaurant.setId(restaurantId);

                            // Get basic info with default values
                            String name = snapshot.child("fullName").getValue(String.class);
                            restaurant.setName(name != null ? name : "Unnamed Restaurant");
                            
                            String email = snapshot.child("email").getValue(String.class);
                            restaurant.setEmail(email != null ? email : "");

                            // Get about info
                            String about = snapshot.child("about").getValue(String.class);
                            restaurant.setDescription(about != null ? about : "");

                            // Set open status
                            Boolean isOpen = snapshot.child("isOpen").getValue(Boolean.class);
                            restaurant.setOpen(isOpen != null ? isOpen : false);

                            // Get hours info
                            DataSnapshot hoursSnapshot = snapshot.child("hours");
                            if (hoursSnapshot.exists()) {
                                String openingTime = hoursSnapshot.child("opening").getValue(String.class);
                                String closingTime = hoursSnapshot.child("closing").getValue(String.class);
                                restaurant.setAddress(String.format("%s - %s", 
                                    openingTime != null ? openingTime : "N/A",
                                    closingTime != null ? closingTime : "N/A"));
                            }

                            // Set default values for fields not in the current structure
                            restaurant.setCuisine("Restaurant");
                            restaurant.setPhone("");
                            restaurant.setPriceRange("$");
                            restaurant.setImageURL("");
                            restaurant.setRating(0.0);
                            restaurant.setNumberOfRatings(0);

                            allRestaurants.add(restaurant);
                            hasRestaurants = true;
                            Log.d("CustomerHomeActivity", "Added restaurant: " + restaurant.getName() + " (ID: " + restaurantId + ")");
                        } catch (Exception e) {
                            Log.e("CustomerHomeActivity", "Error processing restaurant: " + snapshot.getKey(), e);
                            // Continue processing other restaurants
                            continue;
                        }
                    }

                    // Update UI based on results
                    binding.loadingProgressBar.setVisibility(View.GONE);
                    
                    if (hasRestaurants) {
                        binding.emptyStateLayout.setVisibility(View.GONE);
                        binding.restaurantsRecyclerView.setVisibility(View.VISIBLE);
                        restaurantAdapter.setRestaurants(allRestaurants);
                    } else {
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        binding.restaurantsRecyclerView.setVisibility(View.GONE);
                    }

                } catch (Exception e) {
                    Log.e("CustomerHomeActivity", "Error loading restaurants", e);
                    Toast.makeText(CustomerHomeActivity.this, 
                        "Error loading restaurants: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    
                    binding.loadingProgressBar.setVisibility(View.GONE);
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    binding.restaurantsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("CustomerHomeActivity", "Database error: " + databaseError.getMessage());
                Toast.makeText(CustomerHomeActivity.this, 
                    "Failed to load restaurants: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
                
                binding.loadingProgressBar.setVisibility(View.GONE);
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.restaurantsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void filterRestaurants(String query) {
        if (query == null || query.isEmpty()) {
            updateUI(allRestaurants);
            return;
        }

        List<Restaurant> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (Restaurant restaurant : allRestaurants) {
            if (restaurant.getName().toLowerCase().contains(lowerCaseQuery) ||
                restaurant.getCuisine().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(restaurant);
            }
        }

        updateUI(filteredList);
    }

    private void updateUI(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            binding.restaurantsRecyclerView.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            Log.d("CustomerHomeActivity", "Showing empty state - no restaurants to display");
        } else {
            binding.restaurantsRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            restaurantAdapter.setRestaurants(restaurants);
            Log.d("CustomerHomeActivity", "Displaying " + restaurants.size() + " restaurants");
        }
    }

    @Override
    public void onRestaurantClick(Restaurant restaurant) {
        Log.d("CustomerHomeActivity", "Starting onRestaurantClick...");
        
        try {
            if (restaurant == null) {
                Log.e("CustomerHomeActivity", "Restaurant is null");
                Toast.makeText(this, "Error: Invalid restaurant data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Log all restaurant data
            Log.d("CustomerHomeActivity", "Restaurant ID: " + restaurant.getId());
            Log.d("CustomerHomeActivity", "Restaurant Name: " + restaurant.getName());
            Log.d("CustomerHomeActivity", "Restaurant Email: " + restaurant.getEmail());
            Log.d("CustomerHomeActivity", "Restaurant Description: " + restaurant.getDescription());
            Log.d("CustomerHomeActivity", "Restaurant IsOpen: " + restaurant.isOpen());

            // Create a HashMap instead of using Serializable
            HashMap<String, Object> restaurantData = new HashMap<>();
            restaurantData.put("id", restaurant.getId());
            restaurantData.put("name", restaurant.getName());
            restaurantData.put("email", restaurant.getEmail());
            restaurantData.put("description", restaurant.getDescription());
            restaurantData.put("isOpen", restaurant.isOpen());
            restaurantData.put("cuisine", restaurant.getCuisine());
            restaurantData.put("priceRange", restaurant.getPriceRange());
            restaurantData.put("rating", restaurant.getRating());
            restaurantData.put("numberOfRatings", restaurant.getNumberOfRatings());
            restaurantData.put("address", restaurant.getAddress());
            restaurantData.put("imageURL", restaurant.getImageURL());

            Log.d("CustomerHomeActivity", "Created HashMap with restaurant data: " + restaurantData.toString());
            Log.d("CustomerHomeActivity", "Verifying restaurant ID in HashMap: " + restaurantData.get("id"));

            try {
                Intent intent = new Intent(CustomerHomeActivity.this, RestaurantMenuActivity.class);
                intent.putExtra("restaurant_data", restaurantData);
                Log.d("CustomerHomeActivity", "Added restaurant data to intent");
                startActivity(intent);
                Log.d("CustomerHomeActivity", "Started RestaurantMenuActivity");
            } catch (Exception e) {
                Log.e("CustomerHomeActivity", "Error starting activity", e);
                Toast.makeText(this, "Error opening restaurant menu", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("CustomerHomeActivity", "Error in onRestaurantClick", e);
            Toast.makeText(this, "Error processing restaurant data", Toast.LENGTH_SHORT).show();
        }
    }

    public void toggleFavorite(MenuItem menuItem, boolean isFavorite) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("favorites")
            .child(menuItem.getId());

        if (isFavorite) {
            // Add to favorites
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("id", menuItem.getId());
            favoriteData.put("name", menuItem.getName());
            favoriteData.put("description", menuItem.getDescription());
            favoriteData.put("price", menuItem.getPrice());
            favoriteData.put("imageURL", menuItem.getImageURL());
            favoriteData.put("category", menuItem.getCategory());
            favoriteData.put("timestamp", ServerValue.TIMESTAMP);

            favoritesRef.setValue(favoriteData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                });
        } else {
            // Remove from favorites
            favoritesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void loadFavoriteItemIds(MenuAdapter adapter) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("favorites");

        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    favoriteIds.add(itemSnapshot.getKey());
                }
                adapter.setFavoriteItems(favoriteIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CustomerHomeActivity", "Error loading favorites", error.toException());
            }
        });
    }

    @Override
    public void onFavoriteClick(MenuItem menuItem, boolean isFavorite) {
        toggleFavorite(menuItem, isFavorite);
    }
} 