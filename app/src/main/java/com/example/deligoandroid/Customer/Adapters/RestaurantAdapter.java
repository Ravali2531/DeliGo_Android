package com.example.deligoandroid.Customer.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Customer.Models.Restaurant;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {
    private Context context;
    private List<Restaurant> restaurants = new ArrayList<>();
    private OnRestaurantClickListener listener;

    public RestaurantAdapter(Context context) {
        this.context = context;
    }

    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants;
        notifyDataSetChanged();
    }

    public void setOnRestaurantClickListener(OnRestaurantClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView nameText;
        private TextView descriptionText;
        private TextView ratingText;
        private TextView statusText;

        RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.restaurantImage);
            nameText = itemView.findViewById(R.id.restaurantName);
            descriptionText = itemView.findViewById(R.id.restaurantDescription);
            ratingText = itemView.findViewById(R.id.restaurantRating);
            statusText = itemView.findViewById(R.id.restaurantStatus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onRestaurantClick(restaurants.get(position));
                }
            });
        }

        void bind(Restaurant restaurant) {
            nameText.setText(restaurant.getName());
            descriptionText.setText(restaurant.getDescription());
            ratingText.setText(String.format("%.1f", restaurant.getRating()));
            statusText.setText(restaurant.isOpen() ? "Open" : "Closed");
            statusText.setTextColor(context.getColor(
                restaurant.isOpen() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));

            if (restaurant.getImageUrl() != null && !restaurant.getImageUrl().isEmpty()) {
                Glide.with(context)
                    .load(restaurant.getImageUrl())
                    .centerCrop()
                    .into(imageView);
            }
        }
    }

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }
} 