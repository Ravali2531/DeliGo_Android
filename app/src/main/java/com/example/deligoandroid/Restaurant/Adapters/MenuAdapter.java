package com.example.deligoandroid.Restaurant.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.MenuItem;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
    private List<MenuItem> menuItems = new ArrayList<>();
    private Context context;
    private String restaurantId;

    public MenuAdapter(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        
        holder.nameText.setText(item.getName());
        holder.descriptionText.setText(item.getDescription());
        
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        holder.priceText.setText(format.format(item.getPrice()));
        
        holder.availabilitySwitch.setChecked(item.isAvailable());
        
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.id_placeholder)
                .into(holder.imageView);
        }

        holder.availabilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            updateItemAvailability(item.getId(), isChecked));
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
        notifyDataSetChanged();
    }

    private void updateItemAvailability(String itemId, boolean available) {
        DatabaseReference menuRef = FirebaseDatabase.getInstance().getReference()
            .child("restaurants")
            .child(restaurantId)
            .child("menu")
            .child(itemId)
            .child("available");
            
        menuRef.setValue(available);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText, descriptionText, priceText;
        Switch availabilitySwitch;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.menuItemImage);
            nameText = itemView.findViewById(R.id.menuItemName);
            descriptionText = itemView.findViewById(R.id.menuItemDescription);
            priceText = itemView.findViewById(R.id.menuItemPrice);
            availabilitySwitch = itemView.findViewById(R.id.availabilitySwitch);
        }
    }
} 