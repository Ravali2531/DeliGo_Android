package com.example.deligoandroid.Customer.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.app.NotificationManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.media.RingtoneManager;
import androidx.core.app.NotificationCompat;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class CustomerHomeActivity extends AppCompatActivity 
    implements RestaurantAdapter.OnRestaurantClickListener,
               MenuAdapter.OnFavoriteClickListener {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private ActivityCustomerHomeBinding binding;
    private DatabaseReference restaurantsRef;
    private DatabaseReference ordersRef;
    private DatabaseReference notificationsRef;
    private ValueEventListener ordersListener;
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private RestaurantAdapter restaurantAdapter;
    private MenuAdapter menuAdapter;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase references
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ordersRef = FirebaseDatabase.getInstance().getReference().child("orders");
        notificationsRef = FirebaseDatabase.getInstance().getReference().child("notifications");

        // Initialize FCM token
        initializeFCMToken();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();

        setupViews();
        setupBottomNavigation();
        loadRestaurants();
        setupOrderStatusListener();

        // Handle notification navigation
        handleNotificationNavigation(getIntent());
    }

    private void initializeFCMToken() {
        Log.d("CustomerHomeActivity", "Initializing FCM token...");
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("CustomerHomeActivity", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d("CustomerHomeActivity", "FCM Token: " + token);

                // Store the token in Firebase
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (userId != null) {
                    Log.d("CustomerHomeActivity", "Storing FCM token for user: " + userId);
                    FirebaseDatabase.getInstance().getReference("customers")
                        .child(userId)
                        .child("fcmToken")
                        .setValue(token)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("CustomerHomeActivity", "FCM token stored successfully in Firebase");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CustomerHomeActivity", "Failed to store FCM token in Firebase", e);
                        });
                } else {
                    Log.e("CustomerHomeActivity", "User ID is null, cannot store FCM token");
                }
            });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied. Showing all restaurants.", 
                    Toast.LENGTH_SHORT).show();
                loadRestaurants();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Log.d("CustomerHomeActivity", "Getting current location...");

        // Create location request
        com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create();
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds

        // Check location settings
        com.google.android.gms.location.LocationSettingsRequest.Builder builder = 
            new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        com.google.android.gms.location.SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
            .addOnSuccessListener(this, locationSettingsResponse -> {
                // Location settings are satisfied, get location
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            Log.d("CustomerHomeActivity", "Location obtained - Latitude: " + 
                                location.getLatitude() + ", Longitude: " + location.getLongitude());
                            loadRestaurants();
                        } else {
                            Log.e("CustomerHomeActivity", "Location is null, requesting location updates");
                            // Request location updates
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                new com.google.android.gms.location.LocationCallback() {
                                    @Override
                                    public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                                        if (locationResult != null && locationResult.getLastLocation() != null) {
                                            currentLocation = locationResult.getLastLocation();
                                            Log.d("CustomerHomeActivity", "Location update received - Latitude: " + 
                                                currentLocation.getLatitude() + ", Longitude: " + 
                                                currentLocation.getLongitude());
                                            loadRestaurants();
                                            fusedLocationClient.removeLocationUpdates(this);
                                        }
                                    }
                                },
                                null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CustomerHomeActivity", "Error getting location: " + e.getMessage());
                        Toast.makeText(this, "Error getting location: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                    try {
                        // Show dialog to enable location settings
                        com.google.android.gms.common.api.ResolvableApiException resolvable =
                            (com.google.android.gms.common.api.ResolvableApiException) e;
                        resolvable.startResolutionForResult(this, 1002);
                    } catch (android.content.IntentSender.SendIntentException sendEx) {
                        Log.e("CustomerHomeActivity", "Error showing location settings dialog", sendEx);
                    }
                } else {
                    Log.e("CustomerHomeActivity", "Location settings check failed", e);
                    Toast.makeText(this, "Please enable location services in settings", 
                        Toast.LENGTH_LONG).show();
                }
            });
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

        // Setup Spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
            R.array.sort_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sortSpinner.setAdapter(spinnerAdapter);
        
        binding.sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: // Distance
                        sortRestaurantsByDistance();
                        break;
                    case 1: // Price: Low to High
                        sortRestaurantsByPrice(true);
                        break;
                    case 2: // Price: High to Low
                        sortRestaurantsByPrice(false);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
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

        Log.d("CustomerHomeActivity", "Loading restaurants. Current location: " + 
            (currentLocation != null ? 
            "Lat: " + currentLocation.getLatitude() + ", Lng: " + currentLocation.getLongitude() 
            : "Not available"));

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

                            DataSnapshot storeInfosnapshot = snapshot.child("store_info");

                            // Get basic info with default values
                            String name = storeInfosnapshot.child("name").getValue(String.class);
                            restaurant.setName(name != null ? name : "Unnamed Restaurant");
                            
                            String email = storeInfosnapshot.child("email").getValue(String.class);
                            restaurant.setEmail(email != null ? email : "");

                            // Get about info
                            String about = storeInfosnapshot.child("description").getValue(String.class);
                            restaurant.setDescription(about != null ? about : "");

                            // Get price range - try multiple fields
                            String priceRange = storeInfosnapshot.child("priceRange").getValue(String.class);
                            
                            // If no priceRange in store_info, check menu_info
                            if (priceRange == null || priceRange.isEmpty()) {
                                DataSnapshot menuInfoSnapshot = snapshot.child("menu_info");
                                if (menuInfoSnapshot.exists()) {
                                    priceRange = menuInfoSnapshot.child("priceRange").getValue(String.class);
                                }
                            }
                            
                            // If still no priceRange, look for minimum and maximum price
                            if (priceRange == null || priceRange.isEmpty()) {
                                DataSnapshot menuItemsSnapshot = snapshot.child("menu_items");
                                if (menuItemsSnapshot.exists() && menuItemsSnapshot.getChildrenCount() > 0) {
                                    double minPrice = Double.MAX_VALUE;
                                    double maxPrice = 0;
                                    boolean hasPrices = false;
                                    
                                    for (DataSnapshot itemSnapshot : menuItemsSnapshot.getChildren()) {
                                        Double price = itemSnapshot.child("price").getValue(Double.class);
                                        if (price != null && price > 0) {
                                            minPrice = Math.min(minPrice, price);
                                            maxPrice = Math.max(maxPrice, price);
                                            hasPrices = true;
                                        }
                                    }
                                    
                                    if (hasPrices) {
                                        priceRange = "$" + String.format("%.2f", minPrice) + "-$" + String.format("%.2f", maxPrice);
                                    }
                                }
                            }
                            
                            restaurant.setPriceRange(priceRange != null ? priceRange : "");
                            Log.d("CustomerHomeActivity", "Restaurant " + restaurant.getName() + 
                                " has price range: " + restaurant.getPriceRange());

                            // Set open status
                            Boolean isOpen = snapshot.child("isOpen").getValue(Boolean.class);
                            restaurant.setOpen(isOpen != null ? isOpen : false);

                            String address = storeInfosnapshot.child("address").getValue(String.class);
                            restaurant.setAddress(address != null ? address : "");

                            DataSnapshot hoursInfosnapshot = snapshot.child("hours");

                            // Get about info
                            String closing = hoursInfosnapshot.child("closing").getValue(String.class);
                            restaurant.setClosing(closing != null ? closing : "");

                            // Get about info
                            String opening = hoursInfosnapshot.child("opening").getValue(String.class);
                            restaurant.setOpening(opening != null ? opening : "");

                            // Get location info
                            DataSnapshot locationSnapshot = snapshot.child("location");
                            if (locationSnapshot.exists()) {
                                Double lat = locationSnapshot.child("latitude").getValue(Double.class);
                                Double lng = locationSnapshot.child("longitude").getValue(Double.class);
                                String locationName = locationSnapshot.child("name").getValue(String.class);
                                
                                restaurant.setLatitude(lat != null ? lat : 0.0);
                                restaurant.setLongitude(lng != null ? lng : 0.0);

                                Log.d("CustomerHomeActivity", "Restaurant " + restaurant.getName() + 
                                    " location - Lat: " + restaurant.getLatitude() + 
                                    ", Lng: " + restaurant.getLongitude());

                                // Calculate distance if current location is available
                                if (currentLocation != null) {
                                    float[] results = new float[1];
                                    Location.distanceBetween(
                                        currentLocation.getLatitude(), currentLocation.getLongitude(),
                                        restaurant.getLatitude(), restaurant.getLongitude(),
                                        results
                                    );
                                    restaurant.setDistance(results[0] / 1000.0); // Convert to kilometers
                                    Log.d("CustomerHomeActivity", "Distance to " + restaurant.getName() + 
                                        ": " + restaurant.getDistance() + " km");
                                } else {
                                    Log.d("CustomerHomeActivity", "Current location not available for " + 
                                        restaurant.getName() + ", distance not calculated");
                                }
                            } else {
                                Log.d("CustomerHomeActivity", "No location data for restaurant: " + 
                                    restaurant.getName());
                            }

                            // Set default values for fields not in the current structure
                            restaurant.setCuisine("Restaurant");
                            restaurant.setPhone("");
                            restaurant.setImageURL("");

                            // Get ratings data from ratingsandcomments
                            DataSnapshot ratingsSnapshot = snapshot.child("ratingsandcomments").child("rating");
                            if (ratingsSnapshot.exists()) {
                                double totalRating = 0.0;
                                int ratingCount = 0;
                                
                                for (DataSnapshot customerRatingSnapshot : ratingsSnapshot.getChildren()) {
                                    Double rating = customerRatingSnapshot.getValue(Double.class);
                                    if (rating != null) {
                                        totalRating += rating;
                                        ratingCount++;
                                    }
                                }
                                
                                if (ratingCount > 0) {
                                    double averageRating = totalRating / ratingCount;
                                    restaurant.setRating(averageRating);
                                    restaurant.setNumberOfRatings(ratingCount);
                                    Log.d("CustomerHomeActivity", "Restaurant " + restaurant.getName() + 
                                        " has rating: " + averageRating + " from " + ratingCount + " ratings");
                                } else {
                                    restaurant.setRating(0.0);
                                    restaurant.setNumberOfRatings(0);
                                    Log.d("CustomerHomeActivity", "Restaurant " + restaurant.getName() + 
                                        " has no ratings");
                                }
                            } else {
                                restaurant.setRating(0.0);
                                restaurant.setNumberOfRatings(0);
                                Log.d("CustomerHomeActivity", "Restaurant " + restaurant.getName() + 
                                    " has no ratings node");
                            }

                            allRestaurants.add(restaurant);
                            hasRestaurants = true;
                            Log.d("CustomerHomeActivity", "Added restaurant: " + restaurant.getName() + 
                                " (ID: " + restaurantId + ")");
                        } catch (Exception e) {
                            Log.e("CustomerHomeActivity", "Error processing restaurant: " + 
                                snapshot.getKey(), e);
                            continue;
                        }
                    }

                    // Apply the current sort preference based on the selected spinner item
                    applySortBasedOnSelectedItem();
                    
                    // Update UI based on results
                    binding.loadingProgressBar.setVisibility(View.GONE);
                    
                    if (hasRestaurants) {
                        binding.emptyStateLayout.setVisibility(View.GONE);
                        binding.restaurantsRecyclerView.setVisibility(View.VISIBLE);
                        restaurantAdapter.setRestaurants(allRestaurants);
                        Log.d("CustomerHomeActivity", "Displaying " + allRestaurants.size() + " restaurants");
                    } else {
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        binding.restaurantsRecyclerView.setVisibility(View.GONE);
                        Log.d("CustomerHomeActivity", "No restaurants to display");
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

    private void applySortBasedOnSelectedItem() {
        int position = binding.sortSpinner.getSelectedItemPosition();
        // Default to distance sort (0) if no selection
        if (position < 0) position = 0;
        
        switch (position) {
            case 0: // Distance
                sortRestaurantsByDistance();
                break;
            case 1: // Price: Low to High
                sortRestaurantsByPrice(true);
                break;
            case 2: // Price: High to Low
                sortRestaurantsByPrice(false);
                break;
        }
    }

    private double getPriceRangeValue(String priceRange, boolean lowToHigh) {
        if (priceRange == null || priceRange.isEmpty()) {
            return 0; // Default value for unknown price ranges
        }
        
        Log.d("CustomerHomeActivity", "Processing price range: " + priceRange);
        
        // Check if the price range is in "$" format
        if (priceRange.startsWith("$") && !priceRange.contains("-")) {
            return priceRange.length(); // $=1, $$=2, $$$=3, $$$$=4
        }
        
        // Check if price range is in "min-max" format with currency symbol
        if (priceRange.contains("-")) {
            try {
                // Remove currency symbols and any non-numeric characters except decimal points and minus
                String cleanedRange = priceRange.replaceAll("\\$", "");
                String[] parts = cleanedRange.split("-");
                if (parts.length == 2) {
                    // Further clean each part to ensure valid numbers
                    String minPart = parts[0].trim().replaceAll("[^0-9\\.]", "");
                    String maxPart = parts[1].trim().replaceAll("[^0-9\\.]", "");
                    
                    double min = Double.parseDouble(minPart);
                    double max = Double.parseDouble(maxPart);
                    
                    Log.d("CustomerHomeActivity", "Parsed price range: min=" + min + ", max=" + max);
                    
                    // For low to high, use minimum price; for high to low, use maximum price
                    return lowToHigh ? min : max;
                }
            } catch (NumberFormatException e) {
                Log.e("CustomerHomeActivity", "Error parsing price range: " + priceRange, e);
            }
        }
        
        // Try to parse as a single number after removing currency symbol
        try {
            String cleanedPrice = priceRange.replaceAll("[^0-9\\.]", "");
            return Double.parseDouble(cleanedPrice.trim());
        } catch (NumberFormatException e) {
            Log.e("CustomerHomeActivity", "Unable to parse price range: " + priceRange, e);
            return 0;
        }
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
            restaurantData.put("closing", restaurant.getClosing());
            restaurantData.put("opening", restaurant.getOpening());

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
            .child("customers")
            .child(userId)
            .child("favorites")
            .child(menuItem.getId());

        Log.d("CustomerHomeActivity", "Toggling favorite for item: " + menuItem.getName());
        Log.d("CustomerHomeActivity", "Item restaurantId: " + menuItem.getRestaurantId());

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
            favoriteData.put("restaurantId", menuItem.getRestaurantId());

            Log.d("CustomerHomeActivity", "Adding to favorites with restaurantId: " + menuItem.getRestaurantId());

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
            .child("customers")
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

    private void setupOrderStatusListener() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Remove any existing listener
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }

        // Setup new listener for orders
        ordersListener = ordersRef.orderByChild("customerId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            String status = orderSnapshot.child("status").getValue(String.class);
                            String orderStatus = orderSnapshot.child("order_status").getValue(String.class);
                            String orderId = orderSnapshot.getKey();

                            // Check various order statuses and show notifications
                            if ("in_progress".equals(status) && "accepted".equals(orderStatus)) {
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "accepted",
                                    "Order Accepted",
                                    "Your order #" + orderId + " has been accepted by the restaurant"
                                );
                            }
                            
                            if ("in_progress".equals(status) && "driver_accepted".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ? 
                                    "Driver " + driverName + " has accepted your order #" + orderId :
                                    "A driver has accepted your order #" + orderId;
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "driver_accepted",
                                    "Driver Assigned",
                                    message
                                );
                            }
                            
                            if ("in_progress".equals(status) && "picked_up".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ? 
                                    "Driver " + driverName + " has picked up your order #" + orderId :
                                    "Your order #" + orderId + " has been picked up";
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "picked_up",
                                    "Order Picked Up",
                                    message
                                );
                            }

                            if ("delivered".equals(status) && "delivered".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ?
                                    "Driver " + driverName + " has delivered your order #" + orderId :
                                    "Your order #" + orderId + " has been delivered";
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "delivered",
                                    "Order Delivered",
                                    message
                                );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("CustomerHomeActivity", "Error monitoring orders", databaseError.toException());
                    }
                });
    }

    private void checkAndShowNotification(DatabaseReference notificationsRef, String orderId, 
                                        String type, String title, String message) {
        // Check if this notification has already been shown
        notificationsRef.child(orderId).child(type)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists() || !Boolean.TRUE.equals(dataSnapshot.child("shown").getValue(Boolean.class))) {
                            // Show notification
                            showOrderNotification(orderId + "_" + type, title, message);

                            // Store notification data
                            Map<String, Object> notificationData = new HashMap<>();
                            notificationData.put("title", title);
                            notificationData.put("body", message);
                            notificationData.put("timestamp", ServerValue.TIMESTAMP);
                            notificationData.put("shown", true);
                            notificationData.put("read", false);

                            notificationsRef.child(orderId).child(type).setValue(notificationData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("CustomerHomeActivity", "Notification status stored successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CustomerHomeActivity", "Failed to store notification status", e);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("CustomerHomeActivity", "Error checking notification status", databaseError.toException());
                    }
                });
    }

    private void showOrderNotification(String orderId, String title, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "order_notifications",
                "Order Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Create intent for notification click
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("orderId", orderId);
        intent.putExtra("navigate_to", "orders");

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "order_notifications")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(pendingIntent);

        // Show notification
        int notificationId = orderId != null ? orderId.hashCode() : 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void handleNotificationNavigation(Intent intent) {
        if (intent.hasExtra("navigate_to")) {
            String navigateTo = intent.getStringExtra("navigate_to");
            if ("orders".equals(navigateTo)) {
                // Navigate to orders fragment
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new OrdersFragment())
                    .commit();
                
                // Update bottom navigation selection
                binding.bottomNavigation.setSelectedItemId(R.id.nav_orders);
            }
        }
    }

    private void sortRestaurantsByDistance() {
        if (currentLocation != null) {
            Collections.sort(allRestaurants, (r1, r2) -> 
                Double.compare(r1.getDistance(), r2.getDistance()));
            updateUI(allRestaurants);
        } else {
            Toast.makeText(this, "Location not available for distance sorting", Toast.LENGTH_SHORT).show();
        }
    }

    private void sortRestaurantsByPrice(boolean lowToHigh) {
        Collections.sort(allRestaurants, (r1, r2) -> {
            double r1Price = getPriceRangeValue(r1.getPriceRange(), lowToHigh);
            double r2Price = getPriceRangeValue(r2.getPriceRange(), lowToHigh);
            return lowToHigh ? 
                Double.compare(r1Price, r2Price) : 
                Double.compare(r2Price, r1Price);
        });
        updateUI(allRestaurants);
        Log.d("CustomerHomeActivity", "Sorted restaurants by price: " + (lowToHigh ? "Low to High" : "High to Low"));
        
        // Log sorted restaurants and their price ranges
        for (Restaurant r : allRestaurants) {
            Log.d("CustomerHomeActivity", "Restaurant: " + r.getName() + 
                  ", Price range: " + r.getPriceRange() + 
                  ", Sort value: " + getPriceRangeValue(r.getPriceRange(), lowToHigh));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the orders listener when activity is destroyed
        if (ordersListener != null) {
            ordersRef.removeEventListener(ordersListener);
        }
    }
} 