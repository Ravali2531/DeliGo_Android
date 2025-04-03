package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityPaymentTransactionsBinding;
import com.example.deligoandroid.databinding.ItemPaymentTransactionBinding;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentIntentCollection;
import com.stripe.param.PaymentIntentListParams;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentTransactionsActivity extends AppCompatActivity {
    private static final String TAG = "PaymentTransactions";
    private ActivityPaymentTransactionsBinding binding;
    private TransactionAdapter adapter;
    private List<PaymentIntent> transactions;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupSearch();
        initializeStripe();
        loadTransactions();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Payment Transactions");
        }
    }

    private void setupRecyclerView() {
        transactions = new ArrayList<>();
        adapter = new TransactionAdapter(transactions);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchTransactions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeStripe() {
        Stripe.apiKey = "sk_test_51PlVh8P9Bz7XrwZPWSkDzX7AmaNgVr04yPOQWnbAECiYSWKtsmmVgD2Z8JYBY8a5dmEfKXaTewrBESb3fxIliwDo00HdJmKBKz";
        executorService = Executors.newSingleThreadExecutor();
    }

    private void loadTransactions() {
        binding.progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                PaymentIntentListParams params = PaymentIntentListParams.builder()
                        .setLimit(100L) // Adjust limit as needed
                        .build();
                
                PaymentIntentCollection paymentIntents = PaymentIntent.list(params);
                
                runOnUiThread(() -> {
                    transactions.clear();
                    transactions.addAll(paymentIntents.getData());
                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                });
            } catch (StripeException e) {
                Log.e(TAG, "Error loading transactions: " + e.getMessage());
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    // Show error message to user
                });
            }
        });
    }

    private void searchTransactions(String query) {
        if (query.isEmpty()) {
            adapter.updateTransactions(transactions);
            return;
        }

        List<PaymentIntent> filteredList = new ArrayList<>();
        for (PaymentIntent transaction : transactions) {
            if (transaction.getId().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(transaction);
            }
        }
        adapter.updateTransactions(filteredList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
        private List<PaymentIntent> transactions;

        public TransactionAdapter(List<PaymentIntent> transactions) {
            this.transactions = transactions;
        }

        public void updateTransactions(List<PaymentIntent> newTransactions) {
            this.transactions = newTransactions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPaymentTransactionBinding binding = ItemPaymentTransactionBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new TransactionViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
            PaymentIntent transaction = transactions.get(position);
            holder.bind(transaction);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class TransactionViewHolder extends RecyclerView.ViewHolder {
            private final ItemPaymentTransactionBinding binding;

            public TransactionViewHolder(ItemPaymentTransactionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(PaymentIntent transaction) {
                binding.transactionId.setText("Transaction ID: " + transaction.getId());
                binding.amount.setText(String.format(Locale.US, "Amount: $%.2f", transaction.getAmount() / 100.0));
                binding.status.setText("Status: " + transaction.getStatus());
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US);
                binding.date.setText(sdf.format(new Date(transaction.getCreated() * 1000L)));

                // Set status color
                int statusColor;
                switch (transaction.getStatus().toLowerCase()) {
                    case "succeeded":
                        statusColor = R.color.green;
                        break;
                    case "processing":
                        statusColor = R.color.orange;
                        break;
                    case "requires_payment_method":
                        statusColor = R.color.purple;
                        break;
                    default:
                        statusColor = R.color.gray_600;
                        break;
                }
                binding.status.setTextColor(itemView.getContext().getColor(statusColor));
            }
        }
    }
} 