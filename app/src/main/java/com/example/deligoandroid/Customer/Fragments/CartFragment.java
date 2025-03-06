package com.example.deligoandroid.Customer.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.deligoandroid.R;

public class CartFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_empty_state, container, false);
        
        // Set up empty state
        ImageView emptyIcon = view.findViewById(R.id.emptyStateIcon);
        TextView emptyTitle = view.findViewById(R.id.emptyStateTitle);
        TextView emptyMessage = view.findViewById(R.id.emptyStateMessage);

        emptyIcon.setImageResource(R.drawable.ic_cart);
        emptyTitle.setText("Your Cart is Empty");
        emptyMessage.setText("Add items from a restaurant to get started");

        return view;
    }
} 