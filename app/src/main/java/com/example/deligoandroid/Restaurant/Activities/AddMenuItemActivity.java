package com.example.deligoandroid.Restaurant.Activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationItem;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.example.deligoandroid.databinding.ActivityAddMenuItemBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
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
        // TODO: Implement customization dialog
        Toast.makeText(this, "Customization options coming soon", Toast.LENGTH_SHORT).show();
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
                        saveMenuItemToDatabase(menuRef, name, description, price, category, imageUrl);
                    } else {
                        isUploading = false;
                        binding.progressBar.setVisibility(View.GONE);
                        binding.saveButton.setEnabled(true);
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            // Save without image
            saveMenuItemToDatabase(menuRef, name, description, price, category, null);
        }
    }

    private void saveMenuItemToDatabase(DatabaseReference menuRef, String name, String description,
                                      double price, String category, String imageUrl) {
        Map<String, Object> menuItem = new HashMap<>();
        menuItem.put("name", name);
        menuItem.put("description", description);
        menuItem.put("price", price);
        menuItem.put("category", category);
        menuItem.put("imageURL", imageUrl);
        menuItem.put("isAvailable", true);
        menuItem.put("customizationOptions", customizationOptions);

        menuRef.setValue(menuItem)
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