package com.example.deligoandroid.Customer.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Customer.CustomerSupportActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerAccountFragment extends Fragment {
    private TextView customerName;
    private TextView customerEmail;
    private TextView customerPhone;
    private CardView supportCard;
    private FirebaseAuth mAuth;
    private DatabaseReference customerRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_account, container, false);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return view;
        }

        // Initialize views
        customerName = view.findViewById(R.id.customerName);
        customerEmail = view.findViewById(R.id.customerEmail);
        customerPhone = view.findViewById(R.id.customerPhone);
        supportCard = view.findViewById(R.id.supportCard);

        // Set up Firebase reference
        String customerId = mAuth.getCurrentUser().getUid();
        customerRef = FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(customerId);

        // Load customer data
        loadCustomerData();

        // Set up support card click
        supportCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CustomerSupportActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void loadCustomerData() {
        customerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    if (name != null) customerName.setText(name);
                    if (email != null) customerEmail.setText(email);
                    if (phone != null) customerPhone.setText(phone);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
} 