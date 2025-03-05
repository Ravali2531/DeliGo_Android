package com.example.deligoandroid.Restaurant.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.MenuItemModel;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.util.List;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder> {
    private List<MenuItemModel> menuItems;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(MenuItemModel item);
    }

    public MenuAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.menuItems = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_menu, parent, false);
        return new MenuViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        MenuItemModel item = menuItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public void setMenuItems(List<MenuItemModel> menuItems) {
        this.menuItems = menuItems;
        notifyDataSetChanged();
    }

    class MenuViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView itemImage;
        private final TextView itemName;
        private final TextView itemDescription;
        private final TextView itemPrice;
        private final View availabilityIndicator;

        MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            itemName = itemView.findViewById(R.id.itemName);
            itemDescription = itemView.findViewById(R.id.itemDescription);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            availabilityIndicator = itemView.findViewById(R.id.availabilityIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(menuItems.get(position));
                }
            });
        }

        void bind(MenuItemModel item) {
            itemName.setText(item.getName());
            itemDescription.setText(item.getDescription());
            itemPrice.setText(String.format("$%.2f", item.getPrice()));
            
            // Set availability indicator color
            availabilityIndicator.setBackgroundResource(
                item.isAvailable() ? R.drawable.circle_background_green : R.drawable.circle_background_red
            );

            // Load image using Glide
            if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
                Glide.with(context)
                    .load(item.getImageURL())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .centerCrop()
                    .into(itemImage);
            } else {
                itemImage.setImageResource(R.drawable.placeholder_image);
            }
        }
    }
} 