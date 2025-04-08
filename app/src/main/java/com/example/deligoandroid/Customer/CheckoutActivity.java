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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.Serializable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity implements CartAdapter.CartItemListener {
    private static final String TAG = "CheckoutActivity";
    private static final double DELIVERY_FEE = 4.99;
    private static final String BACKEND_URL = "https://c8a9-70-26-192-244.ngrok-free.app"; // Replace with your backend URL
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51QLXl2L0kLdfcs5yhjcDuc0WAnDoZgIu1Ts88JhU7ZpGDDmkZ8X6mkhAnRuFuhYQLePpmWrcKXJby0qtvMiw6FVc00DTOLaHK5"; // Replace with your Stripe publishable key

    private RecyclerView cartItemsRecyclerView;
    private CartAdapter cartAdapter;
    private TextView subtotalText, totalText, tipAmountText, deliveryFeeAmount, discountText;
    private MaterialButton placeOrderButton;
    private RadioGroup deliveryOptionsGroup;
    private RadioGroup paymentMethodGroup;
    private View deliveryAddressSection;
    private AutoCompleteTextView addressInput;
    private TextInputEditText unitInput;
    private TextInputEditText instructionsInput;
    private ChipGroup tipOptions;
    private View deliveryFeeRow;
    private View tipSection;
    private View tipRow;
    private View discountRow;
    
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private Place selectedPlace;
    private AddressAdapter addressAdapter;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    private List<CartItem> cartItems;
    private double subtotal = 0.0;
    private double tipPercentage = 15.0;
    private boolean isDelivery = true;
    private String restaurantId = "";
    private int discountPercentage = 0;
    private double discountAmount = 0.0;

    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private String customerId;
    private String ephemeralKey;

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
        
        // Initialize Stripe
        PaymentConfiguration.init(getApplicationContext(), STRIPE_PUBLISHABLE_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
        
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
        tipSection = findViewById(R.id.tipSection);
        tipRow = findViewById(R.id.tipRow);
        tipOptions = findViewById(R.id.tipOptions);
        tipAmountText = findViewById(R.id.tipAmountText);
        
        // Set up tip options
        setupTipOptions();
        
        // Payment section
        paymentMethodGroup = findViewById(R.id.paymentMethodGroup);
        
        // Summary section
        subtotalText = findViewById(R.id.subtotalText);
        totalText = findViewById(R.id.totalText);
        deliveryFeeAmount = findViewById(R.id.deliveryFeeAmount);
        discountText = findViewById(R.id.discountText);
        deliveryFeeRow = findViewById(R.id.deliveryFeeRow);
        discountRow = findViewById(R.id.discountRow);
        
        // Place order button
        placeOrderButton = findViewById(R.id.placeOrderButton);
    }

    private void setupTipOptions() {
        // Clear any existing chips
        tipOptions.removeAllViews();
        
        // Define tip percentages
        int[] tipPercentages = {0, 10, 15, 20, 25};
        
        for (int percentage : tipPercentages) {
            Chip chip = new Chip(this);
            chip.setText(percentage + "%");
            chip.setCheckable(true);
            chip.setClickable(true);
            
            // Set 15% as default selected
            if (percentage == 15) {
                chip.setChecked(true);
            }
            
            tipOptions.addView(chip);
        }
        
        // Set up listener for tip selection
        tipOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip selectedChip = findViewById(checkedId);
                String tipText = selectedChip.getText().toString();
                tipPercentage = Double.parseDouble(tipText.replace("%", ""));
                Log.d(TAG, "Selected tip percentage: " + tipPercentage + "%");
                updateTotals();
            }
        });
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
            
            // Show/hide tip sections based on delivery option
            tipSection.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            tipRow.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            tipOptions.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
            
            if (!isDelivery) {
                selectedPlace = null;
                if (addressInput != null) {
                    addressInput.setText("");
                }
                // Reset tip to 0% for pickup
                tipPercentage = 0.0;
                for (int i = 0; i < tipOptions.getChildCount(); i++) {
                    Chip chip = (Chip) tipOptions.getChildAt(i);
                    if (chip.getText().toString().equals("0%")) {
                        chip.setChecked(true);
                        break;
                    }
                }
            } else {
                // Set default 15% tip for delivery
                tipPercentage = 15.0;
                for (int i = 0; i < tipOptions.getChildCount(); i++) {
                    Chip chip = (Chip) tipOptions.getChildAt(i);
                    if (chip.getText().toString().equals("15%")) {
                        chip.setChecked(true);
                        break;
                    }
                }
            }
            updateTotals();
        });

        // Payment method listener
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.cardPayment) {
                // Create payment intent when credit card is selected
                createPaymentIntent();
            }
        });

        // Place order button listener
        placeOrderButton.setOnClickListener(v -> {
            if (!validateOrder()) {
                return;
            }
            
            if (paymentMethodGroup.getCheckedRadioButtonId() == R.id.cardPayment) {
                // For card payment, create payment intent first
                createPaymentIntent();
            } else {
                // For cash on delivery, place order directly
                placeOrder();
            }
        });
    }

    private void loadCartItems() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference cartRef = FirebaseDatabase.getInstance()
                .getReference("customers")
                .child(userId)
                .child("cart");

        cartRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    cartItems = new ArrayList<>();
                    for (com.google.firebase.database.DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                        CartItem item = itemSnapshot.getValue(CartItem.class);
                        if (item != null) {
                            cartItems.add(item);
                            // Get the restaurant ID for fetching discount and checking status
                            if (restaurantId.isEmpty() && !item.getRestaurantId().isEmpty()) {
                                restaurantId = item.getRestaurantId();
                                fetchRestaurantDiscount(restaurantId);
                                checkRestaurantStatus(restaurantId);
                            }
                        }
                    }
                    if (!cartItems.isEmpty()) {
                        cartAdapter.setItems(cartItems);
                        calculateSubtotal();
                        updateTotals();
                    } else {
                        showEmptyCart();
                    }
                } else {
                    showEmptyCart();
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                Toast.makeText(CheckoutActivity.this, "Failed to load cart: " + 
                        databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRestaurantDiscount(String restaurantId) {
        if (restaurantId == null || restaurantId.isEmpty()) {
            return;
        }
        
        DatabaseReference discountRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("discount");
                
        discountRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long discountValue = snapshot.getValue(Long.class);
                    if (discountValue != null && discountValue > 0) {
                        discountPercentage = discountValue.intValue();
                        calculateDiscountAmount();
                        discountRow.setVisibility(View.VISIBLE);
                        updateTotals();
                    } else {
                        discountRow.setVisibility(View.GONE);
                        discountPercentage = 0;
                        discountAmount = 0;
                    }
                } else {
                    discountRow.setVisibility(View.GONE);
                    discountPercentage = 0;
                    discountAmount = 0;
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Error loading discount", error.toException());
                discountRow.setVisibility(View.GONE);
                discountPercentage = 0;
                discountAmount = 0;
            }
        });
    }

    private void calculateDiscountAmount() {
        if (discountPercentage > 0) {
            discountAmount = (subtotal * discountPercentage) / 100.0;
            discountText.setText(String.format("-$%.2f", discountAmount));
        } else {
            discountAmount = 0;
            discountText.setText("-$0.00");
        }
    }

    private void updateTotals() {
        subtotalText.setText(String.format("$%.2f", subtotal));
        
        // Calculate tip
        double tipAmount = (subtotal * tipPercentage) / 100.0;
        tipAmountText.setText(String.format("$%.2f", tipAmount));
        
        // Apply discount if available
        if (discountPercentage > 0) {
            calculateDiscountAmount();
            discountRow.setVisibility(View.VISIBLE);
            discountText.setText(String.format("-$%.2f", discountAmount));
        } else {
            discountRow.setVisibility(View.GONE);
        }
        
        // Calculate final total
        double total = subtotal + tipAmount - discountAmount;
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
            String address = addressInput.getText().toString().trim();
            if (TextUtils.isEmpty(address) || selectedPlace == null) {
                Toast.makeText(this, "Please select a valid delivery address", Toast.LENGTH_SHORT).show();
                addressInput.requestFocus();
                return false;
            }
        }

        return true;
    }

    private void createPaymentIntent() {
        double total = calculateTotal();
        long amount = Math.round(total * 100); // Convert to cents

        // Get user info
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userEmail == null) {
            Toast.makeText(this, "Error: User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create JSON for the request
        JSONObject paymentData = new JSONObject();
        try {
            paymentData.put("amount", amount);
            paymentData.put("currency", "cad");
            paymentData.put("email", userEmail);
            paymentData.put("userId", userId);
            
            Log.d(TAG, "Creating payment intent request");
            Log.d(TAG, "Amount: " + amount + " cents");
            Log.d(TAG, "Email: " + userEmail);
            Log.d(TAG, "User ID: " + userId);
            Log.d(TAG, "Backend URL: " + BACKEND_URL);
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment data", e);
            Toast.makeText(this, "Error preparing payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create the request
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, paymentData.toString());
        Request request = new Request.Builder()
                .url(BACKEND_URL + "/create-payment-intent")
                .post(body)
                .build();

        Log.d(TAG, "Sending request to: " + request.url());
        Log.d(TAG, "Request body: " + paymentData.toString());

        // Make the request
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(CheckoutActivity.this, 
                        "Error creating payment intent", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating payment intent", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("error", "Unknown error occurred");
                            Toast.makeText(CheckoutActivity.this, 
                                "Payment Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Payment Error: " + errorMessage);
                            Log.e(TAG, "Response Code: " + response.code());
                            Log.e(TAG, "Response Body: " + responseBody);
                        } catch (Exception e) {
                            Toast.makeText(CheckoutActivity.this, 
                                "Payment Error: " + response.code() + " - " + response.message(), 
                                Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error parsing error response", e);
                            Log.e(TAG, "Response Code: " + response.code());
                            Log.e(TAG, "Response Body: " + responseBody);
                        }
                    });
                    return;
                }

                try {
                    JSONObject responseData = new JSONObject(responseBody);
                    
                    // Check if we have all required fields
                    if (!responseData.has("clientSecret")) {
                        runOnUiThread(() -> {
                            Toast.makeText(CheckoutActivity.this, 
                                "Error: Missing client secret in response", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Missing client secret in response: " + responseBody);
                        });
                        return;
                    }
                    
                    paymentIntentClientSecret = responseData.getString("clientSecret");
                    customerId = responseData.optString("customer");
                    ephemeralKey = responseData.optString("ephemeralKey");
                    
                    Log.d(TAG, "Successfully created payment intent");
                    Log.d(TAG, "Client Secret: " + (paymentIntentClientSecret != null ? "Present" : "Missing"));
                    Log.d(TAG, "Customer ID: " + (customerId != null ? "Present" : "Missing"));
                    Log.d(TAG, "Ephemeral Key: " + (ephemeralKey != null ? "Present" : "Missing"));
                    
                    runOnUiThread(() -> presentPaymentSheet());
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CheckoutActivity.this, 
                            "Error parsing payment response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error parsing payment response", e);
                        Log.e(TAG, "Response Body: " + responseBody);
                    });
                }
            }
        });
    }

    private void presentPaymentSheet() {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("DeliGo")
                .defaultBillingDetails(new PaymentSheet.BillingDetails())
                .allowsDelayedPaymentMethods(true)
                .build();
                
        paymentSheet.presentWithPaymentIntent(
                paymentIntentClientSecret,
                configuration
        );
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            // Payment successful, proceed with order placement
            placeOrder();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Toast.makeText(this, 
                "Payment failed: " + ((PaymentSheetResult.Failed) paymentSheetResult).getError(),
                Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateTotal() {
        double total = subtotal;
        if (isDelivery) {
            total += (subtotal * tipPercentage) / 100.0;
            total += DELIVERY_FEE;
        }
        return total;
    }

    private void placeOrder() {
        if (!validateOrder()) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // First get the customer's details
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference("customers")
                .child(userId);
                
        customerRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot customerSnapshot = task.getResult();
                String phoneNumber = customerSnapshot.child("phone").getValue(String.class);
                String customerName = customerSnapshot.child("fullName").getValue(String.class);
                
                // Get restaurantId from the first cart item
                if (cartItems.isEmpty()) {
                    Toast.makeText(this, "Error: No items in cart", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String restaurantId = cartItems.get(0).getRestaurantId();
                
                // Check if restaurant is open
                DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                        .getReference("restaurants")
                        .child(restaurantId)
                        .child("isOpen");
                        
                restaurantRef.get().addOnCompleteListener(restaurantTask -> {
                    if (restaurantTask.isSuccessful()) {
                        Boolean isOpen = restaurantTask.getResult().getValue(Boolean.class);
                        boolean isScheduledOrder = isOpen != null && !isOpen;
                        
                        // Determine the order reference based on restaurant status
                        String orderRefPath = isScheduledOrder ? "scheduled_orders" : "orders";
                        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference(orderRefPath);
                        String orderId = java.util.UUID.randomUUID().toString().toUpperCase();

                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("customerId", userId);
                        orderData.put("userId", userId);
                        orderData.put("customerName", customerName);
                        orderData.put("customerPhone", phoneNumber);
                        orderData.put("createdAt", System.currentTimeMillis());
                        orderData.put("updatedAt", System.currentTimeMillis());
                        orderData.put("deliveryFee", isDelivery ? DELIVERY_FEE : 0);
                        orderData.put("deliveryOption", isDelivery ? "delivery" : "pickup");
                        orderData.put("status", isScheduledOrder ? "scheduled" : "pending");
                        orderData.put("order_status", "pending");
                        orderData.put("restaurantId", restaurantId);
                        
                        // Add payment method
                        String paymentMethod = paymentMethodGroup.getCheckedRadioButtonId() == R.id.cardPayment ? 
                            "Credit Card" : "Cash on Delivery";
                        orderData.put("paymentMethod", paymentMethod);
                        
                        // Calculate final amounts
                        double tipAmount = (subtotal * tipPercentage) / 100.0;
                        
                        // Add discount information if applicable
                        if (discountPercentage > 0) {
                            orderData.put("discountPercentage", discountPercentage);
                            orderData.put("discountAmount", discountAmount);
                        }
                        
                        double total = subtotal + tipAmount - discountAmount;
                        if (isDelivery) {
                            total += DELIVERY_FEE;
                        }
                        
                        orderData.put("subtotal", subtotal);
                        orderData.put("tipAmount", tipAmount);
                        orderData.put("tipPercentage", tipPercentage);
                        orderData.put("total", total);

                        // Add delivery location if delivery is selected
                        if (isDelivery && selectedPlace != null && selectedPlace.getLatLng() != null) {
                            orderData.put("latitude", selectedPlace.getLatLng().latitude);
                            orderData.put("longitude", selectedPlace.getLatLng().longitude);
                        }

                        // Format items with proper customizations structure
                        Map<String, Object> items = new HashMap<>();
                        for (int i = 0; i < cartItems.size(); i++) {
                            CartItem item = cartItems.get(i);
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("id", item.getId());
                            itemData.put("menuItemId", item.getMenuItemId());
                            itemData.put("name", item.getName());
                            itemData.put("description", item.getDescription());
                            itemData.put("price", item.getPrice());
                            itemData.put("imageURL", item.getImageURL());
                            itemData.put("quantity", item.getQuantity());
                            itemData.put("specialInstructions", item.getSpecialInstructions() != null ? item.getSpecialInstructions() : "");
                            itemData.put("totalPrice", item.getTotalPrice());
                            
                            // Handle customizations with proper structure
                            if (item.getCustomizations() != null && !item.getCustomizations().isEmpty()) {
                                Map<String, Object> customizations = new HashMap<>();
                                
                                for (Map.Entry<String, List<CustomizationSelection>> entry : item.getCustomizations().entrySet()) {
                                    String optionId = entry.getKey();
                                    List<CustomizationSelection> selections = entry.getValue();
                                    
                                    Map<String, Object> customizationData = new HashMap<>();
                                    for (int j = 0; j < selections.size(); j++) {
                                        CustomizationSelection selection = selections.get(j);
                                        Map<String, Object> selectionData = new HashMap<>();
                                        selectionData.put("optionId", optionId);
                                        selectionData.put("optionName", selection.getOptionName());
                                        
                                        // Handle selected items
                                        if (selection.getSelectedItems() != null) {
                                            Map<String, Object> selectedItemsMap = new HashMap<>();
                                            for (int k = 0; k < selection.getSelectedItems().size(); k++) {
                                                SelectedItem selectedItem = selection.getSelectedItems().get(k);
                                                Map<String, Object> selectedItemData = new HashMap<>();
                                                selectedItemData.put("id", selectedItem.getId());
                                                selectedItemData.put("name", selectedItem.getName());
                                                selectedItemData.put("price", selectedItem.getPrice());
                                                selectedItemsMap.put(String.valueOf(k), selectedItemData);
                                            }
                                            selectionData.put("selectedItems", selectedItemsMap);
                                        }
                                        customizationData.put(String.valueOf(j), selectionData);
                                    }
                                    customizations.put(optionId, customizationData);
                                }
                                itemData.put("customizations", customizations);
                            }
                            items.put(String.valueOf(i), itemData);
                        }
                        orderData.put("items", items);

                        // Save the order
                        ordersRef.child(orderId).setValue(orderData)
                            .addOnSuccessListener(aVoid -> {
                                // Clear the cart
                                DatabaseReference cartRef = FirebaseDatabase.getInstance()
                                    .getReference("customers")
                                    .child(userId)
                                    .child("cart");
                                
                                cartRef.removeValue()
                                    .addOnSuccessListener(cartVoid -> {
                                        Toast.makeText(CheckoutActivity.this, 
                                            isScheduledOrder ? "Order scheduled successfully!" : "Order placed successfully!", 
                                            Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CheckoutActivity.this, 
                                            "Error clearing cart: " + e.getMessage(), 
                                            Toast.LENGTH_SHORT).show();
                                    });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CheckoutActivity.this, 
                                    "Error placing order: " + e.getMessage(), 
                                    Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        Toast.makeText(this, 
                            "Error checking restaurant status: " + restaurantTask.getException().getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, 
                    "Error getting customer details: " + task.getException().getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
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

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, this);
        cartItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItemsRecyclerView.setAdapter(cartAdapter);
    }

    private void showEmptyCart() {
        Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void checkRestaurantStatus(String restaurantId) {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("store_info")
                .child("isOpen");
                
        restaurantRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOpen = snapshot.getValue(Boolean.class);
                    if (isOpen != null && !isOpen) {
                        // Restaurant is closed, update button text
                        placeOrderButton.setText("Schedule Order");
                    } else {
                        // Restaurant is open, use default text
                        placeOrderButton.setText("Place Order");
                    }
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Error checking restaurant status", error.toException());
            }
        });
    }
}