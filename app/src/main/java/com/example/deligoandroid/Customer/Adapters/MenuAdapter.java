package com.example.deligoandroid.Customer.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Dialogs.ItemCustomizationDialog;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.Customer.Models.CustomizationOption;
import com.example.deligoandroid.Customer.Models.CustomizationItem;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private Context context;
    private List<Object> items = new ArrayList<>();
    private Map<String, List<MenuItem>> groupedItems;
    private OnAddToCartListener onAddToCartListener;
    private OnFavoriteClickListener onFavoriteClickListener;
    private Set<String> favoriteItemIds;
    private DatabaseReference favoritesRef;
    private ValueEventListener favoritesListener;
    private String restaurantId;

    public MenuAdapter(Context context, String restaurantId) {
        this.context = context;
        this.restaurantId = restaurantId;
        this.groupedItems = new HashMap<>();
        this.favoriteItemIds = new HashSet<>();
        
        // Initialize Firebase reference
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.favoritesRef = FirebaseDatabase.getInstance().getReference()
            .child("users")
            .child(userId)
            .child("favorites");

        // Set up real-time favorites listener
        setupFavoritesListener();
    }

    private void setupFavoritesListener() {
        // Remove existing listener if any
        if (favoritesListener != null) {
            favoritesRef.removeEventListener(favoritesListener);
        }

        favoritesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteItemIds.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    favoriteItemIds.add(itemSnapshot.getKey());
                }
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error loading favorites", Toast.LENGTH_SHORT).show();
            }
        };

        favoritesRef.addValueEventListener(favoritesListener);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Clean up listener when adapter is detached
        if (favoritesListener != null) {
            favoritesRef.removeEventListener(favoritesListener);
        }
    }

    public interface OnAddToCartListener {
        void onAddToCart(CartItem cartItem);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(MenuItem menuItem, boolean isFavorite);
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.onAddToCartListener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }

    public void setFavoriteItems(Set<String> favoriteItemIds) {
        this.favoriteItemIds = favoriteItemIds;
        notifyDataSetChanged();
    }

    public void setMenuItems(Map<String, List<MenuItem>> groupedItems) {
        this.groupedItems = groupedItems;
        items.clear();

        // Flatten the grouped items into a single list with headers
        for (Map.Entry<String, List<MenuItem>> entry : groupedItems.entrySet()) {
            items.add(entry.getKey()); // Add category header
            items.addAll(entry.getValue()); // Add items in that category
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant_menu, parent, false);
            return new MenuItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof MenuItemViewHolder) {
            MenuItem menuItem = (MenuItem) items.get(position);
            if (menuItem != null) {
                MenuItemViewHolder menuItemHolder = (MenuItemViewHolder) holder;
                menuItemHolder.nameTextView.setText(menuItem.getName());
                menuItemHolder.descriptionTextView.setText(menuItem.getDescription());
                menuItemHolder.priceTextView.setText(String.format("$%.2f", menuItem.getPrice()));

                // Load image using Glide
                if (menuItem.getImageURL() != null && !menuItem.getImageURL().isEmpty()) {
                    Glide.with(context)
                        .load(menuItem.getImageURL())
                        .placeholder(R.drawable.ic_food_placeholder)
                        .error(R.drawable.ic_food_placeholder)
                        .into(menuItemHolder.itemImageView);
                } else {
                    menuItemHolder.itemImageView.setImageResource(R.drawable.ic_food_placeholder);
                }

                // Set favorite state
                boolean isFavorite = favoriteItemIds.contains(menuItem.getId());
                menuItemHolder.favoriteButton.setImageResource(
                    isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline
                );
                menuItemHolder.favoriteButton.setColorFilter(
                    ContextCompat.getColor(context, R.color.favorite_red),
                    android.graphics.PorterDuff.Mode.SRC_IN
                );

                // Handle favorite click
                menuItemHolder.favoriteButton.setOnClickListener(v -> toggleFavorite(menuItem));

                // Show customization info if available
                if (menuItem.hasCustomizations()) {
                    menuItemHolder.bulletPoint.setVisibility(View.VISIBLE);
                    menuItemHolder.customizationInfo.setVisibility(View.VISIBLE);
                } else {
                    menuItemHolder.bulletPoint.setVisibility(View.GONE);
                    menuItemHolder.customizationInfo.setVisibility(View.GONE);
                }

                // Handle item click
                menuItemHolder.itemView.setOnClickListener(v -> {
                    if (context instanceof FragmentActivity) {
                        // Add debug logging
                        Log.d("MenuAdapter", "Item clicked: " + menuItem.getName());
                        Log.d("MenuAdapter", "Has customizations: " + menuItem.hasCustomizations());
                        if (menuItem.getCustomizationOptions() != null) {
                            Log.d("MenuAdapter", "Number of customization options: " + menuItem.getCustomizationOptions().size());
                            for (CustomizationOption option : menuItem.getCustomizationOptions()) {
                                Log.d("MenuAdapter", "Option: " + option.getName() + ", Type: " + option.getType());
                            }
                        } else {
                            Log.d("MenuAdapter", "Customization options is null");
                        }

                        ItemCustomizationDialog dialog = ItemCustomizationDialog.newInstance(menuItem);
                        dialog.setOnAddToCartListener(cartItem -> {
                            if (onAddToCartListener != null) {
                                cartItem.setRestaurantId(restaurantId);
                                onAddToCartListener.onAddToCart(cartItem);
                            }
                        });
                        dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "customization");
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
        }

        void bind(String category) {
            categoryTitle.setText(category);
        }
    }

    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImageView;
        TextView nameTextView;
        TextView descriptionTextView;
        TextView priceTextView;
        TextView bulletPoint;
        TextView customizationInfo;
        ImageButton favoriteButton;

        MenuItemViewHolder(View itemView) {
            super(itemView);
            itemImageView = itemView.findViewById(R.id.itemImage);
            nameTextView = itemView.findViewById(R.id.itemName);
            descriptionTextView = itemView.findViewById(R.id.itemDescription);
            priceTextView = itemView.findViewById(R.id.itemPrice);
            bulletPoint = itemView.findViewById(R.id.bulletPoint);
            customizationInfo = itemView.findViewById(R.id.customizationInfo);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }

    private void toggleFavorite(MenuItem menuItem) {
        String itemId = menuItem.getId();
        boolean isFavorite = favoriteItemIds.contains(itemId);

        if (isFavorite) {
            // Remove from favorites
            favoritesRef.child(itemId).removeValue()
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Failed to remove from favorites", Toast.LENGTH_SHORT).show()
                );
        } else {
            // Add to favorites
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("id", menuItem.getId());
            favoriteData.put("name", menuItem.getName());
            favoriteData.put("description", menuItem.getDescription());
            favoriteData.put("price", menuItem.getPrice());
            favoriteData.put("imageURL", menuItem.getImageURL());
            favoriteData.put("category", menuItem.getCategory());
            favoriteData.put("timestamp", ServerValue.TIMESTAMP);
            favoriteData.put("hasCustomizations", menuItem.hasCustomizations());
            
            // Add customization options
            if (menuItem.hasCustomizations() && menuItem.getCustomizationOptions() != null) {
                List<Map<String, Object>> customizationOptionsList = new ArrayList<>();
                for (CustomizationOption option : menuItem.getCustomizationOptions()) {
                    Map<String, Object> optionData = new HashMap<>();
                    optionData.put("id", option.getId());
                    optionData.put("name", option.getName());
                    optionData.put("type", option.getType());
                    optionData.put("required", option.isRequired());
                    
                    List<Map<String, Object>> itemsList = new ArrayList<>();
                    if (option.getOptions() != null) {
                        for (CustomizationItem item : option.getOptions()) {
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("id", item.getId());
                            itemData.put("name", item.getName());
                            itemData.put("price", item.getPrice());
                            itemsList.add(itemData);
                        }
                    }
                    optionData.put("options", itemsList);
                    customizationOptionsList.add(optionData);
                }
                favoriteData.put("customizationOptions", customizationOptionsList);
            }

            favoritesRef.child(itemId).setValue(favoriteData)
                .addOnFailureListener(e -> 
                    Toast.makeText(context, "Failed to add to favorites", Toast.LENGTH_SHORT).show()
                );
        }
    }
} 