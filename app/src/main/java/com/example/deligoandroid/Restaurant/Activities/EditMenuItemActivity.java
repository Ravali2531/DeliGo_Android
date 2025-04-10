package com.example.deligoandroid.Restaurant.Activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.view.LayoutInflater;

import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationItem;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.example.deligoandroid.databinding.ActivityAddMenuItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;

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
                            
                            loadCustomizationOptions(customizationsSnapshot);
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

    private void loadCustomizationOptions(DataSnapshot customizationsSnapshot) {
        customizationOptions.clear();
        Log.d(TAG, "Starting to load customization options");
        Log.d(TAG, "Raw customization data: " + customizationsSnapshot.getValue());
        
        Iterable<DataSnapshot> optionsIterable = customizationsSnapshot.getChildren();
        
        for (DataSnapshot optionSnapshot : optionsIterable) {
            try {
                Log.d(TAG, "Processing option: " + optionSnapshot.getValue());
                
                // Get basic properties
                String name = optionSnapshot.child("name").getValue(String.class);
                String type = optionSnapshot.child("type").getValue(String.class);
                Boolean required = optionSnapshot.child("required").getValue(Boolean.class);
                Long maxSelections = optionSnapshot.child("maxSelections").getValue(Long.class);
                
                Log.d(TAG, String.format("Option details - name: %s, type: %s, required: %s, maxSelections: %s",
                    name, type, required, maxSelections));
                
                if (name == null || type == null) {
                    Log.e(TAG, "Missing required fields for customization option");
                    continue;
                }
                
                // Create CustomizationOption object
                CustomizationOption option = new CustomizationOption(name, type, required != null ? required : false);
                option.setMaxSelections(maxSelections != null ? maxSelections.intValue() : 1);
                
                // Get options
                List<CustomizationOption.CustomizationOptionItem> items = new ArrayList<>();
                DataSnapshot optionsSnapshot = optionSnapshot.child("options");
                if (optionsSnapshot.exists()) {
                    for (DataSnapshot optSnapshot : optionsSnapshot.getChildren()) {
                        String optName = optSnapshot.child("name").getValue(String.class);
                        Object priceObj = optSnapshot.child("price").getValue();
                        
                        if (optName != null && priceObj != null) {
                            double price = 0.0;
                            if (priceObj instanceof Long) {
                                price = ((Long) priceObj).doubleValue();
                            } else if (priceObj instanceof Double) {
                                price = (Double) priceObj;
                            }
                            option.addOption(optName, price);
                        }
                    }
                }
                
                if (!option.getOptions().isEmpty()) {
                    Log.d(TAG, "Adding customization option: " + option.getName() + " with " + option.getOptions().size() + " options");
                    customizationOptions.add(option);
                    addCustomizationToUI(option);
                } else {
                    Log.e(TAG, "Skipping customization option with no options: " + option.getName());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing customization option: " + e.getMessage());
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Finished loading customization options. Total loaded: " + customizationOptions.size());
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

    private void addCustomizationToUI(CustomizationOption option) {
        View view = getLayoutInflater().inflate(R.layout.item_customization_preview, binding.customizationsContainer, false);
        
        TextView nameText = view.findViewById(R.id.customizationName);
        TextView typeText = view.findViewById(R.id.customizationType);
        TextView optionsText = view.findViewById(R.id.optionsText);
        
        nameText.setText(option.getName());
        String typeDisplay = String.format("%s (%s, Max: %d)", 
            option.getType(), 
            option.isRequired() ? "Required" : "Optional",
            option.getMaxSelections());
        typeText.setText(typeDisplay);
        
        StringBuilder optionsStr = new StringBuilder();
        for (CustomizationOption.CustomizationOptionItem item : option.getOptions()) {
            optionsStr.append(String.format("%s ($%.2f)\n", item.getName(), item.getPrice()));
        }
        optionsText.setText(optionsStr.toString().trim());
        
        ImageButton deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            binding.customizationsContainer.removeView(view);
            customizationOptions.remove(option);
        });

        // Make the entire view clickable for editing
        view.setOnClickListener(v -> showEditCustomizationDialog(option, view));
        
        binding.customizationsContainer.addView(view);
    }

    private void showEditCustomizationDialog(CustomizationOption option, View optionView) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_customization, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);
        CheckBox requiredCheckbox = dialogView.findViewById(R.id.requiredCheckbox);
        TextInputEditText maxSelectionsInput = dialogView.findViewById(R.id.maxSelectionsInput);
        LinearLayout optionsContainer = dialogView.findViewById(R.id.optionsContainer);
        Button addOptionButton = dialogView.findViewById(R.id.addOptionButton);

        // Pre-fill existing values
        nameInput.setText(option.getName());
        typeGroup.check(option.getType().equals("single") ? 
            R.id.singleSelectionRadio : R.id.multipleSelectionRadio);
        requiredCheckbox.setChecked(option.isRequired());
        maxSelectionsInput.setText(String.valueOf(option.getMaxSelections()));

        // Pre-fill existing options
        for (CustomizationOption.CustomizationOptionItem item : option.getOptions()) {
            View optionItemView = getLayoutInflater().inflate(
                R.layout.item_customization_option_input, optionsContainer, false);
            TextInputEditText optionNameInput = optionItemView.findViewById(R.id.optionNameInput);
            TextInputEditText optionPriceInput = optionItemView.findViewById(R.id.optionPriceInput);
            ImageButton deleteButton = optionItemView.findViewById(R.id.deleteButton);

            optionNameInput.setText(item.getName());
            optionPriceInput.setText(String.format("%.2f", item.getPrice()));
            deleteButton.setOnClickListener(del -> optionsContainer.removeView(optionItemView));
            
            optionsContainer.addView(optionItemView);
        }

        // Add new option button functionality
        addOptionButton.setOnClickListener(v -> {
            View optionItemView = getLayoutInflater().inflate(
                R.layout.item_customization_option_input, optionsContainer, false);
            TextInputEditText optionNameInput = optionItemView.findViewById(R.id.optionNameInput);
            TextInputEditText optionPriceInput = optionItemView.findViewById(R.id.optionPriceInput);
            ImageButton deleteButton = optionItemView.findViewById(R.id.deleteButton);

            deleteButton.setOnClickListener(del -> optionsContainer.removeView(optionItemView));
            optionsContainer.addView(optionItemView);
        });

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit Customization Option")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String type = typeGroup.getCheckedRadioButtonId() == R.id.singleSelectionRadio ? 
                    "single" : "multiple";
                boolean isRequired = requiredCheckbox.isChecked();
                int maxSelections = 1;
                try {
                    String maxSelectionsStr = maxSelectionsInput.getText().toString().trim();
                    if (!maxSelectionsStr.isEmpty()) {
                        maxSelections = Integer.parseInt(maxSelectionsStr);
                    }
                } catch (NumberFormatException e) {
                    maxSelections = 1;
                }

                // Update the existing customization option
                option.setName(name);
                option.setType(type);
                option.setRequired(isRequired);
                option.setMaxSelections(maxSelections);
                option.clearOptions();

                // Add updated options
                for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                    View optionItemView = optionsContainer.getChildAt(i);
                    TextInputEditText optionNameInput = optionItemView.findViewById(R.id.optionNameInput);
                    TextInputEditText optionPriceInput = optionItemView.findViewById(R.id.optionPriceInput);

                    String optionName = optionNameInput.getText().toString().trim();
                    String priceStr = optionPriceInput.getText().toString().trim();

                    if (!optionName.isEmpty() && !priceStr.isEmpty()) {
                        try {
                            double price = Double.parseDouble(priceStr);
                            option.addOption(optionName, price);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid price for option: " + optionName);
                        }
                    }
                }

                // Update the UI
                TextView nameText = optionView.findViewById(R.id.customizationName);
                TextView typeText = optionView.findViewById(R.id.customizationType);
                TextView optionsText = optionView.findViewById(R.id.optionsText);

                nameText.setText(option.getName());
                String typeDisplay = String.format("%s (%s, Max: %d)", 
                    option.getType(), 
                    option.isRequired() ? "Required" : "Optional",
                    option.getMaxSelections());
                typeText.setText(typeDisplay);

                StringBuilder optionsStr = new StringBuilder();
                for (CustomizationOption.CustomizationOptionItem item : option.getOptions()) {
                    optionsStr.append(String.format("%s ($%.2f)\n", item.getName(), item.getPrice()));
                }
                optionsText.setText(optionsStr.toString().trim());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showAddCustomizationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_customization, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);
        CheckBox requiredCheckbox = dialogView.findViewById(R.id.requiredCheckbox);
        TextInputEditText maxSelectionsInput = dialogView.findViewById(R.id.maxSelectionsInput);
        LinearLayout optionsContainer = dialogView.findViewById(R.id.optionsContainer);
        Button addOptionButton = dialogView.findViewById(R.id.addOptionButton);

        List<Map<String, Object>> options = new ArrayList<>();

        addOptionButton.setOnClickListener(v -> {
            View optionView = getLayoutInflater().inflate(R.layout.item_customization_option_input, optionsContainer, false);
            TextInputEditText optionNameInput = optionView.findViewById(R.id.optionNameInput);
            TextInputEditText optionPriceInput = optionView.findViewById(R.id.optionPriceInput);
            ImageButton deleteButton = optionView.findViewById(R.id.deleteButton);

            deleteButton.setOnClickListener(del -> optionsContainer.removeView(optionView));
            optionsContainer.addView(optionView);
        });

        new MaterialAlertDialogBuilder(this)
            .setTitle("Add Customization Option")
            .setView(dialogView)
            .setPositiveButton("Add", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String type = typeGroup.getCheckedRadioButtonId() == R.id.singleSelectionRadio ? 
                    "single" : "multiple";
                boolean isRequired = requiredCheckbox.isChecked();
                int maxSelections = 1;
                try {
                    String maxSelectionsStr = maxSelectionsInput.getText().toString().trim();
                    if (!maxSelectionsStr.isEmpty()) {
                        maxSelections = Integer.parseInt(maxSelectionsStr);
                    }
                } catch (NumberFormatException e) {
                    maxSelections = 1;
                }

                // Create customization option
                CustomizationOption customization = new CustomizationOption(name, type, isRequired);
                customization.setMaxSelections(maxSelections);

                // Add options
                for (int i = 0; i < optionsContainer.getChildCount(); i++) {
                    View optionView = optionsContainer.getChildAt(i);
                    TextInputEditText optionNameInput = optionView.findViewById(R.id.optionNameInput);
                    TextInputEditText optionPriceInput = optionView.findViewById(R.id.optionPriceInput);

                    String optionName = optionNameInput.getText().toString().trim();
                    String priceStr = optionPriceInput.getText().toString().trim();

                    if (!optionName.isEmpty() && !priceStr.isEmpty()) {
                        try {
                            double price = Double.parseDouble(priceStr);
                            customization.addOption(optionName, price);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Invalid price for option: " + optionName);
                        }
                    }
                }

                customizationOptions.add(customization);
                addCustomizationToUI(customization);
            })
            .setNegativeButton("Cancel", null)
            .show();
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
        updates.put("isAvailable", true);
        
        // Convert customization options using toMap()
        List<Map<String, Object>> formattedOptions = new ArrayList<>();
        for (CustomizationOption option : customizationOptions) {
            formattedOptions.add(option.toMap());
        }
        updates.put("customizationOptions", formattedOptions);

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
