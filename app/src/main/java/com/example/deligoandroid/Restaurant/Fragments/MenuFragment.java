package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.AddMenuItemActivity;
import com.example.deligoandroid.Restaurant.Activities.EditMenuItemActivity;
import com.example.deligoandroid.Restaurant.Adapters.MenuAdapter;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment implements MenuAdapter.OnItemClickListener {
    private RecyclerView menuRecyclerView;
    private LinearLayout emptyMenuLayout;
    private FloatingActionButton fabAddItem;
    private MenuAdapter menuAdapter;
    private DatabaseReference databaseRef;
    private String userId;
    private View addItemButton;
    private static final int ADD_ITEM_REQUEST = 1;
    private static final int EDIT_ITEM_REQUEST = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        
        initializeViews(view);
        setupClickListeners();
        loadMenuItems();
        
        return view;
    }

    private void initializeViews(View view) {
        menuRecyclerView = view.findViewById(R.id.menuRecyclerView);
        emptyMenuLayout = view.findViewById(R.id.emptyMenuLayout);
        fabAddItem = view.findViewById(R.id.fabAddItem);
        addItemButton = view.findViewById(R.id.addItemButton);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants").child(userId).child("menu_items");

        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuAdapter = new MenuAdapter(getContext(), this);
        menuRecyclerView.setAdapter(menuAdapter);
    }

    private void setupClickListeners() {
        View.OnClickListener addItemListener = v -> {
            try {
                if (getContext() != null) {
                    Intent intent = new Intent(getContext(), AddMenuItemActivity.class);
                    startActivityForResult(intent, ADD_ITEM_REQUEST);
                }
            } catch (Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), 
                        "Error launching add item screen: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Set click listener for both the empty state button and FAB
        if (addItemButton != null) {
            addItemButton.setOnClickListener(addItemListener);
        }
        if (fabAddItem != null) {
            fabAddItem.setOnClickListener(addItemListener);
        }
    }

    private void loadMenuItems() {
        if (databaseRef == null) {
            Toast.makeText(getContext(), "Database reference is null", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log the database path and user ID
        Toast.makeText(getContext(), "UserID: " + userId + "\nPath: " + databaseRef.toString(), Toast.LENGTH_LONG).show();
        
        databaseRef
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<MenuItemModel> menuItems = new ArrayList<>();
                    
                    // Log the raw data
                    String rawData = String.valueOf(dataSnapshot.getValue());
                    Toast.makeText(getContext(), 
                        "Raw data: " + (rawData.length() > 100 ? rawData.substring(0, 100) + "..." : rawData), 
                        Toast.LENGTH_LONG).show();
                    
                    for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                        try {
                            // Create a new MenuItemModel manually
                            MenuItemModel item = new MenuItemModel();
                            
                            // Get the basic fields
                            if (itemSnapshot.hasChild("name")) {
                                item.setName(itemSnapshot.child("name").getValue(String.class));
                            }
                            if (itemSnapshot.hasChild("description")) {
                                item.setDescription(itemSnapshot.child("description").getValue(String.class));
                            }
                            if (itemSnapshot.hasChild("price")) {
                                Double price = itemSnapshot.child("price").getValue(Double.class);
                                if (price != null) {
                                    item.setPrice(price);
                                }
                            }
                            if (itemSnapshot.hasChild("imageURL")) {
                                item.setImageURL(itemSnapshot.child("imageURL").getValue(String.class));
                            }
                            if (itemSnapshot.hasChild("category")) {
                                item.setCategory(itemSnapshot.child("category").getValue(String.class));
                            }
                            if (itemSnapshot.hasChild("isAvailable")) {
                                Boolean isAvailable = itemSnapshot.child("isAvailable").getValue(Boolean.class);
                                item.setAvailable(isAvailable != null ? isAvailable : true);
                            }
                            
                            // Set the ID from the key
                            item.setId(itemSnapshot.getKey());
                            
                            // Only add if we have the minimum required fields
                            if (item.getName() != null && item.getPrice() > 0) {
                                menuItems.add(item);
                            } else {
                                Toast.makeText(getContext(),
                                    "Skipping item " + itemSnapshot.getKey() + ": missing required fields",
                                    Toast.LENGTH_SHORT).show();
                            }
                            
                        } catch (Exception e) {
                            if (getContext() != null) {
                                String errorMsg = "Error parsing item at " + itemSnapshot.getKey() + 
                                    "\nError: " + e.getMessage() +
                                    "\nValue: " + itemSnapshot.getValue();
                                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    // Log final count
                    Toast.makeText(getContext(), 
                        "Successfully loaded " + menuItems.size() + " items", 
                        Toast.LENGTH_SHORT).show();
                        
                    updateUI(menuItems);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                            "Database error: " + databaseError.getMessage() +
                            "\nDetails: " + databaseError.getDetails(),
                            Toast.LENGTH_LONG).show();
                        databaseError.toException().printStackTrace();
                    }
                }
            });
    }

    private void updateUI(List<MenuItemModel> menuItems) {
        if (getActivity() == null) return;
        
        if (menuItems.isEmpty()) {
            menuRecyclerView.setVisibility(View.GONE);
            emptyMenuLayout.setVisibility(View.VISIBLE);
            fabAddItem.setVisibility(View.GONE);
        } else {
            menuRecyclerView.setVisibility(View.VISIBLE);
            emptyMenuLayout.setVisibility(View.GONE);
            fabAddItem.setVisibility(View.VISIBLE);
            menuAdapter.setMenuItems(menuItems);
        }
    }

    @Override
    public void onItemClick(MenuItemModel item) {
        try {
            if (getContext() == null) {
                Log.e("MenuFragment", "Context is null");
                return;
            }
            
            if (item == null) {
                Log.e("MenuFragment", "Item is null");
                Toast.makeText(getContext(), "Error: Invalid menu item", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (item.getId() == null) {
                Log.e("MenuFragment", "Item ID is null");
                Toast.makeText(getContext(), "Error: Invalid menu item ID", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("MenuFragment", "Attempting to edit item: " + item.getId());
            Log.d("MenuFragment", "Item details - Name: " + item.getName() + ", Category: " + item.getCategory());
            
            Intent intent = new Intent(getContext(), EditMenuItemActivity.class);
            intent.putExtra("itemId", item.getId());
            startActivityForResult(intent, EDIT_ITEM_REQUEST);
            
        } catch (Exception e) {
            Log.e("MenuFragment", "Error launching edit activity", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), 
                    "Error launching edit screen: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // The menu will be automatically updated through the Firebase listener
    }
} 