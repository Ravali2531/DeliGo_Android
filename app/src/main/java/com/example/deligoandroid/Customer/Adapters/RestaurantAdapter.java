package com.example.deligoandroid.Customer.Adapters;

import android.content.Context;
import android.util.Log;
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
        this.restaurants = restaurants != null ? restaurants : new ArrayList<>();
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
        try {
            Restaurant restaurant = restaurants.get(position);
            holder.bind(restaurant);
        } catch (Exception e) {
            Log.e("RestaurantAdapter", "Error binding restaurant at position " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    class RestaurantViewHolder extends RecyclerView.ViewHolder {
        private ImageView restaurantImage;
        private ImageView placeholderImage;
        private TextView nameText;
        private TextView cuisineText;
        private TextView ratingText;
        private TextView numberOfRatingsText;
        private TextView statusBadge;

        RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            restaurantImage = itemView.findViewById(R.id.restaurantImage);
            placeholderImage = itemView.findViewById(R.id.placeholderImage);
            nameText = itemView.findViewById(R.id.restaurantName);
            cuisineText = itemView.findViewById(R.id.cuisineType);
            ratingText = itemView.findViewById(R.id.ratingText);
            numberOfRatingsText = itemView.findViewById(R.id.numberOfRatings);
            statusBadge = itemView.findViewById(R.id.statusBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    try {
                        listener.onRestaurantClick(restaurants.get(position));
                    } catch (Exception e) {
                        Log.e("RestaurantAdapter", "Error handling restaurant click", e);
                    }
                }
            });
        }

        void bind(Restaurant restaurant) {
            try {
                if (restaurant == null) {
                    Log.e("RestaurantAdapter", "Attempted to bind null restaurant");
                    return;
                }

                nameText.setText(restaurant.getName());
                cuisineText.setText(restaurant.getCuisine());
                ratingText.setText(String.format("%.1f", restaurant.getRating()));
                numberOfRatingsText.setText(String.format("(%d)", restaurant.getNumberOfRatings()));

                // Handle image loading
                String imageUrl = restaurant.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    restaurantImage.setVisibility(View.VISIBLE);
                    placeholderImage.setVisibility(View.GONE);
                    Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_restaurant)
                        .error(R.drawable.ic_restaurant)
                        .centerCrop()
                        .into(restaurantImage);
                } else {
                    restaurantImage.setVisibility(View.GONE);
                    placeholderImage.setVisibility(View.VISIBLE);
                }

                // Handle status badge
                if (!restaurant.isOpen()) {
                    statusBadge.setVisibility(View.VISIBLE);
                    statusBadge.setText("CLOSED");
                } else {
                    statusBadge.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("RestaurantAdapter", "Error binding restaurant data", e);
            }
        }
    }

    public interface OnRestaurantClickListener {
        void onRestaurantClick(Restaurant restaurant);
    }
} 