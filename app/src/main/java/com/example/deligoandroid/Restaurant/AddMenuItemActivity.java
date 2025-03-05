package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.widget.AutoCompleteTextView;
import android.view.View;
import android.widget.ArrayAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;

public class AddMenuItemActivity extends AppCompatActivity {
    private ImageView itemImageView;
    private TextInputEditText itemNameInput;
    private TextInputEditText itemDescriptionInput;
    private TextInputEditText itemPriceInput;
    private MaterialCardView selectImageButton;
    private MaterialButton saveButton;
    private ProgressBar progressBar;
    private AutoCompleteTextView categorySpinner;
    private Uri selectedImageUri;
    private String restaurantId;
    private DatabaseReference databaseRef;
    private StorageReference storageRef;
    private static final int ADD_CUSTOMIZATION_REQUEST = 2;
    private List<CustomizationOption> customizationOptions = new ArrayList<>();
    private LinearLayout customizationsContainer;

    private final ActivityResultLauncher<Intent> customizationLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                String name = data.getStringExtra("name");
                String type = data.getStringExtra("type");
                boolean required = data.getBooleanExtra("required", false);
                ArrayList<String> options = data.getStringArrayListExtra("options");
                
                CustomizationOption customization = new CustomizationOption(name, type, required);
                if (options != null) {
                    customization.setOptions(options);
                }
                
                customizationOptions.add(customization);
                addCustomizationToUI(customization);
            }
        }
    );

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
        itemImageView = findViewById(R.id.itemImage);
        itemNameInput = findViewById(R.id.nameInput);
        itemDescriptionInput = findViewById(R.id.descriptionInput);
        itemPriceInput = findViewById(R.id.priceInput);
        selectImageButton = findViewById(R.id.itemImageLayout);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
        categorySpinner = findViewById(R.id.categorySpinner);
        customizationsContainer = findViewById(R.id.customizationsContainer);

        // Setup category spinner
        String[] categories = {"Appetizers", "Main Course", "Desserts", "Beverages", "Sides"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        );
        categorySpinner.setAdapter(adapter);
        categorySpinner.setText(categories[0], false); // Set default category
    }

    private void setupFirebase() {
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants").child(restaurantId).child("menu_items");
        storageRef = FirebaseStorage.getInstance().getReference()
            .child("restaurants").child(restaurantId).child("menu_items");
    }

    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        saveButton.setOnClickListener(v -> saveMenuItem());

        // Update customization button click listener
        findViewById(R.id.addCustomizationButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCustomizationActivity.class);
            customizationLauncher.launch(intent);
        });
    }

    private void addCustomizationToUI(CustomizationOption customization) {
        View view = getLayoutInflater().inflate(R.layout.item_customization_option, customizationsContainer, false);
        
        TextView nameText = view.findViewById(R.id.customizationName);
        TextView typeText = view.findViewById(R.id.customizationType);
        
        nameText.setText(customization.getName());
        typeText.setText(customization.getType());
        
        ImageButton deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            customizationsContainer.removeView(view);
            customizationOptions.remove(customization);
        });
        
        customizationsContainer.addView(view);
    }

    private void saveMenuItem() {
        String name = itemNameInput.getText().toString().trim();
        String description = itemDescriptionInput.getText().toString().trim();
        String priceStr = itemPriceInput.getText().toString().trim();
        String category = categorySpinner.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                itemPriceInput.setError("Price must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            itemPriceInput.setError("Invalid price");
            return;
        }

        saveButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        String itemId = databaseRef.push().getKey();
        if (itemId == null) {
            Toast.makeText(this, "Error generating item ID", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            return;
        }

        MenuItemModel menuItem = new MenuItemModel(name, description, price);
        menuItem.setId(itemId);
        menuItem.setCategory(category);
        menuItem.setCustomizationOptions(customizationOptions);

        if (selectedImageUri != null) {
            uploadImageAndSaveItem(menuItem);
        } else {
            saveItemToDatabase(menuItem);
        }
    }

    private void uploadImageAndSaveItem(MenuItemModel menuItem) {
        StorageReference imageRef = storageRef.child(menuItem.getId());
        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> 
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    menuItem.setImageURL(uri.toString());
                    saveItemToDatabase(menuItem);
                })
            )
            .addOnFailureListener(e -> {
                saveButton.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            });
    }

    private void saveItemToDatabase(MenuItemModel menuItem) {
        databaseRef.child(menuItem.getId()).setValue(menuItem)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Menu item added successfully", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            })
            .addOnFailureListener(e -> {
                saveButton.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to save menu item", Toast.LENGTH_SHORT).show();
            });
    }
} 