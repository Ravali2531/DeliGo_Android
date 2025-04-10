package com.example.deligoandroid.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatAdapter extends RecyclerView.Adapter<AdminChatAdapter.MessageViewHolder> {
    private final List<AdminChatActivity.ChatMessage> messages;
    private final String currentUserId;

    public AdminChatAdapter(List<AdminChatActivity.ChatMessage> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        AdminChatActivity.ChatMessage message = messages.get(position);
        holder.messageText.setText(message.getMessage());
        
        // Show sender info for group chat
        holder.senderInfo.setVisibility(View.VISIBLE);
        holder.senderName.setText(message.getSenderName());
        holder.senderType.setText(message.getSenderType());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(message.getTimestamp()));
        holder.timestamp.setText(time);

        // Align messages based on sender type
        if (message.getSenderType().equals("admin")) {
            // Admin messages on the right
            holder.messageLayout.setGravity(android.view.Gravity.END);
            holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_sent);
            holder.senderInfo.setGravity(android.view.Gravity.END);
        } else {
            // Other messages on the left
            holder.messageLayout.setGravity(android.view.Gravity.START);
            holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_received);
            holder.senderInfo.setGravity(android.view.Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderName, senderType, timestamp;
        LinearLayout messageLayout, senderInfo;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderName = itemView.findViewById(R.id.senderName);
            senderType = itemView.findViewById(R.id.senderType);
            timestamp = itemView.findViewById(R.id.timestamp);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            senderInfo = itemView.findViewById(R.id.senderInfo);
        }
    }
} 