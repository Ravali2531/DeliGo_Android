package com.example.deligoandroid.Restaurant.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
//import android.widget.SearchView;
import androidx.appcompat.widget.SearchView;

import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Activities.EditMenuItemActivity;
import com.example.deligoandroid.Restaurant.Activities.AddMenuItemActivity;
import com.example.deligoandroid.Restaurant.Adapters.MenuAdapter;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment implements MenuAdapter.OnMenuItemClickListener {
    private RecyclerView menuRecyclerView;
    private LinearLayout emptyMenuLayout;
    private MenuAdapter menuAdapter;
    private DatabaseReference databaseRef;
    private String userId;
    private View addItemButton;
    private SearchView searchView;
    private List<MenuItemModel> allMenuItems; // Store all menu items for filtering
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
        addItemButton = view.findViewById(R.id.addItemButton);
        searchView = (SearchView) view.findViewById(R.id.searchView);
        searchView.setQueryHint("Search by name or category...");

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants").child(userId).child("menu_items");

        menuRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        menuAdapter = new MenuAdapter(getContext());
        menuAdapter.setOnMenuItemClickListener(this);
        menuRecyclerView.setAdapter(menuAdapter);

        setupSearchView();
    }

    private void setupClickListeners() {
        View.OnClickListener addItemListener = v -> {
            try {
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), AddMenuItemActivity.class);
                    startActivity(intent);
                }
            } catch (Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), 
                        "Error launching add item screen: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // Add this to see the full error in logcat
                }
            }
        };

        // Set click listener for the add item button
        if (addItemButton != null) {
            addItemButton.setOnClickListener(addItemListener);
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMenuItems(newText);
                return true;
            }
        });
    }

    private void filterMenuItems(String query) {
        if (allMenuItems == null) return;

        if (query == null || query.isEmpty()) {
            updateUI(allMenuItems);
            return;
        }

        String lowerCaseQuery = query.toLowerCase().trim();
        List<MenuItemModel> filteredList = new ArrayList<>();

        for (MenuItemModel item : allMenuItems) {
            if (item.getName().toLowerCase().contains(lowerCaseQuery) ||
                (item.getCategory() != null && item.getCategory().toLowerCase().contains(lowerCaseQuery))) {
                filteredList.add(item);
            }
        }

        updateUI(filteredList);
    }

    private void loadMenuItems() {
        if (databaseRef == null) {
            Toast.makeText(getContext(), "Database reference is null", Toast.LENGTH_SHORT).show();
            return;
        }
        
        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allMenuItems = new ArrayList<>();
                
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    try {
                        // Create a new MenuItemModel manually
                        MenuItemModel item = new MenuItemModel();
                        
                        // Set the ID first
                        String itemId = itemSnapshot.getKey();
                        if (itemId == null) {
                            Log.e("MenuFragment", "Skipping item: null ID");
                            continue;
                        }
                        item.setId(itemId);
                        
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
                        
                        // Only add if we have the minimum required fields
                        if (item.getId() != null && item.getName() != null && item.getPrice() > 0) {
                            Log.d("MenuFragment", "Adding item with ID: " + item.getId());
                            allMenuItems.add(item);
                        } else {
                            Log.e("MenuFragment", "Skipping item " + itemId + ": missing required fields");
                        }
                        
                    } catch (Exception e) {
                        Log.e("MenuFragment", "Error parsing item: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // Apply any existing search filter
                String currentQuery = searchView != null ? searchView.getQuery().toString() : "";
                if (!currentQuery.isEmpty()) {
                    filterMenuItems(currentQuery);
                } else {
                    updateUI(allMenuItems);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("MenuFragment", "Database error: " + databaseError.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                        "Database error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(List<MenuItemModel> menuItems) {
        if (getActivity() == null) return;
        
        if (menuItems.isEmpty()) {
            menuRecyclerView.setVisibility(View.GONE);
            emptyMenuLayout.setVisibility(View.VISIBLE);
        } else {
            menuRecyclerView.setVisibility(View.VISIBLE);
            emptyMenuLayout.setVisibility(View.GONE);
            menuAdapter.setMenuItems(menuItems);
        }
    }

    @Override
    public void onMenuItemClick(MenuItemModel item) {
        // Open EditMenuItemActivity with the selected item
        Intent intent = new Intent(getActivity(), EditMenuItemActivity.class);
        intent.putExtra("itemId", item.getId());
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // The menu will be automatically updated through the Firebase listener
    }
} 