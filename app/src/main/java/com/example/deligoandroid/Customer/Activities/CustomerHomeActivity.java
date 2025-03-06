package com.example.deligoandroid.Customer.Activities;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.RestaurantAdapter;
import com.example.deligoandroid.Customer.Fragments.AccountFragment;
import com.example.deligoandroid.Customer.Fragments.CartFragment;
import com.example.deligoandroid.Customer.Fragments.FavoritesFragment;
import com.example.deligoandroid.Customer.Fragments.OrdersFragment;
import com.example.deligoandroid.Customer.Models.Restaurant;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityCustomerHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerHomeActivity extends AppCompatActivity {
    private ActivityCustomerHomeBinding binding;
    private DatabaseReference restaurantsRef;
    private List<Restaurant> allRestaurants = new ArrayList<>();
    private RestaurantAdapter restaurantAdapter;

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
        binding.restaurantsRecyclerView.setAdapter(restaurantAdapter);

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
                binding.fragmentContainer.setVisibility(View.GONE);
                binding.mainContent.setVisibility(View.VISIBLE);
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

    private void showFragment(Fragment fragment) {
        binding.mainContent.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    private void loadRestaurants() {
        restaurantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allRestaurants.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Restaurant restaurant = snapshot.getValue(Restaurant.class);
                    if (restaurant != null) {
                        restaurant.setId(snapshot.getKey());
                        allRestaurants.add(restaurant);
                    }
                }
                updateUI(allRestaurants);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
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
            if (restaurant.getName().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(restaurant);
            }
        }

        updateUI(filteredList);
    }

    private void updateUI(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            binding.restaurantsRecyclerView.setVisibility(View.GONE);
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            binding.restaurantsRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyStateLayout.setVisibility(View.GONE);
            restaurantAdapter.setRestaurants(restaurants);
        }
    }
} 