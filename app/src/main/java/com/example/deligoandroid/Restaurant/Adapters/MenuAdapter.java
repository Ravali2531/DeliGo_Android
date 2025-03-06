package com.example.deligoandroid.Restaurant.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuItemViewHolder> {
    private Context context;
    private List<MenuItemModel> menuItems;
    private OnMenuItemClickListener listener;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItemModel item);
    }

    public MenuAdapter(Context context) {
        this.context = context;
        this.menuItems = new ArrayList<>();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.listener = listener;
    }

    public void setMenuItems(List<MenuItemModel> menuItems) {
        this.menuItems = menuItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant_menu, parent, false);
        return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
        MenuItemModel menuItem = menuItems.get(position);
        holder.bind(menuItem);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemImage;
        private TextView itemName;
        private TextView itemDescription;
        private TextView itemPrice;
        private TextView customizationInfo;

        MenuItemViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemDescription = itemView.findViewById(R.id.itemDescription);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            customizationInfo = itemView.findViewById(R.id.customizationInfo);

            // Set click listener for the entire item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMenuItemClick(menuItems.get(position));
                }
            });
        }

        void bind(MenuItemModel menuItem) {
            itemName.setText(menuItem.getName());
            itemDescription.setText(menuItem.getDescription());
            itemPrice.setText(String.format("$%.2f", menuItem.getPrice()));

            // Load image using Glide
            if (menuItem.getImageURL() != null && !menuItem.getImageURL().isEmpty()) {
                Glide.with(context)
                    .load(menuItem.getImageURL())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.ic_food_placeholder);
            }

            // Show customization info if available
            if (menuItem.getCustomizationOptions() != null && !menuItem.getCustomizationOptions().isEmpty()) {
                customizationInfo.setVisibility(View.VISIBLE);
            } else {
                customizationInfo.setVisibility(View.GONE);
            }
        }
    }
} 