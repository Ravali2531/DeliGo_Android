package com.example.deligoandroid.Restaurant.Activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationItem;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.example.deligoandroid.databinding.ActivityAddMenuItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class EditMenuItemActivity extends AppCompatActivity {
    private static final String TAG = "EditMenuItemActivity";
    private ActivityAddMenuItemBinding binding;
    private Uri selectedImageUri;
    private String itemId;
    private String currentImageUrl;
    private ArrayList<CustomizationOption> customizationOptions = new ArrayList<>();
    private boolean isUploading = false;
    private boolean imageChanged = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                binding.itemImage.setImageURI(selectedImageUri);
                binding.addImageText.setVisibility(View.GONE);
                imageChanged = true;
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Starting onCreate");

            binding = ActivityAddMenuItemBinding.inflate(getLayoutInflater());
            if (binding == null) {
                Log.e(TAG, "View binding failed to initialize");
                Toast.makeText(this, "Error: Failed to initialize view", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            setContentView(binding.getRoot());

            itemId = getIntent().getStringExtra("itemId");
            Log.d(TAG, "Received itemId: " + itemId);

            if (itemId == null) {
                Log.e(TAG, "Item ID not found in intent");
                Toast.makeText(this, "Error: Item ID not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            setupToolbar();
            setupSpinner();
            setupClickListeners();
            loadMenuItem();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing edit screen: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Menu Item");
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.food_categories, android.R.layout.simple_dropdown_item_1line);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.itemImageLayout.setOnClickListener(v -> openImagePicker());
        binding.addCustomizationButton.setOnClickListener(v -> showAddCustomizationDialog());
        binding.saveButton.setOnClickListener(v -> updateMenuItem());
        
        // Show and setup delete button
        binding.deleteButton.setVisibility(View.VISIBLE);
        binding.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadMenuItem() {
        try {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "Loading menu item for user: " + userId + ", itemId: " + itemId);

            DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
                .child("restaurants")
                .child(userId)
                .child("menu_items")
                .child(itemId);

            Log.d(TAG, "Database reference path: " + menuRef.toString());

            menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        Log.d(TAG, "Data snapshot exists: " + snapshot.exists());
                        if (!snapshot.exists()) {
                            Log.e(TAG, "Item not found in database");
                            Toast.makeText(EditMenuItemActivity.this,
                                "Error: Item not found in database", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // Manually construct the MenuItemModel
                        MenuItemModel item = new MenuItemModel();
                        item.setId(snapshot.getKey());

                        if (snapshot.hasChild("name")) {
                            item.setName(snapshot.child("name").getValue(String.class));
                        }
                        if (snapshot.hasChild("description")) {
                            item.setDescription(snapshot.child("description").getValue(String.class));
                        }
                        if (snapshot.hasChild("price")) {
                            Double price = snapshot.child("price").getValue(Double.class);
                            if (price != null) {
                                item.setPrice(price);
                            }
                        }
                        if (snapshot.hasChild("imageURL")) {
                            item.setImageURL(snapshot.child("imageURL").getValue(String.class));
                        }
                        if (snapshot.hasChild("category")) {
                            item.setCategory(snapshot.child("category").getValue(String.class));
                        }
                        if (snapshot.hasChild("isAvailable")) {
                            Boolean isAvailable = snapshot.child("isAvailable").getValue(Boolean.class);
                            item.setAvailable(isAvailable != null ? isAvailable : true);
                        }

                        // Clear existing customization options
                        customizationOptions.clear();

                        // Handle customization options if they exist
                        if (snapshot.hasChild("customizationOptions")) {
                            Log.d(TAG, "Found customizationOptions in snapshot");
                            DataSnapshot customizationsSnapshot = snapshot.child("customizationOptions");
                            
                            // Iterate through each customization option
                            for (DataSnapshot optionSnapshot : customizationsSnapshot.getChildren()) {
                                try {
                                    CustomizationOption option = new CustomizationOption();
                                    
                                    // Get the ID
                                    option.setId(optionSnapshot.child("id").getValue(String.class));
                                    Log.d(TAG, "Processing customization option with ID: " + option.getId());
                                    
                                    // Get the name
                                    String name = optionSnapshot.child("name").getValue(String.class);
                                    option.setName(name);
                                    Log.d(TAG, "Name: " + name);
                                    
                                    // Get the type
                                    String type = optionSnapshot.child("type").getValue(String.class);
                                    option.setType(type);
                                    Log.d(TAG, "Type: " + type);
                                    
                                    // Get required status
                                    Boolean required = optionSnapshot.child("required").getValue(Boolean.class);
                                    option.setRequired(required != null ? required : false);
                                    Log.d(TAG, "Required: " + required);
                                    
                                    // Get max selections
                                    Long maxSelections = optionSnapshot.child("maxSelections").getValue(Long.class);
                                    if (maxSelections != null) {
                                        option.setMaxSelections(maxSelections.intValue());
                                        Log.d(TAG, "Max Selections: " + maxSelections);
                                    }
                                    
                                    // Get options list
                                    List<String> options = new ArrayList<>();
                                    if (optionSnapshot.hasChild("options")) {
                                        for (DataSnapshot optSnapshot : optionSnapshot.child("options").getChildren()) {
                                            String opt = optSnapshot.getValue(String.class);
                                            if (opt != null) {
                                                options.add(opt);
                                                Log.d(TAG, "Added option: " + opt);
                                            }
                                        }
                                    }
                                    option.setOptions(options);
                                    
                                    // Add the option to our list
                                    customizationOptions.add(option);
                                    Log.d(TAG, "Successfully added customization option: " + name);
                                    
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing customization option", e);
                                }
                            }
                        }

                        Log.d(TAG, "Total customization options loaded: " + customizationOptions.size());

                        if (item.getName() != null && item.getPrice() > 0) {
                            Log.d(TAG, "Successfully loaded item: " + item.getName());
                            populateFields(item);
                        } else {
                            Log.e(TAG, "Required fields missing");
                            Toast.makeText(EditMenuItemActivity.this,
                                "Error: Required fields missing", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing database data", e);
                        Toast.makeText(EditMenuItemActivity.this,
                            "Error processing item data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Database error: " + error.getMessage());
                    Toast.makeText(EditMenuItemActivity.this,
                        "Error loading item: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadMenuItem", e);
            Toast.makeText(this, "Error loading menu item: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateFields(MenuItemModel item) {
        binding.nameInput.setText(item.getName());
        binding.descriptionInput.setText(item.getDescription());
        binding.priceInput.setText(String.format("%.2f", item.getPrice()));
        
        // Set category
        binding.categorySpinner.setText(item.getCategory(), false);

        // Load image
        currentImageUrl = item.getImageURL();
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(this)
                .load(currentImageUrl)
                .into(binding.itemImage);
            binding.addImageText.setVisibility(View.GONE);
        }

        // Load customization options
        Log.d(TAG, "Starting to populate customization options. Count: " + customizationOptions.size());
        binding.customizationsContainer.removeAllViews();

        // Add a divider
        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2);
        dividerParams.setMargins(0, 16, 0, 16);
        binding.customizationsContainer.addView(divider, dividerParams);

        // Add header text
        TextView headerText = new TextView(this);
        headerText.setText("Customization Options (" + customizationOptions.size() + ")");
        headerText.setTextSize(18);
        headerText.setTextColor(getResources().getColor(android.R.color.black));
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setPadding(0, 16, 0, 16);
        binding.customizationsContainer.addView(headerText);

        if (customizationOptions.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No customization options available");
            emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyText.setPadding(16, 8, 16, 8);
            binding.customizationsContainer.addView(emptyText);
            Log.d(TAG, "Added empty state text view");
            return;
        }

        // Add each customization option
        for (CustomizationOption option : customizationOptions) {
            if (option == null) {
                Log.e(TAG, "Null customization option found!");
                continue;
            }
            Log.d(TAG, "Processing customization: " + option.getName());
            
            // Add a small divider between items
            View itemDivider = new View(this);
            itemDivider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            LinearLayout.LayoutParams itemDividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
            itemDividerParams.setMargins(16, 0, 16, 0);
            binding.customizationsContainer.addView(itemDivider, itemDividerParams);
            
            addCustomizationToUI(option);
        }

        // Add final divider
        View finalDivider = new View(this);
        finalDivider.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        LinearLayout.LayoutParams finalDividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2);
        finalDividerParams.setMargins(0, 16, 0, 16);
        binding.customizationsContainer.addView(finalDivider, finalDividerParams);

        binding.customizationsContainer.requestLayout();
    }

    private void addCustomizationToUI(CustomizationOption customization) {
        try {
            View view = getLayoutInflater().inflate(
                R.layout.item_customization_option,
                binding.customizationsContainer,
                false
            );

            // Add card background and elevation
            view.setBackgroundResource(android.R.color.white);
            view.setElevation(4);
            
            TextView nameText = view.findViewById(R.id.customizationName);
            TextView typeText = view.findViewById(R.id.customizationType);
            ImageButton deleteButton = view.findViewById(R.id.deleteButton);

            if (nameText == null || typeText == null) {
                Log.e(TAG, "Failed to find views in inflated layout");
                return;
            }

            nameText.setText(customization.getName());
            
            // Build the type description
            StringBuilder typeBuilder = new StringBuilder();
            String type = customization.getType().toLowerCase();
            typeBuilder.append(type);
            
            if ("multiple".equals(type) && customization.getMaxSelections() > 0) {
                typeBuilder.append(" (max ").append(customization.getMaxSelections()).append(")");
            }
            
            if (customization.isRequired()) {
                typeBuilder.append(" - Required");
            }
            
            List<String> options = customization.getOptions();
            if (options != null && !options.isEmpty()) {
                typeBuilder.append(" - ").append(options.size()).append(" options");
            }
            
            typeText.setText(typeBuilder.toString());

            if (deleteButton != null) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    customizationOptions.remove(customization);
                    binding.customizationsContainer.removeView(view);
                });
            }

            // Add margins and padding
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);
            view.setPadding(16, 16, 16, 16);
            
            binding.customizationsContainer.addView(view, params);
            Log.d(TAG, "Added customization view: " + customization.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding customization to UI", e);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showAddCustomizationDialog() {
        // TODO: Implement customization dialog
        Toast.makeText(this, "Customization options coming soon", Toast.LENGTH_SHORT).show();
    }

    private void updateMenuItem() {
        String name = binding.nameInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        String priceStr = binding.priceInput.getText().toString().trim();
        String category = binding.categorySpinner.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isUploading) {
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            binding.priceInput.setError("Invalid price");
            return;
        }

        isUploading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.saveButton.setEnabled(false);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(userId)
            .child("menu_items")
            .child(itemId);

        if (selectedImageUri != null && imageChanged) {
            // Upload new image
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("menu_items")
                .child(itemId + ".jpg");

            // Delete old image if exists
            if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                StorageReference oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl);
                oldImageRef.delete();
            }

            storageRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return storageRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String imageUrl = task.getResult().toString();
                        updateMenuItemInDatabase(menuRef, name, description, price, category, imageUrl);
                    } else {
                        isUploading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        binding.saveButton.setEnabled(true);
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            // Update without changing image
            updateMenuItemInDatabase(menuRef, name, description, price, category, currentImageUrl);
        }
    }

    private void updateMenuItemInDatabase(DatabaseReference menuRef, String name, String description,
                                        double price, String category, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("category", category);
        updates.put("imageURL", imageUrl);
        updates.put("customizationOptions", customizationOptions);

        menuRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Menu item updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                isUploading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to update menu item", Toast.LENGTH_SHORT).show();
            });
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete this menu item? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteMenuItem())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteMenuItem() {
        if (itemId == null) {
            Toast.makeText(this, "Error: Item ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(userId)
            .child("menu_items")
            .child(itemId);

        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.deleteButton.setEnabled(false);
        binding.saveButton.setEnabled(false);

        // If there's an image URL, delete it from storage first
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Error deleting image file", task.getException());
                    }
                    // Continue with deleting the database entry regardless of image deletion result
                    deleteMenuItemFromDatabase(menuRef);
                });
        } else {
            // No image to delete, just delete the database entry
            deleteMenuItemFromDatabase(menuRef);
        }
    }

    private void deleteMenuItemFromDatabase(DatabaseReference menuRef) {
        menuRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Menu item deleted successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            })
            .addOnFailureListener(e -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.deleteButton.setEnabled(true);
                binding.saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to delete menu item: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
