package com.example.deligoandroid.Customer.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.CartAdapter;
import com.example.deligoandroid.Customer.CheckoutActivity;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.CustomizationSelection;
import com.example.deligoandroid.Customer.Models.SelectedItem;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.FragmentCartBinding;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment implements CartAdapter.CartItemListener {
    private static final String TAG = "CartFragment";
    private FragmentCartBinding binding;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private double subtotal = 0.0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupCheckoutButton();
        loadCartItems();
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(requireContext(), this);
        binding.cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.cartRecyclerView.setAdapter(cartAdapter);
        Log.d(TAG, "RecyclerView setup complete");
    }

    private void setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener(v -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                String firstRestaurantId = cartItems.get(0).getRestaurantId();
                boolean allSameRestaurant = true;
                
                for (CartItem item : cartItems) {
                    if (!firstRestaurantId.equals(item.getRestaurantId())) {
                        allSameRestaurant = false;
                        break;
                    }
                }
                
                if (allSameRestaurant) {
                    Intent intent = new Intent(getActivity(), CheckoutActivity.class);
                    intent.putExtra("subtotal", subtotal);
                    intent.putExtra("cartItems", new ArrayList<>(cartItems));
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), 
                        "Please select items from the same restaurant", 
                        Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadCartItems() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "No user logged in");
            updateUI();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading cart items for user: " + userId);

        cartRef = FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(userId)
                .child("cart");

        Log.d(TAG, "Database reference path: " + cartRef.toString());

        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Log.d(TAG, "Cart data changed. Number of items: " + snapshot.getChildrenCount());
                Log.d(TAG, "Snapshot exists: " + snapshot.exists());
                Log.d(TAG, "Snapshot value: " + snapshot.getValue());

                cartItems.clear();
                subtotal = 0.0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    try {
                        String firebaseKey = itemSnapshot.getKey();
                        Log.d(TAG, "Processing item with key: " + firebaseKey);
                        
                        // Log raw data for debugging
                        Object rawValue = itemSnapshot.getValue();
                        Log.d(TAG, "Item raw data: " + rawValue);
                        
                        CartItem item = null;
                        
                        // Try to parse using CartItem.class first
                        try {
                            item = itemSnapshot.getValue(CartItem.class);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse directly to CartItem: " + e.getMessage());
                        }
                        
                        // If direct parsing fails, try manual conversion from Map
                        if (item == null && rawValue instanceof Map) {
                            try {
                                Map<String, Object> itemMap = (Map<String, Object>) rawValue;
                                // Create a new CartItem manually
                                item = new CartItem();
                                if (itemMap.containsKey("menuItemId")) item.setMenuItemId((String) itemMap.get("menuItemId"));
                                if (itemMap.containsKey("name")) item.setName((String) itemMap.get("name"));
                                if (itemMap.containsKey("description")) item.setDescription((String) itemMap.get("description"));
                                if (itemMap.containsKey("price")) {
                                    Object priceObj = itemMap.get("price");
                                    if (priceObj instanceof Number) {
                                        item.setPrice(((Number) priceObj).doubleValue());
                                    }
                                }
                                if (itemMap.containsKey("quantity")) {
                                    Object quantityObj = itemMap.get("quantity");
                                    if (quantityObj instanceof Number) {
                                        item.setQuantity(((Number) quantityObj).intValue());
                                    }
                                }
                                if (itemMap.containsKey("imageURL")) item.setImageURL((String) itemMap.get("imageURL"));
                                if (itemMap.containsKey("restaurantId")) item.setRestaurantId((String) itemMap.get("restaurantId"));
                                if (itemMap.containsKey("totalPrice")) {
                                    Object totalPriceObj = itemMap.get("totalPrice");
                                    if (totalPriceObj instanceof Number) {
                                        item.setTotalPrice(((Number) totalPriceObj).doubleValue());
                                    }
                                }
                                if (itemMap.containsKey("specialInstructions")) {
                                    item.setSpecialInstructions((String) itemMap.get("specialInstructions"));
                                }
                                
                                // Handle customizations
                                if (itemMap.containsKey("customizations") && itemMap.get("customizations") instanceof Map) {
                                    Map<String, Object> customizationsMap = (Map<String, Object>) itemMap.get("customizations");
                                    Map<String, List<CustomizationSelection>> parsedCustomizations = new HashMap<>();
                                    
                                    for (Map.Entry<String, Object> entry : customizationsMap.entrySet()) {
                                        String customizationId = entry.getKey();
                                        if (entry.getValue() instanceof List) {
                                            List<Object> selections = (List<Object>) entry.getValue();
                                            List<CustomizationSelection> selectionsList = new ArrayList<>();
                                            
                                            for (Object selObj : selections) {
                                                if (selObj instanceof Map) {
                                                    Map<String, Object> selMap = (Map<String, Object>) selObj;
                                                    CustomizationSelection selection = new CustomizationSelection();
                                                    
                                                    if (selMap.containsKey("optionId")) selection.setOptionId((String) selMap.get("optionId"));
                                                    if (selMap.containsKey("optionName")) selection.setOptionName((String) selMap.get("optionName"));
                                                    
                                                    if (selMap.containsKey("selectedItems") && selMap.get("selectedItems") instanceof List) {
                                                        List<Object> itemsList = (List<Object>) selMap.get("selectedItems");
                                                        List<SelectedItem> selectedItems = new ArrayList<>();
                                                        
                                                        for (Object itemObj : itemsList) {
                                                            if (itemObj instanceof Map) {
                                                                Map<String, Object> itemMap2 = (Map<String, Object>) itemObj;
                                                                SelectedItem selectedItem = new SelectedItem();
                                                                
                                                                if (itemMap2.containsKey("id")) selectedItem.setId((String) itemMap2.get("id"));
                                                                if (itemMap2.containsKey("name")) selectedItem.setName((String) itemMap2.get("name"));
                                                                if (itemMap2.containsKey("price")) {
                                                                    Object priceObj = itemMap2.get("price");
                                                                    if (priceObj instanceof Number) {
                                                                        selectedItem.setPrice(((Number) priceObj).doubleValue());
                                                                    }
                                                                }
                                                                
                                                                selectedItems.add(selectedItem);
                                                            }
                                                        }
                                                        
                                                        selection.setSelectedItems(selectedItems);
                                                    }
                                                    
                                                    selectionsList.add(selection);
                                                }
                                            }
                                            
                                            parsedCustomizations.put(customizationId, selectionsList);
                                        }
                                    }
                                    
                                    item.setCustomizations(parsedCustomizations);
                                }
                                
                                Log.d(TAG, "Successfully created CartItem manually from Map");
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to create CartItem manually: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        
                        if (item != null) {
                            // Always use the Firebase key as the ID
                            item.setId(firebaseKey);
                            Log.d(TAG, "Loaded cart item: " + item.getName() + ", Price: " + item.getTotalPrice() + ", ID: " + item.getId());
                            cartItems.add(item);
                            subtotal += item.getTotalPrice();
                        } else {
                            Log.e(TAG, "Failed to parse item from snapshot with ID: " + firebaseKey);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing cart item: " + e.getMessage());
                        Log.e(TAG, "Item data that caused error: " + itemSnapshot.getValue());
                        e.printStackTrace();
                    }
                }

                Log.d(TAG, "Total items loaded: " + cartItems.size() + ", Subtotal: " + subtotal);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                
                Log.e(TAG, "Error loading cart items: " + error.getMessage());
                Log.e(TAG, "Error details: " + error.getDetails());
                Log.e(TAG, "Error code: " + error.getCode());
                Toast.makeText(getContext(), "Error loading cart items", Toast.LENGTH_SHORT).show();
            }
        };
        cartRef.addValueEventListener(cartListener);
    }

    private void updateUI() {
        if (!isAdded()) return;

        if (cartItems.isEmpty()) {
            Log.d(TAG, "Cart is empty, showing empty state");
            binding.emptyCartText.setVisibility(View.VISIBLE);
            binding.cartRecyclerView.setVisibility(View.GONE);
            binding.checkoutButton.setEnabled(false);
        } else {
            Log.d(TAG, "Cart has items (" + cartItems.size() + "), showing cart content");
            binding.emptyCartText.setVisibility(View.GONE);
            binding.cartRecyclerView.setVisibility(View.VISIBLE);
            binding.checkoutButton.setEnabled(true);
        }

        binding.subtotalText.setText(String.format("Subtotal: $%.2f", subtotal));
        cartAdapter.setItems(cartItems);
        Log.d(TAG, "UI updated with " + cartItems.size() + " items and subtotal: $" + subtotal);
    }

    @Override
    public void onUpdateQuantity(String itemId, int newQuantity) {
        if (!isAdded()) return;

        Log.d(TAG, "Updating quantity for item: " + itemId + " to " + newQuantity);
        if (itemId != null) {
            // Update local list and UI first
            for (CartItem item : cartItems) {
                if (item.getId().equals(itemId)) {
                    item.setQuantity(newQuantity);
                    item.setTotalPrice(item.getPrice() * newQuantity);
                    break;
                }
            }
            
            calculateSubtotal();
            updateUI();

            // Then update Firebase
            if (cartRef != null) {
                cartRef.child(itemId).setValue(cartItems.stream()
                        .filter(item -> item.getId().equals(itemId))
                        .findFirst()
                        .orElse(null))
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Log.d(TAG, "Item updated successfully in Firebase");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Log.e(TAG, "Error updating item: " + e.getMessage());
                            Toast.makeText(getContext(), "Error updating quantity", Toast.LENGTH_SHORT).show();
                        }
                    });
            }
        }
    }

    @Override
    public void onRemoveItem(String itemId) {
        if (!isAdded()) return;

        Log.d(TAG, "Attempting to remove item with ID: " + itemId);
        
        if (itemId != null && !itemId.isEmpty() && cartRef != null) {
            // Create a direct reference to the item in Firebase
            DatabaseReference itemRef = cartRef.child(itemId);
            
            // Remove the item from Firebase
            itemRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully removed item from Firebase: " + itemId);
                    Toast.makeText(getContext(), "Item removed from cart", Toast.LENGTH_SHORT).show();
                    
                    // Note: We don't need to manually update the UI or local list
                    // because the ValueEventListener will trigger and reload everything
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove item from Firebase: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to remove item", Toast.LENGTH_SHORT).show();
                });
        } else {
            Log.e(TAG, "Cannot remove item - invalid ID or database reference");
        }
    }

    private void calculateSubtotal() {
        subtotal = 0.0;
        for (CartItem item : cartItems) {
            double itemTotal = item.getPrice() * item.getQuantity();
            Log.d(TAG, "Item: " + item.getName() + ", Price: " + item.getPrice() + 
                  ", Quantity: " + item.getQuantity() + ", Total: " + itemTotal);
            item.setTotalPrice(itemTotal);
            subtotal += itemTotal;
        }
        Log.d(TAG, "New subtotal: " + subtotal);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cartRef != null && cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
        binding = null;
    }
} 