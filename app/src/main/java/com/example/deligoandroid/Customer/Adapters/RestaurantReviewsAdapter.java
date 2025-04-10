package com.example.deligoandroid.Customer.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class RestaurantReviewsAdapter extends RecyclerView.Adapter<RestaurantReviewsAdapter.ViewHolder> {
    private static final String TAG = "RestaurantReviewsAdapter";
    private List<Map<String, Object>> reviewsComments;
    private List<Map<String, Object>> reviewsRatings;
    private DatabaseReference customersRef;

    public RestaurantReviewsAdapter(List<Map<String, Object>> reviewsComments, List<Map<String, Object>> reviewsRatings) {
        this.reviewsComments = reviewsComments;
        this.reviewsRatings = reviewsRatings;
        this.customersRef = FirebaseDatabase.getInstance().getReference().child("customers");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_restaurant_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Map<String, Object> reviewComment = reviewsComments.get(position);
            Map<String, Object> reviewRating = reviewsRatings.get(position);
            
            if (reviewComment == null || reviewRating == null) {
                Log.e(TAG, "Review at position " + position + " is null");
                setDefaultValues(holder);
                return;
            }

            // Get customerId and review data
            String customerId = reviewComment.keySet().iterator().next();
            String comment = null;
            Float rating = null;

            try {
                // Get comment
                comment = (String) reviewComment.get(customerId);
                
                // Get rating
                Object ratingObj = reviewRating.get(customerId);
                if (ratingObj instanceof Number) {
                    rating = ((Number) ratingObj).floatValue();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing review data: " + e.getMessage());
            }

            // Set customer name
            final String finalCustomerId = customerId;
            if (customerId != null) {
                customersRef.child(customerId).child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            String name = snapshot.getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                holder.customerName.setText(name);
                            } else {
                                holder.customerName.setText("Customer " + finalCustomerId.substring(0, 5));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting customer name: " + e.getMessage());
                            holder.customerName.setText("Customer " + finalCustomerId.substring(0, 5));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error fetching customer name: " + error.getMessage());
                        holder.customerName.setText("Customer " + finalCustomerId.substring(0, 5));
                    }
                });
            } else {
                holder.customerName.setText("Anonymous");
            }

            // Set rating
            if (rating != null) {
                holder.ratingBar.setRating(rating);
            } else {
                holder.ratingBar.setRating(0);
            }

            // Set review text
            if (comment != null && !comment.isEmpty()) {
                holder.reviewText.setText(comment);
            } else {
                holder.reviewText.setText("No comment provided");
            }

            // Hide date view as we don't have timestamp data
            holder.reviewDate.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder: " + e.getMessage());
            setDefaultValues(holder);
        }
    }

    private void setDefaultValues(ViewHolder holder) {
        holder.customerName.setText("Anonymous");
        holder.ratingBar.setRating(0);
        holder.reviewText.setText("No comment provided");
        holder.reviewDate.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return reviewsComments != null ? reviewsComments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RatingBar ratingBar;
        TextView reviewText;
        TextView reviewDate;
        TextView customerName;

        ViewHolder(View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            reviewText = itemView.findViewById(R.id.reviewText);
            reviewDate = itemView.findViewById(R.id.reviewDate);
        }
    }
} 