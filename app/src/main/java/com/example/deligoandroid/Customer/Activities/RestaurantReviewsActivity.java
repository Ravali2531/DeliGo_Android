package com.example.deligoandroid.Customer.Activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.RestaurantReviewsAdapter;
import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestaurantReviewsActivity extends AppCompatActivity {
    private static final String TAG = "RestaurantReviewsActivity";
    private RecyclerView reviewsRecyclerView;
    private TextView noReviewsText;
    private RestaurantReviewsAdapter adapter;
    private List<Map<String, Object>> reviewComments;
    private List<Map<String, Object>> reviewRatings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_reviews);

        // Get restaurant ID from intent
        String restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId == null) {
            finish();
            return;
        }

        // Initialize views
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        noReviewsText = findViewById(R.id.noReviewsText);
        ImageButton backButton = findViewById(R.id.backButton);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Initialize reviews lists and adapter
        reviewComments = new ArrayList<>();
        reviewRatings = new ArrayList<>();
        adapter = new RestaurantReviewsAdapter(reviewComments, reviewRatings);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(adapter);

        // Load reviews
        loadReviews(restaurantId);
    }

    private void loadReviews(String restaurantId) {
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("ratingsandcomments");

        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewComments.clear();
                reviewRatings.clear();
                
                // Get comments
                DataSnapshot commentsSnapshot = snapshot.child("comment");
                DataSnapshot ratingsSnapshot = snapshot.child("rating");
                
                if (commentsSnapshot.exists()) {
                    for (DataSnapshot commentSnapshot : commentsSnapshot.getChildren()) {
                        String userId = commentSnapshot.getKey();
                        String comment = commentSnapshot.getValue(String.class);
                        
                        if (comment != null && !comment.isEmpty()) {
                            // Add comment
                            Map<String, Object> commentMap = new HashMap<>();
                            commentMap.put(userId, comment);
                            reviewComments.add(commentMap);

                            // Add corresponding rating
                            Map<String, Object> ratingMap = new HashMap<>();
                            if (ratingsSnapshot.hasChild(userId)) {
                                Float rating = ratingsSnapshot.child(userId).getValue(Float.class);
                                if (rating != null) {
                                    ratingMap.put(userId, rating);
                                } else {
                                    ratingMap.put(userId, 0f);
                                }
                            } else {
                                ratingMap.put(userId, 0f);
                            }
                            reviewRatings.add(ratingMap);
                        }
                    }
                }

                // Update UI
                if (reviewComments.isEmpty()) {
                    noReviewsText.setVisibility(View.VISIBLE);
                    reviewsRecyclerView.setVisibility(View.GONE);
                } else {
                    noReviewsText.setVisibility(View.GONE);
                    reviewsRecyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading reviews", error.toException());
            }
        });
    }
} 