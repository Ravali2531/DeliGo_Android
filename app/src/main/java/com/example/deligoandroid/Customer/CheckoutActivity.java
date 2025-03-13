package com.example.deligoandroid.Customer;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.CartAdapter;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity implements CartAdapter.CartItemListener {
    private static final String TAG = "CheckoutActivity";
    private static final double DELIVERY_FEE = 4.99;

    private RecyclerView cartItemsRecyclerView;
    private CartAdapter cartAdapter;
    private TextView subtotalText, totalText, tipAmountText, deliveryFeeAmount;
    private MaterialButton placeOrderButton;
    private RadioGroup deliveryOptionsGroup;
    private RadioGroup paymentMethodGroup;
    private View deliveryAddressSection;
    private TextInputEditText streetAddressInput;
    private TextInputEditText unitInput;
    private TextInputEditText instructionsInput;
    private ChipGroup tipOptions;
    private View deliveryFeeRow;

    private List<CartItem> cartItems;
    private double subtotal = 0.0;
    private double tipPercentage = 15.0; // Default tip percentage
    private boolean isDelivery = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        initializeViews();
        setupListeners();
        loadCartItems();
        setupRecyclerView();
        updateTotals();
    }

    private void initializeViews() {
        // Cart items section
        cartItemsRecyclerView = findViewById(R.id.cartItemsRecyclerView);
        
        // Delivery options section
        deliveryOptionsGroup = findViewById(R.id.deliveryOptionsGroup);
        deliveryAddressSection = findViewById(R.id.deliveryAddressSection);
        
        // Address inputs
        streetAddressInput = findViewById(R.id.streetAddressInput);
        unitInput = findViewById(R.id.unitInput);
        instructionsInput = findViewById(R.id.instructionsInput);
        
        // Tip section
        tipOptions = findViewById(R.id.tipOptions);
        tipAmountText = findViewById(R.id.tipAmountText);
        
        // Payment section
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        
        // Summary section
        subtotalText = findViewById(R.id.subtotalText);
        totalText = findViewById(R.id.totalText);
        deliveryFeeAmount = findViewById(R.id.deliveryFeeAmount);
        deliveryFeeRow = findViewById(R.id.deliveryFeeRow);
        
        // Place order button
        placeOrderButton = findViewById(R.id.placeOrderButton);
    }

    private void setupListeners() {
        // Delivery option listener
        deliveryOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isDelivery = checkedId == R.id.deliveryOption;
            deliveryAddressSection.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            deliveryFeeRow.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            updateTotals();
        });

        // Tip options listener
        tipOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip selectedChip = findViewById(checkedId);
                String tipText = selectedChip.getText().toString();
                tipPercentage = Double.parseDouble(tipText.replace("%", ""));
                updateTotals();
            }
        });

        // Place order button listener
        placeOrderButton.setOnClickListener(v -> placeOrder());
    }

    private void loadCartItems() {
        // Get cart items from intent
        Serializable items = getIntent().getSerializableExtra("cartItems");
        if (items instanceof ArrayList<?>) {
            cartItems = (ArrayList<CartItem>) items;
        } else {
            cartItems = new ArrayList<>();
            Log.e(TAG, "Failed to get cart items from intent");
        }
        subtotal = getIntent().getDoubleExtra("subtotal", 0.0);

        Log.d(TAG, "Received " + (cartItems != null ? cartItems.size() : 0) + " items");
        Log.d(TAG, "Subtotal: " + subtotal);
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartAdapter);
        cartAdapter.setItems(cartItems);
    }

    private void updateTotals() {
        // Calculate tip amount
        double tipAmount = (subtotal * tipPercentage) / 100.0;
        
        // Update UI
        subtotalText.setText(String.format("$%.2f", subtotal));
        tipAmountText.setText(String.format("$%.2f", tipAmount));
        
        // Calculate total
        double total = subtotal + tipAmount;
        if (isDelivery) {
            total += DELIVERY_FEE;
        }
        
        totalText.setText(String.format("$%.2f", total));
    }

    private boolean validateOrder() {
        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isDelivery) {
            String address = streetAddressInput.getText().toString().trim();
            if (TextUtils.isEmpty(address)) {
                Toast.makeText(this, "Please provide a delivery address", Toast.LENGTH_SHORT).show();
                streetAddressInput.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void placeOrder() {
        if (!validateOrder()) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders");
        String orderId = ordersRef.push().getKey();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", userId);
        orderData.put("items", cartItems);
        orderData.put("subtotal", subtotal);
        orderData.put("tipPercentage", tipPercentage);
        orderData.put("tipAmount", (subtotal * tipPercentage) / 100.0);
        orderData.put("isDelivery", isDelivery);
        orderData.put("deliveryFee", isDelivery ? DELIVERY_FEE : 0);
        orderData.put("total", calculateTotal());
        orderData.put("status", "pending");
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("paymentMethod", paymentMethodGroup.getCheckedRadioButtonId() == R.id.cardPayment ? "card" : "cod");

        if (isDelivery) {
            Map<String, String> deliveryAddress = new HashMap<>();
            deliveryAddress.put("street", streetAddressInput.getText().toString().trim());
            deliveryAddress.put("unit", unitInput.getText().toString().trim());
            deliveryAddress.put("instructions", instructionsInput.getText().toString().trim());
            orderData.put("deliveryAddress", deliveryAddress);
        }

        ordersRef.child(orderId).setValue(orderData)
                .addOnSuccessListener(aVoid -> {
                    // Clear cart after successful order
                    DatabaseReference cartRef = FirebaseDatabase.getInstance()
                            .getReference("customers")
                            .child(userId)
                            .child("cart");
                    cartRef.removeValue();

                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private double calculateTotal() {
        double tipAmount = (subtotal * tipPercentage) / 100.0;
        double total = subtotal + tipAmount;
        if (isDelivery) {
            total += DELIVERY_FEE;
        }
        return total;
    }

    @Override
    public void onUpdateQuantity(String itemId, int newQuantity) {
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                if (item.getId().equals(itemId)) {
                    item.setQuantity(newQuantity);
                    calculateSubtotal();
                    updateTotals();
                    break;
                }
            }
        }
    }

    @Override
    public void onRemoveItem(String itemId) {
        if (cartItems != null) {
            cartItems.removeIf(item -> item.getId().equals(itemId));
            cartAdapter.setItems(cartItems);
            calculateSubtotal();
            updateTotals();
        }
    }

    private void calculateSubtotal() {
        subtotal = 0.0;
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                subtotal += item.getTotalPrice();
            }
        }
    }
}