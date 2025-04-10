package com.example.deligoandroid.Restaurant.Activities;

import static android.content.ContentValues.TAG;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationItem;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.example.deligoandroid.databinding.ActivityAddMenuItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddMenuItemActivity extends AppCompatActivity {
    private ActivityAddMenuItemBinding binding;
    private Uri selectedImageUri;
    private final ArrayList<CustomizationOption> customizationOptions = new ArrayList<>();
    private boolean isUploading = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                binding.itemImage.setImageURI(selectedImageUri);
                binding.addImageText.setVisibility(View.GONE);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMenuItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupSpinner();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Menu Item");
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
            R.array.food_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.itemImageLayout.setOnClickListener(v -> openImagePicker());
        binding.addCustomizationButton.setOnClickListener(v -> showAddCustomizationDialog());
        binding.saveButton.setOnClickListener(v -> saveMenuItem());
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
        
        binding.customizationsContainer.addView(view);
    }

    private void saveMenuItem() {
        String name = binding.nameInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();
        String priceStr = binding.priceInput.getText().toString().trim();
        String category = binding.categorySpinner.getText().toString().trim();


        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
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

        String itemId = UUID.randomUUID().toString();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(userId)
            .child("menu_items")
            .child(itemId);

        if (selectedImageUri != null) {
            // Upload image first
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("menu_items")
                .child(itemId + ".jpg");

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
            // Save without image
            updateMenuItemInDatabase(menuRef, name, description, price, category, null);
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
        
//         Convert customization options to the correct format// Convert customization options using toMap()
                List<Map<String, Object>> formattedOptions = new ArrayList<>();
                for (CustomizationOption option : customizationOptions) {
                    formattedOptions.add(option.toMap());
                }

        updates.put("customizationOptions", formattedOptions);

        menuRef.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Menu item added successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                isUploading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to add menu item", Toast.LENGTH_SHORT).show();
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