package com.example.deligoandroid.Customer.Activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.Customer.Adapters.ChatAdapter;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView chatTitleText;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private String orderId;
    private String chatType;
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get order details from intent
        orderId = getIntent().getStringExtra("orderId");
        chatType = getIntent().getStringExtra("chatType");

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatTitleText = findViewById(R.id.chatTitleText);

        // Set chat title based on type
        if (chatType.equals("group")) {
            chatTitleText.setText("Group Chat");
        } else if (chatType.equals("restaurant")) {
            chatTitleText.setText("Chat with Restaurant");
        } else if (chatType.equals("driver")) {
            chatTitleText.setText("Chat with Driver");
        }

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize Firebase chat reference based on chat type
        DatabaseReference orderRef = FirebaseDatabase.getInstance().getReference("orders").child(orderId);
        if (chatType.equals("group")) {
            chatRef = orderRef.child("group_chat");
        } else if (chatType.equals("restaurant")) {
            chatRef = orderRef.child("messages");
        } else if (chatType.equals("driver")) {
            chatRef = orderRef.child("driver_customer_messages");
        }

        // Load existing messages
        loadMessages();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messages.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue(String.class);
                    String senderId = snapshot.child("senderId").getValue(String.class);
                    String senderName = snapshot.child("senderName").getValue(String.class);
                    String senderType = snapshot.child("senderType").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    if (message != null && senderId != null && timestamp != null) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setMessage(message);
                        chatMessage.setSenderId(senderId);
                        chatMessage.setSenderName(senderName);
                        chatMessage.setSenderType(senderType);
                        chatMessage.setTimestamp(timestamp);
                        messages.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            long timestamp = System.currentTimeMillis();

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", messageText);
            messageMap.put("senderId", senderId);
            messageMap.put("senderType", "customer");
            messageMap.put("timestamp", timestamp);

            // Get customer name from Firebase
            FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(senderId)
                .child("fullName")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String senderName = task.getResult().getValue(String.class);
                        messageMap.put("senderName", senderName != null ? senderName : "Customer");
                        
                        // Send message to the correct reference based on chat type
                        DatabaseReference messageRef = chatRef.push();
                        messageRef.setValue(messageMap)
                            .addOnSuccessListener(aVoid -> {
                                messageInput.setText("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        messageMap.put("senderName", "Customer");
                        chatRef.push().setValue(messageMap)
                            .addOnSuccessListener(aVoid -> {
                                messageInput.setText("");
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                            });
                    }
                });
        }
    }

    public static class ChatMessage {
        private String message;
        private String senderId;
        private String senderName;
        private String senderType;
        private long timestamp;

        public ChatMessage() {
            // Required empty constructor for Firebase
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
        private List<ChatMessage> messages;

        public ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
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
            ChatMessage message = messages.get(position);
            holder.messageText.setText(message.getMessage());
            
            // Show sender info only for group chats
            if (chatType.equals("group")) {
                holder.senderInfo.setVisibility(View.VISIBLE);
                holder.senderName.setText(message.getSenderName());
                holder.senderType.setText(message.getSenderType());
            } else {
                holder.senderInfo.setVisibility(View.GONE);
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(message.getTimestamp()));
            holder.timestamp.setText(time);

            // Align messages based on sender type
            if (message.getSenderType().equals("customer")) {
                holder.messageLayout.setGravity(android.view.Gravity.END);
                holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_sent);
            } else {
                holder.messageLayout.setGravity(android.view.Gravity.START);
                holder.messageText.setBackgroundResource(R.drawable.bg_chat_message_received);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
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
} 