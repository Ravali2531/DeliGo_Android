package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.example.deligoandroid.Admin.ChatDetailAdapter;
import com.example.deligoandroid.Admin.models.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantSupportActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatDetailAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String restaurantId;
    private String restaurantName;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // Set up toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Support Chat");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatDetailAdapter(true); // true for restaurant view (restaurant messages on right)
        recyclerView.setAdapter(chatAdapter);

        // Get restaurant ID and set up Firebase references
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("chat_management")
                .child("messages")
                .child(restaurantId);

        // Load restaurant name
        loadRestaurantName();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load messages
        loadMessages();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadRestaurantName() {
        DatabaseReference restaurantRef = FirebaseDatabase.getInstance().getReference()
                .child("restaurants")
                .child(restaurantId);

        restaurantRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                restaurantName = snapshot.getValue(String.class);
                if (restaurantName == null) {
                    restaurantName = "Restaurant";
                }
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(restaurantName);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RestaurantSupportActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        messagesRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Chat> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Chat message = messageSnapshot.getValue(Chat.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                Collections.sort(messages, (m1, m2) -> 
                    Long.compare(m1.getTimestamp(), m2.getTimestamp()));
                chatAdapter.setMessages(messages);
                recyclerView.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RestaurantSupportActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) return;

        // Generate a unique message ID
        String messageId = messagesRef.push().getKey();
        if (messageId == null) return;

        // Create message data
        Chat message = new Chat();
        message.setMessageId(messageId);
        message.setMessage(messageText);
        message.setSenderId(restaurantId);
        message.setSenderName(restaurantName);
        message.setSenderType("restaurant");
        message.setRead(false);
        message.setTimestamp(System.currentTimeMillis());

        // Save message under restaurantId/messageId
        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(RestaurantSupportActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }
} 