package com.example.deligoandroid.Customer.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
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
import com.example.deligoandroid.R;
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

    public MenuAdapter(Context context) {
        this.context = context;
        this.groupedItems = new HashMap<>();
        this.favoriteItemIds = new HashSet<>();
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
                menuItemHolder.priceTextView.setText(String.format(Locale.getDefault(), "$%.2f", menuItem.getPrice()));

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

                // Set availability indicator
                menuItemHolder.availabilityIndicator.setBackgroundTintList(ColorStateList.valueOf(
                    menuItem.isAvailable() ? 
                    ContextCompat.getColor(context, android.R.color.holo_green_light) : 
                    ContextCompat.getColor(context, android.R.color.holo_red_light)
                ));

                // Set favorite state
                boolean isFavorite = favoriteItemIds.contains(menuItem.getId());
                menuItemHolder.favoriteButton.setImageResource(isFavorite ? 
                    R.drawable.ic_favorite_filled : 
                    R.drawable.ic_favorite_outline);

                // Handle favorite click
                menuItemHolder.favoriteButton.setOnClickListener(v -> {
                    if (onFavoriteClickListener != null) {
                        boolean newState = !isFavorite;
                        onFavoriteClickListener.onFavoriteClick(menuItem, newState);
                    }
                });

                // Handle add to cart click
                menuItemHolder.addToCartButton.setOnClickListener(v -> {
                    if (onAddToCartListener != null && menuItem.isAvailable()) {
                        CartItem cartItem = new CartItem(menuItem);
                        onAddToCartListener.onAddToCart(cartItem);
                    } else if (!menuItem.isAvailable()) {
                        Toast.makeText(context, "This item is currently unavailable", Toast.LENGTH_SHORT).show();
                    }
                });

                // Show customization info if available
                if (menuItem.hasCustomizations()) {
                    menuItemHolder.customizationInfo.setVisibility(View.VISIBLE);
                } else {
                    menuItemHolder.customizationInfo.setVisibility(View.GONE);
                }
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
        View availabilityIndicator;
        ImageButton favoriteButton;
        Button addToCartButton;
        TextView customizationInfo;

        MenuItemViewHolder(View itemView) {
            super(itemView);
            itemImageView = itemView.findViewById(R.id.itemImage);
            nameTextView = itemView.findViewById(R.id.itemName);
            descriptionTextView = itemView.findViewById(R.id.itemDescription);
            priceTextView = itemView.findViewById(R.id.itemPrice);
            availabilityIndicator = itemView.findViewById(R.id.availabilityIndicator);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            addToCartButton = itemView.findViewById(R.id.addToCartButton);
            customizationInfo = itemView.findViewById(R.id.customizationInfo);
        }
    }
} 