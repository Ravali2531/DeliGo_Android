package com.example.deligoandroid.Restaurant.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.OrderItem;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {
    private List<OrderItem> items;

    public OrderItemsAdapter(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_order_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        
        OrderItem item = items.get(position);
        if (item == null) return;
        
        // Set item name and quantity
        String name = item.getName() != null ? item.getName() : "";
        int quantity = Math.max(1, item.getQuantity()); // Ensure quantity is at least 1
        String itemText = quantity + "x " + name;
        
        // Add customizations to the item text
        if (item.getCustomizations() != null && !item.getCustomizations().isEmpty()) {
            StringBuilder customizationsText = new StringBuilder();
            for (OrderItem.Customization customization : item.getCustomizations()) {
                if (customization != null && customization.getChoice() != null) {
                    customizationsText.append("\n  • ").append(customization.getChoice());
                    if (customization.getPrice() > 0) {
                        customizationsText.append(" (+").append(NumberFormat.getCurrencyInstance().format(customization.getPrice())).append(")");
                    }
                }
            }
            itemText += customizationsText.toString();
        }
        holder.itemName.setText(itemText);
        
        // Handle special instructions
        String instructions = item.getSpecialInstructions();
        if (instructions != null && !instructions.trim().isEmpty()) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText("Note: " + instructions);
        } else {
            holder.specialInstructions.setVisibility(View.GONE);
        }
        
        // Set price
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        double price = Math.max(0, item.getPrice()); // Ensure price is not negative
        double totalPrice = price * quantity;
        
        // Add customization prices
        if (item.getCustomizations() != null) {
            for (OrderItem.Customization customization : item.getCustomizations()) {
                if (customization != null) {
                    totalPrice += customization.getPrice() * quantity;
                }
            }
        }
        holder.itemPrice.setText(format.format(totalPrice));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, specialInstructions, itemPrice;

        ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            specialInstructions = itemView.findViewById(R.id.specialInstructions);
            itemPrice = itemView.findViewById(R.id.itemPrice);
        }
    }
} 