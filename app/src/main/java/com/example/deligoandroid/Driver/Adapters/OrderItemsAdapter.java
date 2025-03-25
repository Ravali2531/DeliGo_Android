package com.example.deligoandroid.Driver.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Models.OrderItem;
import com.example.deligoandroid.R;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {
    private static final String TAG = "OrderItemsAdapter";
    private List<OrderItem> items;
    private NumberFormat currencyFormat;

    public OrderItemsAdapter(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) {
            Log.e(TAG, "Invalid position: " + position);
            return;
        }
        
        OrderItem item = items.get(position);
        if (item == null) {
            Log.e(TAG, "Null item at position: " + position);
            return;
        }
        
        try {
            // Set item name and quantity
            String name = item.getName() != null ? item.getName() : "";
            int quantity = Math.max(1, item.getQuantity()); // Ensure quantity is at least 1
            String itemText = quantity + "x " + name;
            
            Log.d(TAG, "Binding item: " + name + ", quantity: " + quantity);
            holder.itemName.setText(itemText);
            
            // Calculate item total price
            double price = Math.max(0, item.getPrice()); // Ensure price is not negative
            double itemTotal = price * quantity;
            double customizationsTotal = 0.0;
            
            // Handle customizations
            StringBuilder customizationsText = new StringBuilder();
            List<OrderItem.CustomizationOption> customizations = item.getCustomizations();
            
            if (customizations != null && !customizations.isEmpty()) {
                Log.d(TAG, "Processing " + customizations.size() + " customization options for item: " + name);
                
                for (OrderItem.CustomizationOption option : customizations) {
                    if (option == null) continue;
                    
                    List<OrderItem.SelectedItem> selectedItems = option.getSelectedItems();
                    if (selectedItems != null && !selectedItems.isEmpty()) {
                        for (OrderItem.SelectedItem selectedItem : selectedItems) {
                            if (selectedItem == null || selectedItem.getName() == null) continue;
                            
                            double selectedItemPrice = selectedItem.getPrice();
                            customizationsText.append("\n  + ").append(selectedItem.getName());
                            if (selectedItemPrice > 0) {
                                customizationsText.append(" (+").append(currencyFormat.format(selectedItemPrice)).append(")");
                                customizationsTotal += selectedItemPrice * quantity;
                            }
                            Log.d(TAG, "Added customization: " + selectedItem.getName() + 
                                  " with price: " + selectedItemPrice);
                        }
                    }
                }
            }
            
            // Show customizations if any exist
            if (customizationsText.length() > 0) {
                holder.itemCustomizations.setVisibility(View.VISIBLE);
                holder.itemCustomizations.setText(customizationsText.toString());
                Log.d(TAG, "Set customizations text: " + customizationsText.toString());
            } else {
                holder.itemCustomizations.setVisibility(View.GONE);
                Log.d(TAG, "No customizations to display");
            }
            
            // Set final item price including customizations
            double totalPrice = itemTotal + customizationsTotal;
            holder.itemPrice.setText(currencyFormat.format(totalPrice));
            Log.d(TAG, "Set total price: " + currencyFormat.format(totalPrice) + 
                  " (base: " + itemTotal + ", customizations: " + customizationsTotal + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error binding item at position " + position, e);
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName;
        public TextView itemPrice;
        public TextView itemCustomizations;

        public ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.itemName);
            itemPrice = view.findViewById(R.id.itemPrice);
            itemCustomizations = view.findViewById(R.id.itemCustomizations);
        }
    }
} 