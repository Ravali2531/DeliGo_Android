package com.example.deligoandroid.Restaurant.Activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deligoandroid.R;
import com.example.deligoandroid.Restaurant.Adapters.RestaurantChatAdapter;
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

public class RestaurantChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView customerNameText;
    private RestaurantChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private String orderId;
    private String customerId;
    private String customerName;
    private DatabaseReference chatRef;
    private DatabaseReference restaurantRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_chat);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get order details from intent
        orderId = getIntent().getStringExtra("orderId");
        customerId = getIntent().getStringExtra("customerId");
        customerName = getIntent().getStringExtra("customerName");
        String chatType = getIntent().getStringExtra("chatType");
        String chatRefPath = getIntent().getStringExtra("chatRef");

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        customerNameText = findViewById(R.id.customerNameText);

        // Set title based on chat type
        if (chatType != null && chatType.equals("group")) {
            getSupportActionBar().setTitle("Group Chat");
            customerNameText.setText("Group Chat");
        } else {
            getSupportActionBar().setTitle("Chat with " + customerName);
            customerNameText.setText(customerName);
        }

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new RestaurantChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize Firebase references
        if (chatRefPath != null) {
            chatRef = FirebaseDatabase.getInstance().getReference(chatRefPath);
        } else {
            chatRef = FirebaseDatabase.getInstance()
                    .getReference("orders")
                    .child(orderId)
                    .child("messages");
        }
        
        String restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        restaurantRef = FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(restaurantId)
                .child("store_info");

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
                    Boolean isRead = snapshot.child("isRead").getValue(Boolean.class);

                    if (message != null && senderId != null && timestamp != null) {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setMessage(message);
                        chatMessage.setSenderId(senderId);
                        chatMessage.setSenderName(senderName);
                        chatMessage.setSenderType(senderType);
                        chatMessage.setTimestamp(timestamp);
                        chatMessage.setRead(isRead != null ? isRead : false);
                        messages.add(chatMessage);
                    }
                }
                chatAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RestaurantChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
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
            messageMap.put("senderType", "restaurant");
            messageMap.put("timestamp", timestamp);
            messageMap.put("isRead", false);

            // Get restaurant name from Firebase
            restaurantRef.child("name").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String senderName = task.getResult().getValue(String.class);
                    messageMap.put("senderName", senderName != null ? senderName : "Restaurant");
                    
                    // Send message
                    chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> {
                        messageInput.setText("");
                    }).addOnFailureListener(e -> {
                        Toast.makeText(RestaurantChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    messageMap.put("senderName", "Restaurant");
                    chatRef.push().setValue(messageMap).addOnSuccessListener(aVoid -> {
                        messageInput.setText("");
                    }).addOnFailureListener(e -> {
                        Toast.makeText(RestaurantChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ChatMessage {
        private String message;
        private String senderId;
        private String senderName;
        private String senderType;
        private long timestamp;
        private boolean isRead;

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

        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
    }
} 