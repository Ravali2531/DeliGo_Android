package com.example.deligoandroid.Admin;

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

public class AdminChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private TextView chatTitleText;
    private AdminChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private String orderId;
    private DatabaseReference chatRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get order details from intent
        orderId = getIntent().getStringExtra("orderId");
        String chatRefPath = getIntent().getStringExtra("chatRef");

        // Initialize views
        recyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatTitleText = findViewById(R.id.chatTitleText);

        // Set chat title
        chatTitleText.setText("Group Chat");

        // Initialize chat
        messages = new ArrayList<>();
        chatAdapter = new AdminChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Initialize Firebase chat reference
        chatRef = FirebaseDatabase.getInstance().getReference(chatRefPath);

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
                Toast.makeText(AdminChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
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
            messageMap.put("senderType", "admin");
            messageMap.put("senderName", "Admin");
            messageMap.put("timestamp", timestamp);

            chatRef.push().setValue(messageMap)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
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
} 