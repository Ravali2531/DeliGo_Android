package com.example.deligoandroid.Admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.Admin.ManageUsersActivity;
import com.example.deligoandroid.Admin.UserDocument;
import com.example.deligoandroid.R;
import java.util.ArrayList;
import java.util.List;

public class UserDocumentsAdapter extends RecyclerView.Adapter<UserDocumentsAdapter.ViewHolder> {
    private List<UserDocument> documents = new ArrayList<>();
    private Context context;
    private String currentUserType;

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
        holder.userName.setText(document.name);
        holder.userEmail.setText("Email: " + document.email);
        holder.userPhone.setText("Phone: " + (document.phone != null ? document.phone : "Not provided"));
        holder.userAddress.setText("Address: " + (document.address != null ? document.address : "Not provided"));

        // Handle restaurant specific info
        if (currentUserType.equals("restaurants")) {
            holder.restaurantInfoLayout.setVisibility(View.VISIBLE);
            holder.restaurantName.setText("Restaurant: " + document.restaurantName);
            String hours = "Hours: " + document.openingHours + " - " + document.closingHours;
            holder.businessHours.setText(hours);
        } else {
            holder.restaurantInfoLayout.setVisibility(View.GONE);
        }

        // Handle documents section
        if (currentUserType.equals("drivers") || currentUserType.equals("restaurants")) {
            holder.documentsLayout.setVisibility(View.VISIBLE);
            
            if (document.documentsSubmitted != null && document.documentsSubmitted) {
                // Load document images
                Glide.with(context)
                    .load(document.document1Url)
                    .placeholder(R.drawable.id_placeholder)
                    .into(holder.document1Preview);

                Glide.with(context)
                    .load(document.document2Url)
                    .placeholder(R.drawable.id_placeholder)
                    .into(holder.document2Preview);

                // Show/hide approval buttons based on status
                boolean isPending = document.documentStatus == null || 
                                  document.documentStatus.equals("pending_review");
                holder.documentActionButtons.setVisibility(isPending ? View.VISIBLE : View.GONE);

                // Set click listeners for approve/reject
                if (isPending) {
                    holder.approveButton.setOnClickListener(v -> {
                        if (context instanceof ManageUsersActivity) {
                            ((ManageUsersActivity) context).updateDocumentStatus(document, "approved");
                        }
                    });

                    holder.rejectButton.setOnClickListener(v -> {
                        if (context instanceof ManageUsersActivity) {
                            ((ManageUsersActivity) context).updateDocumentStatus(document, "rejected");
                        }
                    });
                }
            } else {
                holder.documentsLayout.setVisibility(View.GONE);
            }
        } else {
            holder.documentsLayout.setVisibility(View.GONE);
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
        TextView userName, userEmail, userPhone, userAddress;
        TextView restaurantName, businessHours;
        ImageView document1Preview, document2Preview;
        Button approveButton, rejectButton;
        LinearLayout restaurantInfoLayout, documentsLayout, documentActionButtons;

        ViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            userPhone = itemView.findViewById(R.id.userPhone);
            userAddress = itemView.findViewById(R.id.userAddress);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            businessHours = itemView.findViewById(R.id.businessHours);
            document1Preview = itemView.findViewById(R.id.document1Preview);
            document2Preview = itemView.findViewById(R.id.document2Preview);
            approveButton = itemView.findViewById(R.id.approveButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            restaurantInfoLayout = itemView.findViewById(R.id.restaurantInfoLayout);
            documentsLayout = itemView.findViewById(R.id.documentsLayout);
            documentActionButtons = itemView.findViewById(R.id.documentActionButtons);
        }
    }
} 