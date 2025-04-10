package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityOtherActivitiesBinding;

public class OtherActivitiesActivity extends AppCompatActivity {
    private ActivityOtherActivitiesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtherActivitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupClickListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupClickListeners() {
        binding.btnOrderManagement.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrderManagementActivity.class);
            startActivity(intent);
        });

        binding.btnViewPaymentTransactions.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentTransactionsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 