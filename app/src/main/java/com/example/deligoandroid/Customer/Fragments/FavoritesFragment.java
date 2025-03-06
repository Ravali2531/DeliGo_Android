package com.example.deligoandroid.Customer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Customer.Adapters.MenuAdapter;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.Customer.Models.CustomizationOption;
import com.example.deligoandroid.Customer.Models.CustomizationItem;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FavoritesFragment extends Fragment implements MenuAdapter.OnAddToCartListener {
    private View loadingLayout;
    private View emptyLayout;
    private RecyclerView favoritesRecyclerView;
    private MenuAdapter menuAdapter;
    private List<MenuItem> favoriteItems = new ArrayList<>();
    private DatabaseReference favoritesRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        initializeViews(view);
        loadFavorites();
        
        return view;
    }

    private void initializeViews(View view) {
        loadingLayout = view.findViewById(R.id.loadingLayout);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        favoritesRecyclerView = view.findViewById(R.id.favoritesRecyclerView);
        
        // Setup RecyclerView
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuAdapter = new MenuAdapter(getContext());
        menuAdapter.setOnAddToCartListener(this);
        favoritesRecyclerView.setAdapter(menuAdapter);

        // Initialize Firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("favorites");
    }

    private void loadFavorites() {
        loadingLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
        favoritesRecyclerView.setVisibility(View.GONE);

        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                favoriteItems.clear();
                
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    try {
                        String itemId = itemSnapshot.getKey();
                        
                        // Get basic fields
                        String name = itemSnapshot.child("name").getValue(String.class);
                        String description = itemSnapshot.child("description").getValue(String.class);
                        String category = itemSnapshot.child("category").getValue(String.class);
                        Double price = itemSnapshot.child("price").getValue(Double.class);
                        String imageURL = itemSnapshot.child("imageURL").getValue(String.class);
                        Double timestamp = itemSnapshot.child("timestamp").getValue(Double.class);

                        if (name == null || price == null || category == null) {
                            continue;
                        }

                        MenuItem menuItem = new MenuItem();
                        menuItem.setId(itemId);
                        menuItem.setName(name);
                        menuItem.setDescription(description != null ? description : "");
                        menuItem.setCategory(category);
                        menuItem.setPrice(price);
                        menuItem.setImageURL(imageURL);
                        menuItem.setAvailable(true);

                        // Load customization options
                        if (itemSnapshot.hasChild("customizationOptions")) {
                            Log.d("FavoritesFragment", "Loading customization options for: " + name);
                            List<CustomizationOption> customizationOptions = new ArrayList<>();
                            DataSnapshot optionsSnapshot = itemSnapshot.child("customizationOptions");
                            
                            for (DataSnapshot optionSnapshot : optionsSnapshot.getChildren()) {
                                try {
                                    String optionId = optionSnapshot.child("id").getValue(String.class);
                                    String optionName = optionSnapshot.child("name").getValue(String.class);
                                    String optionType = optionSnapshot.child("type").getValue(String.class);
                                    Boolean required = optionSnapshot.child("required").getValue(Boolean.class);

                                    Log.d("FavoritesFragment", "Found option: " + optionName + ", Type: " + optionType);

                                    CustomizationOption option = new CustomizationOption();
                                    option.setId(optionId);
                                    option.setName(optionName);
                                    option.setType(optionType);
                                    option.setRequired(required != null && required);

                                    List<CustomizationItem> items = new ArrayList<>();
                                    DataSnapshot optionsData = optionSnapshot.child("options");
                                    Log.d("FavoritesFragment", "Number of items in option: " + optionsData.getChildrenCount());

                                    for (DataSnapshot itemOptionSnapshot : optionsData.getChildren()) {
                                        try {
                                            CustomizationItem customItem = new CustomizationItem();
                                            customItem.setId(itemOptionSnapshot.child("id").getValue(String.class));
                                            customItem.setName(itemOptionSnapshot.child("name").getValue(String.class));
                                            Double itemPrice = itemOptionSnapshot.child("price").getValue(Double.class);
                                            customItem.setPrice(itemPrice != null ? itemPrice : 0.0);
                                            items.add(customItem);
                                            
                                            Log.d("FavoritesFragment", "Added item: " + customItem.getName() + ", Price: " + customItem.getPrice());
                                        } catch (Exception e) {
                                            Log.e("FavoritesFragment", "Error processing customization item: " + e.getMessage());
                                        }
                                    }
                                    option.setOptions(items);
                                    customizationOptions.add(option);
                                } catch (Exception e) {
                                    Log.e("FavoritesFragment", "Error processing option: " + e.getMessage());
                                }
                            }
                            
                            // Only set customization options if we actually loaded some
                            if (!customizationOptions.isEmpty()) {
                                Log.d("FavoritesFragment", "Setting " + customizationOptions.size() + " customization options");
                                menuItem.setCustomizationOptions(customizationOptions);
                                menuItem.setHasCustomizations(true);
                                // Add detailed logging
                                Log.d("FavoritesFragment", "MenuItem state after setting options:");
                                Log.d("FavoritesFragment", "- Name: " + menuItem.getName());
                                Log.d("FavoritesFragment", "- HasCustomizations: " + menuItem.hasCustomizations());
                                Log.d("FavoritesFragment", "- Options size: " + menuItem.getCustomizationOptions().size());
                                for (CustomizationOption opt : menuItem.getCustomizationOptions()) {
                                    Log.d("FavoritesFragment", "  - Option: " + opt.getName() + " (Items: " + opt.getOptions().size() + ")");
                                }
                            } else {
                                Log.d("FavoritesFragment", "No customization options were loaded");
                                menuItem.setHasCustomizations(false);
                            }
                        } else {
                            Log.d("FavoritesFragment", "No customizationOptions child for: " + name);
                            menuItem.setHasCustomizations(false);
                        }

                        favoriteItems.add(menuItem);
                    } catch (Exception e) {
                        Log.e("FavoritesFragment", "Error processing favorite item", e);
                    }
                }

                // Sort by timestamp (most recent first)
                Collections.sort(favoriteItems, (item1, item2) -> {
                    Double timestamp1 = snapshot.child(item1.getId()).child("timestamp").getValue(Double.class);
                    Double timestamp2 = snapshot.child(item2.getId()).child("timestamp").getValue(Double.class);
                    if (timestamp1 == null) timestamp1 = 0.0;
                    if (timestamp2 == null) timestamp2 = 0.0;
                    return timestamp2.compareTo(timestamp1);
                });

                updateUI();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FavoritesFragment", "Error loading favorites", error.toException());
                Toast.makeText(getContext(), "Error loading favorites", Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    private void updateUI() {
        loadingLayout.setVisibility(View.GONE);
        
        if (favoriteItems.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
            menuAdapter.setMenuItems(Collections.singletonMap("Favorites", favoriteItems));
        }
    }

    @Override
    public void onAddToCart(CartItem cartItem) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("cart");

        cartRef.child(cartItem.getId()).setValue(cartItem)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Item added to cart", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to add item to cart", Toast.LENGTH_SHORT).show();
            });
    }
} 