package com.example.deligoandroid.Restaurant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.BestSellingDishesActivity.BestSellingDish;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BestSellingDishesAdapter extends RecyclerView.Adapter<BestSellingDishesAdapter.DishViewHolder> {
    private Context context;
    private List<BestSellingDish> items = new ArrayList<>();
    
    public BestSellingDishesAdapter(Context context) {
        this.context = context;
    }
    
    public void setItems(List<BestSellingDish> items) {
        this.items = new ArrayList<>(items);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public DishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_best_selling_dish, parent, false);
        return new DishViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DishViewHolder holder, int position) {
        BestSellingDish dish = items.get(position);
        holder.bind(dish);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    class DishViewHolder extends RecyclerView.ViewHolder {
        private ImageView dishImage;
        private TextView dishName;
        private TextView dishCategory;
        private TextView salesCount;
        private TextView dishPrice;
        
        DishViewHolder(@NonNull View itemView) {
            super(itemView);
            dishImage = itemView.findViewById(R.id.dishImage);
            dishName = itemView.findViewById(R.id.dishName);
            dishCategory = itemView.findViewById(R.id.dishCategory);
            salesCount = itemView.findViewById(R.id.salesCount);
            dishPrice = itemView.findViewById(R.id.dishPrice);
        }
        
        void bind(BestSellingDish dish) {
            // Set dish name
            dishName.setText(dish.getMenuItem().getName());
            
            // Set category (default to "Main Course" if not set)
            String category = dish.getMenuItem().getCategory();
            if (category == null || category.isEmpty()) {
                category = "Main Course";
            }
            dishCategory.setText(category);
            
            // Set sales count
            salesCount.setText(String.format(Locale.getDefault(), "%d sold", dish.getSalesCount()));
            
            // Set price
            dishPrice.setText(String.format(Locale.getDefault(), "%.2f", dish.getMenuItem().getPrice()));
            
            // Load image
            String imageUrl = dish.getMenuItem().getImageURL();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_food)
                    .error(R.drawable.placeholder_food)
                    .centerCrop()
                    .into(dishImage);
            } else {
                dishImage.setImageResource(R.drawable.placeholder_food);
            }
        }
    }
} 