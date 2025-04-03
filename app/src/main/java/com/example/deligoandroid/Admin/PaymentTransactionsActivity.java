package com.example.deligoandroid.Admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.databinding.ActivityPaymentTransactionsBinding;

public class PaymentTransactionsActivity extends AppCompatActivity {
    private ActivityPaymentTransactionsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Payment Transactions");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 