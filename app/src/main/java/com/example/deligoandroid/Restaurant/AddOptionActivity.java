package com.example.deligoandroid.Restaurant;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddOptionActivity extends AppCompatActivity {
    private TextInputEditText nameInput;
    private TextInputEditText priceInput;
    private MaterialButton addButton;
    private MaterialButton cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_option);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        nameInput = findViewById(R.id.nameInput);
        priceInput = findViewById(R.id.priceInput);
        addButton = findViewById(R.id.addButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> finish());
        
        addButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String priceStr = priceInput.getText().toString().trim();

            if (name.isEmpty()) {
                nameInput.setError("Name is required");
                return;
            }

            double price = 0.0;
            if (!priceStr.isEmpty()) {
                try {
                    price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        priceInput.setError("Price cannot be negative");
                        return;
                    }
                } catch (NumberFormatException e) {
                    priceInput.setError("Invalid price");
                    return;
                }
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("name", name);
            resultIntent.putExtra("price", price);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
} 