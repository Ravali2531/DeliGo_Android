package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.AddMenuItemActivity;
import com.example.deligoandroid.Restaurant.Adapters.MenuAdapter;
import com.example.deligoandroid.Restaurant.Models.MenuItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment {
    private RecyclerView menuItemsRecyclerView;
    private Button addMenuItemBtn;
    private MenuAdapter menuAdapter;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        initializeViews(view);
        loadMenuItems();

        return view;
    }

    private void initializeViews(View view) {
        menuItemsRecyclerView = view.findViewById(R.id.menuItemsRecyclerView);
        addMenuItemBtn = view.findViewById(R.id.addMenuItemBtn);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuAdapter = new MenuAdapter(userId);
        menuItemsRecyclerView.setAdapter(menuAdapter);

        addMenuItemBtn.setOnClickListener(v -> 
            startActivity(new Intent(getContext(), AddMenuItemActivity.class)));
    }

    private void loadMenuItems() {
        databaseRef.child("restaurants").child(userId).child("menu")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<MenuItem> menuItems = new ArrayList<>();
                    for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                        MenuItem item = itemSnapshot.getValue(MenuItem.class);
                        if (item != null) {
                            item.setId(itemSnapshot.getKey());
                            menuItems.add(item);
                        }
                    }
                    menuAdapter.setMenuItems(menuItems);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle error
                }
            });
    }
} 