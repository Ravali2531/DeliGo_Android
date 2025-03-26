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
                .inflate(R.layout.item_order_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        OrderItem item = items.get(position);
        Log.d("DriverOrderItemsAdapter", "Binding item: " + item.getName() + ", quantity: " + item.getQuantity());

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
            Log.d("DriverOrderItemsAdapter", "Added customizations: " + customizationsText.toString());
        } else {
            Log.d("DriverOrderItemsAdapter", "No customizations to display");
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
        
        Log.d("DriverOrderItemsAdapter", "Item: " + item.getName() + 
            ", Base price: $" + String.format("%.2f", basePrice) + 
            ", Customizations: $" + String.format("%.2f", customizationTotal) + 
            ", Quantity: " + item.getQuantity());
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName;
        public TextView itemPrice;
        public TextView itemCustomizations;
        public TextView itemSubtotal;

        public ViewHolder(View view) {
            super(view);
            itemName = view.findViewById(R.id.itemName);
            itemPrice = view.findViewById(R.id.itemPrice);
            itemCustomizations = view.findViewById(R.id.itemCustomizations);
            itemSubtotal = view.findViewById(R.id.itemSubtotal);
        }
    }
} 