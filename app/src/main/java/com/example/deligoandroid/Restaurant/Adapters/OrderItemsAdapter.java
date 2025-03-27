package com.example.deligoandroid.Restaurant.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Models.OrderItem;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        OrderItem item = items.get(position);
        Log.d("RestaurantOrderItemsAdapter", "Binding item: " + item.getName() + ", quantity: " + item.getQuantity());

        // Set item name and base price
        holder.itemName.setText(item.getQuantity() + "x " + item.getName());
        double basePrice = item.getPrice();
        holder.itemPrice.setText(NumberFormat.getCurrencyInstance(Locale.US).format(basePrice));

        // Handle customizations
        StringBuilder customizationsText = new StringBuilder();
        double customizationTotal = 0.0;

        if (item.getCustomizations() != null && !item.getCustomizations().isEmpty()) {
            for (OrderItem.CustomizationOption option : item.getCustomizations()) {
                if (option.getSelectedItems() != null && !option.getSelectedItems().isEmpty()) {
                    for (OrderItem.SelectedItem selectedItem : option.getSelectedItems()) {
                        customizationsText.append("\n+ ").append(selectedItem.getName())
                            .append(" (+$").append(String.format("%.2f", selectedItem.getPrice())).append(")");
                        customizationTotal += selectedItem.getPrice();
                    }
                }
            }
            Log.d("RestaurantOrderItemsAdapter", "Added customizations: " + customizationsText.toString());
        } else {
            Log.d("RestaurantOrderItemsAdapter", "No customizations to display");
        }

        // Set customizations text if any
        if (customizationsText.length() > 0) {
            holder.itemCustomizations.setVisibility(View.VISIBLE);
            holder.itemCustomizations.setText(customizationsText.toString());
            
            // Show subtotal when there are customizations
            double subtotal = (basePrice + customizationTotal) * item.getQuantity();
            holder.itemSubtotal.setVisibility(View.VISIBLE);
            holder.itemSubtotal.setText("Subtotal: " + NumberFormat.getCurrencyInstance(Locale.US).format(subtotal));
        } else {
            holder.itemCustomizations.setVisibility(View.GONE);
            // Only show subtotal if quantity > 1
            if (item.getQuantity() > 1) {
                double subtotal = basePrice * item.getQuantity();
                holder.itemSubtotal.setVisibility(View.VISIBLE);
                holder.itemSubtotal.setText("Subtotal: " + NumberFormat.getCurrencyInstance(Locale.US).format(subtotal));
            } else {
                holder.itemSubtotal.setVisibility(View.GONE);
            }
        }

        // Handle special instructions
        String instructions = item.getSpecialInstructions();
        if (instructions != null && !instructions.trim().isEmpty()) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText("Note: " + instructions);
        } else {
            holder.specialInstructions.setVisibility(View.GONE);
        }
        
        Log.d("RestaurantOrderItemsAdapter", "Item: " + item.getName() + 
            ", Base price: $" + String.format("%.2f", basePrice) + 
            ", Customizations: $" + String.format("%.2f", customizationTotal) + 
            ", Quantity: " + item.getQuantity());
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, specialInstructions, itemPrice, itemCustomizations, itemSubtotal;

        ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            specialInstructions = itemView.findViewById(R.id.specialInstructions);
            itemPrice = itemView.findViewById(R.id.itemPrice);
            itemCustomizations = itemView.findViewById(R.id.itemCustomizations);
            itemSubtotal = itemView.findViewById(R.id.itemSubtotal);
        }
    }
} 