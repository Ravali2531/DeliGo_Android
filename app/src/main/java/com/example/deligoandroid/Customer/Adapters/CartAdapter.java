package com.example.deligoandroid.Customer.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Models.CartItem;
import com.example.deligoandroid.Customer.Models.CustomizationSelection;
import com.example.deligoandroid.Customer.Models.SelectedItem;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private Context context;
    private List<CartItem> items;
    private CartItemListener listener;

    public interface CartItemListener {
        void onUpdateQuantity(String itemId, int newQuantity);
        void onRemoveItem(String itemId);
    }

    public CartAdapter(Context context, CartItemListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        
        // Set item details
        holder.nameText.setText(item.getName());
        holder.descriptionText.setText(item.getDescription());
        holder.priceText.setText(String.format("$%.2f", item.getPrice()));
        holder.quantityText.setText(String.valueOf(item.getQuantity()));

        // Load image
        if (item.getImageURL() != null && !item.getImageURL().isEmpty()) {
            Glide.with(context)
                .load(item.getImageURL())
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .into(holder.itemImage);
        } else {
            holder.itemImage.setImageResource(R.drawable.ic_food_placeholder);
        }

        // Setup customizations
        if (!item.getCustomizations().isEmpty()) {
            holder.customizationsContainer.setVisibility(View.VISIBLE);
            setupCustomizations(holder.customizationsContainer, item.getCustomizations());
        } else {
            holder.customizationsContainer.setVisibility(View.GONE);
        }

        // Setup quantity controls
        holder.decreaseButton.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                listener.onUpdateQuantity(item.getId(), item.getQuantity() - 1);
            }
        });

        holder.increaseButton.setOnClickListener(v -> {
            listener.onUpdateQuantity(item.getId(), item.getQuantity() + 1);
        });

        // Setup remove button
        holder.removeButton.setOnClickListener(v -> listener.onRemoveItem(item.getId()));
    }

    private void setupCustomizations(ViewGroup container, Map<String, List<CustomizationSelection>> customizations) {
        container.removeAllViews();
        
        // Add "Customizations" header
        TextView header = new TextView(context);
        header.setText("Customizations");
        header.setTextAppearance(context, android.R.style.TextAppearance_Medium);
        header.setPadding(0, 8, 0, 8);
        container.addView(header);

        // Add each customization group
        for (List<CustomizationSelection> selections : customizations.values()) {
            for (CustomizationSelection selection : selections) {
                // Add option name
                TextView optionName = new TextView(context);
                optionName.setText(selection.getOptionName());
                optionName.setTextAppearance(context, android.R.style.TextAppearance_Small);
                container.addView(optionName);

                // Add selected items
                for (SelectedItem selectedItem : selection.getSelectedItems()) {
                    TextView itemText = new TextView(context);
                    String text = "• " + selectedItem.getName();
                    if (selectedItem.getPrice() > 0) {
                        text += String.format(" (+$%.2f)", selectedItem.getPrice());
                    }
                    itemText.setText(text);
                    itemText.setTextAppearance(context, android.R.style.TextAppearance_Small);
                    itemText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                    itemText.setPadding(16, 2, 0, 2);
                    container.addView(itemText);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView nameText;
        TextView descriptionText;
        TextView priceText;
        TextView quantityText;
        ImageButton decreaseButton;
        ImageButton increaseButton;
        Button removeButton;
        ViewGroup customizationsContainer;

        CartViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.itemImage);
            nameText = itemView.findViewById(R.id.itemName);
            descriptionText = itemView.findViewById(R.id.itemDescription);
            priceText = itemView.findViewById(R.id.itemPrice);
            quantityText = itemView.findViewById(R.id.quantityText);
            decreaseButton = itemView.findViewById(R.id.decreaseQuantity);
            increaseButton = itemView.findViewById(R.id.increaseQuantity);
            removeButton = itemView.findViewById(R.id.removeButton);
            customizationsContainer = itemView.findViewById(R.id.customizationsContainer);
        }
    }
} 