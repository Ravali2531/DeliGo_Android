package com.example.deligoandroid.Admin;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class UserDocumentsAdapter extends RecyclerView.Adapter<UserDocumentsAdapter.ViewHolder> {
    private List<UserDocument> documents = new ArrayList<>();
    private String currentUserType;
    private Context context;
    private DatabaseReference databaseRef;

    public UserDocumentsAdapter() {
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_user_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserDocument document = documents.get(position);
        
        // Set basic info
        holder.restaurantName.setText(document.restaurantName != null ? document.restaurantName : document.name);
        holder.userEmail.setText(document.email);
        holder.userPhone.setText(document.phone != null ? document.phone : "Not provided");
        
        // Show status for restaurants and drivers
        if (currentUserType.equals("restaurants") || currentUserType.equals("drivers")) {
            holder.restaurantStatus.setVisibility(View.VISIBLE);
            String status = "Status: ";
            if (document.documentStatus != null) {
                status += "\"" + document.documentStatus + "\"";
            } else if (document.documentsSubmitted != null && document.documentsSubmitted) {
                status += "\"Documents Submitted\"";
            } else {
                status += "\"Pending Documents\"";
            }
            holder.restaurantStatus.setText(status);
        } else {
            holder.restaurantStatus.setVisibility(View.GONE);
        }

        // Set block button text and click listener
        boolean isBlocked = document.blocked != null && document.blocked;
        holder.blockButton.setVisibility(View.VISIBLE);
        holder.blockedStatus.setVisibility(isBlocked ? View.VISIBLE : View.GONE);
        
        if (isBlocked) {
            holder.blockButton.setText("Unblock User");
            holder.blockButton.setIcon(context.getDrawable(R.drawable.ic_unlock));
            holder.blockButton.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.green)));
        } else {
            holder.blockButton.setText("Block User");
            holder.blockButton.setIcon(context.getDrawable(R.drawable.ic_lock));
            holder.blockButton.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.red)));
        }

        holder.blockButton.setOnClickListener(v -> {
            boolean newBlockStatus = !isBlocked;
            databaseRef.child(currentUserType).child(document.userId).child("blocked")
                .setValue(newBlockStatus)
                .addOnSuccessListener(aVoid -> {
                    document.blocked = newBlockStatus;
                    notifyItemChanged(position);
                    Toast.makeText(context, 
                        newBlockStatus ? "User blocked successfully" : "User unblocked successfully", 
                        Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, 
                        "Failed to update block status: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        });

        // Set click listener based on user type
        holder.itemView.setOnClickListener(v -> {
            if (currentUserType.equals("restaurants")) {
                Intent intent = new Intent(context, RestaurantDetailsActivity.class);
                intent.putExtra("restaurantId", document.userId);
                context.startActivity(intent);
            } else if (currentUserType.equals("drivers")) {
                Intent intent = new Intent(context, DriverDetailsActivity.class);
                intent.putExtra("driver_id", document.userId);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public void setDocuments(List<UserDocument> documents, String userType) {
        this.documents = documents;
        this.currentUserType = userType;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView restaurantName, userEmail, userPhone, restaurantStatus, blockedStatus;
        MaterialButton blockButton;

        ViewHolder(View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            userEmail = itemView.findViewById(R.id.userEmail);
            userPhone = itemView.findViewById(R.id.userPhone);
            restaurantStatus = itemView.findViewById(R.id.restaurantStatus);
            blockButton = itemView.findViewById(R.id.blockButton);
            blockedStatus = itemView.findViewById(R.id.blockedStatus);
        }
    }
} 