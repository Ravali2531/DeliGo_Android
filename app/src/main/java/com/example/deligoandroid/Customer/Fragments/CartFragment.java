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
                        Log.d(TAG, "Processing item with key: " + itemSnapshot.getKey());
                        Log.d(TAG, "Item raw data: " + itemSnapshot.getValue());
                        
                        CartItem item = itemSnapshot.getValue(CartItem.class);
                        if (item != null) {
                            if (item.getId() == null) {
                                item.setId(itemSnapshot.getKey());
                            }
                            Log.d(TAG, "Loaded cart item: " + item.getName() + ", Price: " + item.getTotalPrice());
                            cartItems.add(item);
                            subtotal += item.getTotalPrice();
                        } else {
                            Log.e(TAG, "Failed to parse item from snapshot: " + itemSnapshot.getValue());
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

        Log.d(TAG, "Starting to remove item with ID: " + itemId);
        if (itemId != null && cartRef != null) {
            // Find the item to be removed
            CartItem itemToRemove = null;
            for (CartItem item : cartItems) {
                if (item.getId().equals(itemId)) {
                    itemToRemove = item;
                    break;
                }
            }

            if (itemToRemove == null) {
                Log.e(TAG, "Could not find item with ID: " + itemId);
                return;
            }

            // Generate Firebase key based on menu item ID and customizations
            String firebaseKey = itemToRemove.getMenuItemId();
            if (itemToRemove.getCustomizations() != null && !itemToRemove.getCustomizations().isEmpty()) {
                StringBuilder keyBuilder = new StringBuilder(firebaseKey);
                for (Map.Entry<String, List<CustomizationSelection>> entry : itemToRemove.getCustomizations().entrySet()) {
                    keyBuilder.append("_").append(entry.getKey());
                    List<CustomizationSelection> selections = entry.getValue();
                    if (selections != null) {
                        for (CustomizationSelection selection : selections) {
                            if (selection.getSelectedItems() != null) {
                                for (SelectedItem item : selection.getSelectedItems()) {
                                    keyBuilder.append("_").append(item.getName());
                                }
                            }
                        }
                    }
                }
                firebaseKey = keyBuilder.toString();
            }
            
            Log.d(TAG, "Generated Firebase key for removal: " + firebaseKey);
            
            // Remove from local list first
            cartItems.removeIf(item -> item.getId().equals(itemId));
            calculateSubtotal();
            updateUI();

            // Then remove from Firebase
            Log.d(TAG, "Attempting to remove item from Firebase, path: " + cartRef.child(firebaseKey).toString());
            cartRef.child(firebaseKey).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Log.d(TAG, "Item successfully removed from Firebase");
                        Toast.makeText(getContext(), "Item removed from cart", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Log.e(TAG, "Error removing item from Firebase: " + e.getMessage());
                        Toast.makeText(getContext(), "Error removing item", Toast.LENGTH_SHORT).show();
                    }
                });
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