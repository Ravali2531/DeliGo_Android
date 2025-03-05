package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.CustomizationOption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;

public class AddCustomizationActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private AutoCompleteTextView typeSpinner;
    private SwitchMaterial requiredSwitch;
    private LinearLayout optionsContainer;
    private MaterialButton addOptionButton;
    private MaterialButton saveButton;
    private ArrayList<String> options = new ArrayList<>();
    private ArrayList<Double> optionPrices = new ArrayList<>();

    private final ActivityResultLauncher<Intent> addOptionLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                String optionName = data.getStringExtra("name");
                double optionPrice = data.getDoubleExtra("price", 0.0);
                
                options.add(optionName);
                optionPrices.add(optionPrice);
                
                // Add to UI
                View view = getLayoutInflater().inflate(R.layout.item_customization_option, optionsContainer, false);
                
                TextView nameText = view.findViewById(R.id.customizationName);
                TextView typeText = view.findViewById(R.id.customizationType);
                
                nameText.setText(optionName);
                if (optionPrice > 0) {
                    typeText.setText(String.format("+ $%.2f", optionPrice));
                }
                
                ImageButton deleteButton = view.findViewById(R.id.deleteButton);
                deleteButton.setOnClickListener(v -> {
                    int index = optionsContainer.indexOfChild(view);
                    optionsContainer.removeView(view);
                    if (index >= 0 && index < options.size()) {
                        options.remove(index);
                        optionPrices.remove(index);
                    }
                });
                
                optionsContainer.addView(view);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_customization);

        initializeViews();
        setupTypeSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        typeSpinner = findViewById(R.id.typeSpinner);
        requiredSwitch = findViewById(R.id.requiredSwitch);
        optionsContainer = findViewById(R.id.optionsContainer);
        addOptionButton = findViewById(R.id.addOptionButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupTypeSpinner() {
        String[] types = {"Single Selection", "Multiple Selection"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            types
        );
        typeSpinner.setAdapter(adapter);
        typeSpinner.setText(types[0], false);
    }

    private void setupClickListeners() {
        addOptionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddOptionActivity.class);
            addOptionLauncher.launch(intent);
        });
        
        findViewById(R.id.cancelButton).setOnClickListener(v -> finish());
        
        saveButton.setOnClickListener(v -> saveCustomization());
    }

    private void saveCustomization() {
        String name = nameInput.getText().toString().trim();
        String type = typeSpinner.getText().toString();
        boolean required = requiredSwitch.isChecked();

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            return;
        }

        if (options.isEmpty()) {
            Toast.makeText(this, "Add at least one option", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create customization option
        CustomizationOption customization = new CustomizationOption(name, type, required);
        customization.setOptions(options);

        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("name", name);
        resultIntent.putExtra("type", type);
        resultIntent.putExtra("required", required);
        resultIntent.putStringArrayListExtra("options", options);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
} 