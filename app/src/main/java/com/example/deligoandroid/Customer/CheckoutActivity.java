package com.example.deligoandroid.Customer;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.CartAdapter;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.CustomizationSelection;
import com.example.deligoandroid.Customer.Models.SelectedItem;
import com.example.deligoandroid.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private AutoCompleteTextView addressInput;
    private TextInputEditText unitInput;
    private TextInputEditText instructionsInput;
    private ChipGroup tipOptions;
    private View deliveryFeeRow;
    
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private Place selectedPlace;
    private AddressAdapter addressAdapter;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    private List<CartItem> cartItems;
    private double subtotal = 0.0;
    private double tipPercentage = 15.0;
    private boolean isDelivery = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Places API
        if (!Places.isInitialized()) {
            String apiKey = getString(R.string.google_maps_api_key);
            Log.d(TAG, "Initializing Places API with key: " + (apiKey.isEmpty() ? "EMPTY" : "NOT EMPTY"));
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();
        
        setContentView(R.layout.activity_checkout);

        initializeViews();
        setupAddressAutocomplete();
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
        addressInput = findViewById(R.id.addressInput);
        unitInput = findViewById(R.id.unitInput);
        instructionsInput = findViewById(R.id.instructionsInput);
        
        // Initialize address adapter
        addressAdapter = new AddressAdapter(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        addressInput.setAdapter(addressAdapter);
        
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

    private void setupAddressAutocomplete() {
        addressInput.setThreshold(3);
        
        // Set dropdown properties
        addressInput.setDropDownBackgroundResource(android.R.color.white);
        addressInput.setDropDownVerticalOffset(10);
        
        addressInput.addTextChangedListener(new android.text.TextWatcher() {
            private android.os.Handler handler = new android.os.Handler();
            private Runnable runnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                
                runnable = () -> {
                    if (s.length() >= 3) {
                        Log.d(TAG, "Getting predictions for: " + s.toString());
                        getAddressPredictions(s.toString());
                    }
                };
                
                // Add a small delay to avoid too many API calls
                handler.postDelayed(runnable, 300);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        addressInput.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = addressAdapter.getItem(position);
            if (prediction != null) {
                Log.d(TAG, "Selected prediction: " + prediction.getFullText(null));
                addressInput.setText(prediction.getFullText(null));
                
                // Get the place details
                List<Place.Field> placeFields = Arrays.asList(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.LAT_LNG
                );
                
                com.google.android.libraries.places.api.net.FetchPlaceRequest request = 
                    com.google.android.libraries.places.api.net.FetchPlaceRequest.builder(prediction.getPlaceId(), placeFields)
                    .setSessionToken(sessionToken)
                    .build();

                placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                    Place place = response.getPlace();
                    selectedPlace = place;
                    Log.d(TAG, "Place found: " + place.getName() + ", " + place.getAddress());
                    // Hide dropdown after selection
                    addressInput.dismissDropDown();
                }).addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                        Toast.makeText(CheckoutActivity.this, 
                            "Error fetching place details", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void getAddressPredictions(String query) {
        // Create a new session token if null
        if (sessionToken == null) {
            sessionToken = AutocompleteSessionToken.newInstance();
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setCountry("CA")
                .setTypeFilter(TypeFilter.ADDRESS)
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            Log.d(TAG, "Got " + response.getAutocompletePredictions().size() + " predictions");
            predictions.clear();
            predictions.addAll(response.getAutocompletePredictions());
            addressAdapter.clear();
            addressAdapter.addAll(predictions);
            addressAdapter.notifyDataSetChanged();
            
            if (predictions.size() > 0) {
                addressInput.showDropDown();
            }
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Place not found: " + apiException.getStatusCode());
                Log.e(TAG, "Error message: " + apiException.getMessage());
                Toast.makeText(CheckoutActivity.this, 
                    "Error getting address suggestions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AddressAdapter extends ArrayAdapter<AutocompletePrediction> {
        public AddressAdapter(Context context, int resource, List<AutocompletePrediction> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }

            TextView textView = (TextView) convertView;
            AutocompletePrediction item = getItem(position);
            if (item != null) {
                String primaryText = item.getPrimaryText(null).toString();
                String secondaryText = item.getSecondaryText(null).toString();
                textView.setText(primaryText + "\n" + secondaryText);
                textView.setSingleLine(false);
                textView.setLines(2);
            }

            return convertView;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = predictions;
                    results.count = predictions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }
    }

    private void setupListeners() {
        // Delivery option listener
        deliveryOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isDelivery = checkedId == R.id.deliveryOption;
            deliveryAddressSection.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            deliveryFeeRow.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            if (!isDelivery) {
                selectedPlace = null;
                if (addressInput != null) {
                    addressInput.setText("");
                }
            }
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
        Log.d(TAG, "Calculating totals - Subtotal: " + subtotal + ", Tip: " + tipAmount);
        
        // Update UI
        subtotalText.setText(String.format("$%.2f", subtotal));
        tipAmountText.setText(String.format("$%.2f", tipAmount));
        
        // Calculate total
        double total = subtotal + tipAmount;
        if (isDelivery) {
            total += DELIVERY_FEE;
            Log.d(TAG, "Added delivery fee. New total: " + total);
        }
        
        totalText.setText(String.format("$%.2f", total));
        Log.d(TAG, "Final total: " + total);
    }

    private boolean validateOrder() {
        if (cartItems == null || cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isDelivery) {
            String address = addressInput.getText().toString().trim();
            if (TextUtils.isEmpty(address) || selectedPlace == null) {
                Toast.makeText(this, "Please select a valid delivery address", Toast.LENGTH_SHORT).show();
                addressInput.requestFocus();
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
        String orderId = java.util.UUID.randomUUID().toString().toUpperCase();

        // Calculate final amounts
        double tipAmount = (subtotal * tipPercentage) / 100.0;
        double total = subtotal + tipAmount;
        if (isDelivery) {
            total += DELIVERY_FEE;
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("id", orderId);
        orderData.put("userId", userId);
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("deliveryFee", isDelivery ? DELIVERY_FEE : 0);
        orderData.put("deliveryOption", isDelivery ? "delivery" : "pickup");
        orderData.put("status", "pending");
        orderData.put("subtotal", subtotal);
        orderData.put("tipAmount", tipAmount);
        orderData.put("tipPercentage", tipPercentage);
        orderData.put("total", total);
        orderData.put("paymentMethod", paymentMethodGroup.getCheckedRadioButtonId() == R.id.cardPayment ? 
            "Credit Card" : "Cash on Delivery");

        // Get restaurant ID from the first item
        if (cartItems != null && !cartItems.isEmpty()) {
            CartItem firstItem = cartItems.get(0);
            String restaurantId = firstItem.getRestaurantId();
            Log.d(TAG, "Getting restaurant ID from first cart item: " + firstItem.getName());
            Log.d(TAG, "Restaurant ID: " + restaurantId);
            if (restaurantId == null || restaurantId.isEmpty()) {
                Log.e(TAG, "Restaurant ID is missing or empty!");
                Toast.makeText(this, "Error: Restaurant ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }
            orderData.put("restaurantId", restaurantId);
        }

        // Format items according to the required structure
        List<Map<String, Object>> formattedItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> formattedItem = new HashMap<>();
            formattedItem.put("id", item.getId());
            formattedItem.put("menuItemId", item.getMenuItemId());
            formattedItem.put("name", item.getName());
            formattedItem.put("description", item.getDescription());
            formattedItem.put("price", item.getPrice());
            formattedItem.put("imageURL", item.getImageURL());
            formattedItem.put("quantity", item.getQuantity());
            
            // Add customizations if they exist
            Map<String, List<CustomizationSelection>> customizationsMap = item.getCustomizations();
            if (customizationsMap != null && !customizationsMap.isEmpty()) {
                List<Map<String, Object>> formattedCustomizations = new ArrayList<>();
                
                for (Map.Entry<String, List<CustomizationSelection>> entry : customizationsMap.entrySet()) {
                    for (CustomizationSelection selection : entry.getValue()) {
                        for (SelectedItem selectedItem : selection.getSelectedItems()) {
                            Map<String, Object> customization = new HashMap<>();
                            customization.put("name", entry.getKey());
                            customization.put("choice", selectedItem.getName());
                            customization.put("price", selectedItem.getPrice());
                            formattedCustomizations.add(customization);
                        }
                    }
                }
                
                if (!formattedCustomizations.isEmpty()) {
                    Log.d(TAG, "Adding customizations for item: " + item.getName() + ", count: " + formattedCustomizations.size());
                    formattedItem.put("customizations", formattedCustomizations);
                }
            }
            
            formattedItems.add(formattedItem);
        }
        orderData.put("items", formattedItems);

        if (isDelivery && selectedPlace != null) {
            // Create address object
            Map<String, Object> addressData = new HashMap<>();
            addressData.put("street", selectedPlace.getAddress());
            if (!TextUtils.isEmpty(unitInput.getText())) {
                addressData.put("unit", unitInput.getText().toString().trim());
            }
            if (!TextUtils.isEmpty(instructionsInput.getText())) {
                addressData.put("instructions", instructionsInput.getText().toString().trim());
            }
            orderData.put("address", addressData);
            
            // Add coordinates separately
            orderData.put("latitude", selectedPlace.getLatLng().latitude);
            orderData.put("longitude", selectedPlace.getLatLng().longitude);
        }

        // Add order to Firebase
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

    @Override
    public void onUpdateQuantity(String itemId, int newQuantity) {
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                if (item.getId().equals(itemId)) {
                    Log.d(TAG, "Updating quantity for item: " + item.getName() + " from " + item.getQuantity() + " to " + newQuantity);
                    item.setQuantity(newQuantity);
                    item.setTotalPrice(item.getPrice() * newQuantity); // Update total price
                    cartAdapter.notifyDataSetChanged(); // Notify adapter of the change
                    calculateSubtotal(); // Recalculate subtotal
                    updateTotals(); // Update all totals including tip and delivery fee
                    
                    // Update the quantity in Firebase
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference cartRef = FirebaseDatabase.getInstance()
                            .getReference("customers")
                            .child(userId)
                            .child("cart")
                            .child(itemId);
                    
                    // Update the entire item in Firebase to ensure consistency
                    cartRef.setValue(item)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully updated item in Firebase");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update item in Firebase: " + e.getMessage());
                                Toast.makeText(this, "Failed to update quantity", Toast.LENGTH_SHORT).show();
                            });
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

            // Remove from Firebase
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference cartRef = FirebaseDatabase.getInstance()
                    .getReference("customers")
                    .child(userId)
                    .child("cart")
                    .child(itemId);
            
            cartRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully removed item from Firebase");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove item from Firebase: " + e.getMessage());
                        Toast.makeText(this, "Failed to remove item", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void calculateSubtotal() {
        subtotal = 0.0;
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                double itemTotal = item.getPrice() * item.getQuantity();
                Log.d(TAG, "Item: " + item.getName() + ", Price: " + item.getPrice() + 
                      ", Quantity: " + item.getQuantity() + ", Total: " + itemTotal);
                item.setTotalPrice(itemTotal); // Update item's total price
                subtotal += itemTotal;
            }
        }
        Log.d(TAG, "New subtotal: " + subtotal);
    }
}