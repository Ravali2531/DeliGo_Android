package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.MenuItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AddMenuItemActivity extends AppCompatActivity {
    private ImageView itemImageView;
    private EditText itemNameInput, itemDescriptionInput, itemPriceInput;
    private Button selectImageButton, saveButton;
    private Uri selectedImageUri;
    private String restaurantId;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.id_placeholder)
                    .into(itemImageView);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_menu_item);

        initializeViews();
        setupFirebase();
        setupClickListeners();
    }

    private void initializeViews() {
        itemImageView = findViewById(R.id.itemImageView);
        itemNameInput = findViewById(R.id.itemNameInput);
        itemDescriptionInput = findViewById(R.id.itemDescriptionInput);
        itemPriceInput = findViewById(R.id.itemPriceInput);
        selectImageButton = findViewById(R.id.selectImageButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupFirebase() {
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants").child(restaurantId).child("menu");
        storageRef = FirebaseStorage.getInstance().getReference()
            .child("restaurants").child(restaurantId).child("menu_items");
    }

    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        saveButton.setOnClickListener(v -> saveMenuItem());
    }

    private void saveMenuItem() {
        String name = itemNameInput.getText().toString().trim();
        String description = itemDescriptionInput.getText().toString().trim();
        String priceStr = itemPriceInput.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            itemPriceInput.setError("Invalid price");
            return;
        }

        saveButton.setEnabled(false);
        String itemId = databaseRef.push().getKey();
        MenuItem menuItem = new MenuItem(name, description, price);
        menuItem.setId(itemId);

        if (selectedImageUri != null) {
            uploadImageAndSaveItem(menuItem);
        } else {
            saveItemToDatabase(menuItem);
        }
    }

    private void uploadImageAndSaveItem(MenuItem menuItem) {
        StorageReference imageRef = storageRef.child(menuItem.getId());
        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> 
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    menuItem.setImageUrl(uri.toString());
                    saveItemToDatabase(menuItem);
                })
            )
            .addOnFailureListener(e -> {
                saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            });
    }

    private void saveItemToDatabase(MenuItem menuItem) {
        databaseRef.child(menuItem.getId()).setValue(menuItem)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Menu item added successfully", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                saveButton.setEnabled(true);
                Toast.makeText(this, "Failed to save menu item", Toast.LENGTH_SHORT).show();
            });
    }
} 