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
        if (position < 0 || position >= items.size()) return;
        
        OrderItem item = items.get(position);
        if (item == null) return;
        
        // Set item name and quantity
        String name = item.getName() != null ? item.getName() : "";
        int quantity = Math.max(1, item.getQuantity()); // Ensure quantity is at least 1
        String itemText = quantity + "x " + name;
        
        Log.d("OrderItemsAdapter", "Binding item: " + name + ", quantity: " + quantity);
        
        // Add customizations to the item text
        List<OrderItem.CustomizationOption> customizations = item.getCustomizations();
        if (customizations != null && !customizations.isEmpty()) {
            Log.d("OrderItemsAdapter", "Item has " + customizations.size() + " customization options");
            StringBuilder customizationsText = new StringBuilder();
            
            // Process each customization option
            for (OrderItem.CustomizationOption option : customizations) {
                String optionName = option.getOptionName();
                List<OrderItem.SelectedItem> selectedItems = option.getSelectedItems();
                
                Log.d("OrderItemsAdapter", "Processing option: " + optionName + 
                    " with " + (selectedItems != null ? selectedItems.size() : 0) + " selected items");
                
                if (selectedItems != null && !selectedItems.isEmpty()) {
                    customizationsText.append("\n  ").append(optionName).append(":");
                    
                    for (OrderItem.SelectedItem selectedItem : selectedItems) {
                        String selectedItemName = selectedItem.getName();
                        double selectedItemPrice = selectedItem.getPrice();
                        Log.d("OrderItemsAdapter", "Selected item: " + selectedItemName + 
                            ", price: " + selectedItemPrice);
                        
                        customizationsText.append("\n    • ").append(selectedItemName);
                        if (selectedItemPrice > 0) {
                            customizationsText.append(" (+")
                                .append(NumberFormat.getCurrencyInstance().format(selectedItemPrice))
                                .append(")");
                        }
                    }
                }
            }
            itemText += customizationsText.toString();
            Log.d("OrderItemsAdapter", "Final item text with customizations: " + itemText);
        } else {
            Log.d("OrderItemsAdapter", "Item has no customizations");
        }
        holder.itemName.setText(itemText);
        
        // Handle special instructions
        String instructions = item.getSpecialInstructions();
        if (instructions != null && !instructions.trim().isEmpty()) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText("Note: " + instructions);
            Log.d("OrderItemsAdapter", "Special instructions: " + instructions);
        } else {
            holder.specialInstructions.setVisibility(View.GONE);
        }
        
        // Set price
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        double price = Math.max(0, item.getPrice()); // Ensure price is not negative
        double totalPrice = price * quantity;
        
        // Add customization prices
        if (customizations != null) {
            for (OrderItem.CustomizationOption option : customizations) {
                if (option.getSelectedItems() != null) {
                    for (OrderItem.SelectedItem selectedItem : option.getSelectedItems()) {
                        totalPrice += selectedItem.getPrice() * quantity;
                    }
                }
            }
        }
        holder.itemPrice.setText(format.format(totalPrice));
        Log.d("OrderItemsAdapter", "Total price: " + format.format(totalPrice));
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