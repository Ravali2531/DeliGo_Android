package com.example.deligoandroid.Customer.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Customer.Models.OrderItem;
import com.example.deligoandroid.R;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerOrderItemsAdapter extends RecyclerView.Adapter<CustomerOrderItemsAdapter.ViewHolder> {
    private static final String TAG = "CustomerOrderItemsAdapter";
    private List<OrderItem> items;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public CustomerOrderItemsAdapter(List<Map<String, Object>> itemMaps, Map<String, Object> customizations) {
        this.items = new ArrayList<>();
        if (itemMaps != null) {
            for (Map<String, Object> itemMap : itemMaps) {
                OrderItem item = OrderItem.fromMap(itemMap, customizations);
                items.add(item);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            OrderItem item = items.get(position);
            
            // Set item name and quantity
            String name = item.getName() != null ? item.getName() : "";
            int quantity = Math.max(1, item.getQuantity());
            holder.itemName.setText(quantity + "x " + name);
            
            Log.d(TAG, "Binding item: " + name + ", quantity: " + quantity);
            
            // Build customizations text
            StringBuilder customizationsText = new StringBuilder();
            List<OrderItem.CustomizationOption> customizations = item.getCustomizations();
            
            if (customizations != null && !customizations.isEmpty()) {
                Log.d(TAG, "Item has " + customizations.size() + " customization options");
                
                // Process each customization option
                for (OrderItem.CustomizationOption option : customizations) {
                    String optionName = option.getOptionName();
                    List<OrderItem.SelectedItem> selectedItems = option.getSelectedItems();
                    
                    Log.d(TAG, "Processing option: " + optionName + 
                        " with " + (selectedItems != null ? selectedItems.size() : 0) + " selected items");
                    
                    if (selectedItems != null && !selectedItems.isEmpty()) {
                        customizationsText.append("\n• ").append(optionName).append(":");
                        
                        for (OrderItem.SelectedItem selectedItem : selectedItems) {
                            String selectedItemName = selectedItem.getName();
                            double selectedItemPrice = selectedItem.getPrice();
                            Log.d(TAG, "Selected item: " + selectedItemName + ", price: " + selectedItemPrice);
                            
                            customizationsText.append("\n  - ").append(selectedItemName);
                            if (selectedItemPrice > 0) {
                                customizationsText.append(" (+").append(currencyFormat.format(selectedItemPrice)).append(")");
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "Item has no customizations");
            }
            
            // Set customizations text if any
            if (customizationsText.length() > 0) {
                holder.itemCustomizations.setVisibility(View.VISIBLE);
                holder.itemCustomizations.setText(customizationsText.toString());
            } else {
                holder.itemCustomizations.setVisibility(View.GONE);
            }
            
            // Calculate total price
            double basePrice = Math.max(0, item.getPrice());
            double totalPrice = basePrice * quantity;
            
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
            
            // Set total price
            holder.itemPrice.setText(currencyFormat.format(totalPrice));
            Log.d(TAG, "Total price: " + currencyFormat.format(totalPrice));
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding item at position " + position + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName;
        TextView itemCustomizations;
        TextView itemPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemCustomizations = itemView.findViewById(R.id.itemCustomizations);
            itemPrice = itemView.findViewById(R.id.itemPrice);
        }
    }
} 