package com.example.deligoandroid.Admin;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;

public class UserDocumentsAdapter extends RecyclerView.Adapter<UserDocumentsAdapter.ViewHolder> {
    private List<UserDocument> documents = new ArrayList<>();
    private String currentUserType;
    private Context context;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
            .inflate(R.layout.item_user_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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

        // Set click listener for restaurants
        if (currentUserType.equals("restaurants")) {
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RestaurantDetailsActivity.class);
                intent.putExtra("restaurantId", document.userId);
                context.startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
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
        TextView restaurantName, userEmail, userPhone, restaurantStatus;

        ViewHolder(View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            userEmail = itemView.findViewById(R.id.userEmail);
            userPhone = itemView.findViewById(R.id.userPhone);
            restaurantStatus = itemView.findViewById(R.id.restaurantStatus);
        }
    }
} 