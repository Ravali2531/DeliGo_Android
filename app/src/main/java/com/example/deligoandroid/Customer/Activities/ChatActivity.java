package com.example.deligoandroid.Customer.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView restaurantNameText;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private String orderId;
    private String restaurantId;
    private String restaurantName;
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get order details from intent
        orderId = getIntent().getStringExtra("orderId");
        restaurantId = getIntent().getStringExtra("restaurantId");
        restaurantName = getIntent().getStringExtra("restaurantName");

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        restaurantNameText = findViewById(R.id.restaurantNameText);

        restaurantNameText.setText(restaurantName);

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize Firebase chat reference
        chatRef = FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(orderId)
                .child("restaurant_customer_chat");

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
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        messages.add(message);
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
            messageMap.put("senderId", senderId);
            messageMap.put("message", messageText);
            messageMap.put("timestamp", timestamp);
            messageMap.put("senderType", "customer");

            chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> {
                messageInput.setText("");
            }).addOnFailureListener(e -> {
                Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static class ChatMessage {
        private String senderId;
        private String message;
        private long timestamp;
        private String senderType;

        public ChatMessage() {
            // Required empty constructor for Firebase
        }

        // Getters and setters
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }
    }
} 