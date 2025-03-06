package com.example.deligoandroid.Customer.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Customer.Adapters.CartAdapter;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class CartFragment extends Fragment implements CartAdapter.CartItemListener {
    private View emptyStateView;
    private View cartContentView;
    private RecyclerView cartRecyclerView;
    private TextView totalPriceText;
    private CartAdapter cartAdapter;
    private DatabaseReference cartRef;
    private ValueEventListener cartListener;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        
        initializeViews(view);
        setupCartListener();
        
        return view;
    }

    private void initializeViews(View view) {
        emptyStateView = view.findViewById(R.id.emptyStateLayout);
        cartContentView = view.findViewById(R.id.cartContentLayout);
        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        totalPriceText = view.findViewById(R.id.totalPriceText);
        Button checkoutButton = view.findViewById(R.id.checkoutButton);
        Button clearCartButton = view.findViewById(R.id.clearCartButton);

        // Setup RecyclerView
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cartAdapter = new CartAdapter(getContext(), this);
        cartRecyclerView.setAdapter(cartAdapter);

        // Setup empty state
        ImageView emptyIcon = emptyStateView.findViewById(R.id.emptyStateIcon);
        TextView emptyTitle = emptyStateView.findViewById(R.id.emptyStateTitle);
        TextView emptyMessage = emptyStateView.findViewById(R.id.emptyStateMessage);

        emptyIcon.setImageResource(R.drawable.ic_cart);
        emptyTitle.setText("Your Cart is Empty");
        emptyMessage.setText("Add items from a restaurant to get started");

        // Setup buttons
        clearCartButton.setOnClickListener(v -> showClearCartDialog());
        checkoutButton.setOnClickListener(v -> proceedToCheckout());

        // Initialize Firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cartRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("cart");
    }

    private void setupCartListener() {
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItems.clear();
                double total = 0;
                
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem cartItem = itemSnapshot.getValue(CartItem.class);
                    if (cartItem != null) {
                        cartItems.add(cartItem);
                        total += cartItem.getTotalPrice();
                    }
                }

                updateUI(total);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("CartFragment", "Error loading cart items", error.toException());
                Toast.makeText(getContext(), "Error loading cart items", Toast.LENGTH_SHORT).show();
            }
        };

        cartRef.addValueEventListener(cartListener);
    }

    private void updateUI(double total) {
        if (cartItems.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            cartContentView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            cartContentView.setVisibility(View.VISIBLE);
            cartAdapter.setItems(cartItems);
            totalPriceText.setText(String.format("$%.2f", total));
        }
    }

    private void showClearCartDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cart")
            .setMessage("Are you sure you want to clear your cart?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (dialog, which) -> clearCart())
            .show();
    }

    private void clearCart() {
        cartRef.removeValue()
            .addOnSuccessListener(aVoid -> 
                Toast.makeText(getContext(), "Cart cleared", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> 
                Toast.makeText(getContext(), "Failed to clear cart", Toast.LENGTH_SHORT).show());
    }

    private void proceedToCheckout() {
        // TODO: Implement checkout functionality
        Toast.makeText(getContext(), "Proceeding to checkout...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cartListener != null) {
            cartRef.removeEventListener(cartListener);
        }
    }

    @Override
    public void onUpdateQuantity(String itemId, int newQuantity) {
        cartRef.child(itemId).child("quantity").setValue(newQuantity)
            .addOnFailureListener(e -> 
                Toast.makeText(getContext(), "Failed to update quantity", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRemoveItem(String itemId) {
        cartRef.child(itemId).removeValue()
            .addOnFailureListener(e -> 
                Toast.makeText(getContext(), "Failed to remove item", Toast.LENGTH_SHORT).show());
    }
} 