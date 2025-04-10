package com.example.deligoandroid.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.Models.CartItem;
import com.example.deligoandroid.R;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class CartItemsAdapter extends RecyclerView.Adapter<CartItemsAdapter.CartItemViewHolder> {
    private List<CartItem> cartItems;
    private CartItemListener listener;

    public interface CartItemListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    public CartItemsAdapter(List<CartItem> cartItems, CartItemListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView quantityText;
        private TextView priceText;
        private MaterialButton removeButton;
        private ImageButton decreaseQuantity;
        private ImageButton increaseQuantity;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.itemName);
            quantityText = itemView.findViewById(R.id.quantityText);
            priceText = itemView.findViewById(R.id.itemPrice);
            removeButton = itemView.findViewById(R.id.removeButton);
            decreaseQuantity = itemView.findViewById(R.id.decreaseQuantity);
            increaseQuantity = itemView.findViewById(R.id.increaseQuantity);
        }

        public void bind(CartItem item, CartItemListener listener) {
            nameText.setText(item.getName());
            quantityText.setText(String.valueOf(item.getQuantity()));
            priceText.setText(String.format("$%.2f", item.getTotalPrice()));

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveItem(item);
                }
            });

            decreaseQuantity.setOnClickListener(v -> {
                if (listener != null && item.getQuantity() > 1) {
                    listener.onQuantityChanged(item, item.getQuantity() - 1);
                }
            });

            increaseQuantity.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuantityChanged(item, item.getQuantity() + 1);
                }
            });

            // Enable/disable decrease button based on quantity
            decreaseQuantity.setEnabled(item.getQuantity() > 1);
            decreaseQuantity.setAlpha(item.getQuantity() > 1 ? 1.0f : 0.5f);
        }
    }
} 