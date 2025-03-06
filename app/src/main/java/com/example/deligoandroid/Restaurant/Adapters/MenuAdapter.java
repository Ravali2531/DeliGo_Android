package com.example.deligoandroid.Restaurant.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Models.MenuItem;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuItemViewHolder> {
    private Context context;
    private List<MenuItemModel> menuItems = new ArrayList<>();
    private OnMenuItemClickListener listener;

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MenuItemModel item);
    }

    public MenuAdapter(Context context) {
        this.context = context;
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
        holder.bind(menuItems.get(position));
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView itemImage;
        private ImageView placeholderImage;
        private TextView itemName;
        private TextView itemDescription;
        private TextView itemPrice;
        private TextView customizationInfo;
        private View availabilityIndicator;

        MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            placeholderImage = itemView.findViewById(R.id.placeholderImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemDescription = itemView.findViewById(R.id.itemDescription);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            customizationInfo = itemView.findViewById(R.id.customizationInfo);
            availabilityIndicator = itemView.findViewById(R.id.availabilityIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMenuItemClick(menuItems.get(position));
                }
            });
        }

        void bind(MenuItemModel item) {
            itemName.setText(item.getName());
            itemDescription.setText(item.getDescription());
            itemPrice.setText(String.format("$%.2f", item.getPrice()));

            // Handle image loading
            if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
                itemImage.setVisibility(View.VISIBLE);
                placeholderImage.setVisibility(View.GONE);
                Glide.with(context)
                    .load(item.getImageURL())
                    .centerCrop()
                    .into(itemImage);
            } else {
                itemImage.setVisibility(View.GONE);
                placeholderImage.setVisibility(View.VISIBLE);
            }

            // Show customization info if item has customization options
            if (item.getCustomizationOptions() != null && !item.getCustomizationOptions().isEmpty()) {
                customizationInfo.setVisibility(View.VISIBLE);
            } else {
                customizationInfo.setVisibility(View.GONE);
            }

            // Set availability indicator color
            availabilityIndicator.setBackgroundResource(R.drawable.circle_background);
            availabilityIndicator.getBackground().setTint(
                ContextCompat.getColor(context, item.isAvailable() ? 
                    android.R.color.holo_green_light : android.R.color.holo_red_light)
            );
        }
    }
} 