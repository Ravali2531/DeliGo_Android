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
import java.util.Map;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {
    private List<OrderItem> items = new ArrayList<>();

    public OrderItemsAdapter(Map<String, OrderItem> itemsMap) {
        if (itemsMap != null) {
            this.items.addAll(itemsMap.values());
        }
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
        
        String itemText = item.getQuantity() + "x " + item.getName();
        holder.itemName.setText(itemText);
        
        if (item.getSpecialInstructions() != null && !item.getSpecialInstructions().isEmpty()) {
            holder.specialInstructions.setVisibility(View.VISIBLE);
            holder.specialInstructions.setText("Note: " + item.getSpecialInstructions());
        } else {
            holder.specialInstructions.setVisibility(View.GONE);
        }
        
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        double totalPrice = item.getPrice() * item.getQuantity();
        holder.itemPrice.setText(format.format(totalPrice));
    }

    @Override
    public int getItemCount() {
        return items.size();
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