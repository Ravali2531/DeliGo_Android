package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SpecialDiscountsActivity extends AppCompatActivity {

    private TextView selectedDiscountText;
    private CardView dropdownMenuCard;
    private View progressBar;
    private LinearLayout discountSelectorLayout;
    
    private TextView discount10, discount30, discount40, discount50;
    private LinearLayout discount20;
    
    private DatabaseReference restaurantRef;
    private String restaurantId;
    private int currentDiscount = 20; // Default to 20%
    
    private boolean dropdownVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_special_discounts);
        
        initializeViews();
        initializeFirebase();
        setupClickListeners();
        loadCurrentDiscount();
    }
    
    private void initializeViews() {
        selectedDiscountText = findViewById(R.id.selectedDiscountText);
        dropdownMenuCard = findViewById(R.id.dropdownMenuCard);
        progressBar = findViewById(R.id.progressBar);
        discountSelectorLayout = findViewById(R.id.discountSelectorLayout);
        
        discount10 = findViewById(R.id.discount10);
        discount20 = findViewById(R.id.discount20);
        discount30 = findViewById(R.id.discount30);
        discount40 = findViewById(R.id.discount40);
        discount50 = findViewById(R.id.discount50);
        
        ImageButton backButton = findViewById(R.id.backButton);
        Button doneButton = findViewById(R.id.doneButton);
        
        backButton.setOnClickListener(v -> onBackPressed());
        doneButton.setOnClickListener(v -> saveDiscountAndExit());
    }
    
    private void initializeFirebase() {
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        restaurantRef = FirebaseDatabase.getInstance().getReference("restaurants")
                .child(restaurantId);
    }
    
    private void setupClickListeners() {
        // Toggle dropdown when selector is clicked
        discountSelectorLayout.setOnClickListener(v -> toggleDropdown());
        
        // Set up click listeners for discount options
        discount10.setOnClickListener(v -> selectDiscount(10));
        discount20.setOnClickListener(v -> selectDiscount(20));
        discount30.setOnClickListener(v -> selectDiscount(30));
        discount40.setOnClickListener(v -> selectDiscount(40));
        discount50.setOnClickListener(v -> selectDiscount(50));
    }
    
    private void loadCurrentDiscount() {
        showLoading(true);
        
        restaurantRef.child("discount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showLoading(false);
                
                if (dataSnapshot.exists()) {
                    Long value = dataSnapshot.getValue(Long.class);
                    if (value != null) {
                        currentDiscount = value.intValue();
                        updateDiscountDisplay();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(SpecialDiscountsActivity.this, 
                        "Failed to load discount: " + databaseError.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void toggleDropdown() {
        dropdownVisible = !dropdownVisible;
        dropdownMenuCard.setVisibility(dropdownVisible ? View.VISIBLE : View.GONE);
    }
    
    private void selectDiscount(int discount) {
        currentDiscount = discount;
        updateDiscountDisplay();
        toggleDropdown(); // Hide dropdown after selection
    }
    
    private void updateDiscountDisplay() {
        selectedDiscountText.setText(currentDiscount + "%");
        
        // Update checkmark visibility based on selection
        discount20.findViewById(R.id.discount20).setSelected(currentDiscount == 20);
        // For other options, we would add checkmarks dynamically if needed
    }
    
    private void saveDiscountAndExit() {
        showLoading(true);
        
        restaurantRef.child("discount").setValue(currentDiscount)
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                Toast.makeText(SpecialDiscountsActivity.this, 
                        "Discount updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                Toast.makeText(SpecialDiscountsActivity.this,
                        "Failed to update discount: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        discountSelectorLayout.setEnabled(!show);
    }
} 