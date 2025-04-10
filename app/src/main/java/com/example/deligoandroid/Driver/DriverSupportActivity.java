package com.example.deligoandroid.Driver;

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

public class DriverSupportActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatDetailAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String driverId;
    private String driverName;
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
        chatAdapter = new ChatDetailAdapter(true); // true for driver view (driver messages on right)
        recyclerView.setAdapter(chatAdapter);

        // Get driver ID and set up Firebase references
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference()
                .child("chat_management")
                .child("messages")
                .child(driverId);

        // Load driver name
        loadDriverName();

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

    private void loadDriverName() {
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference()
                .child("drivers")
                .child(driverId);

        driverRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                driverName = snapshot.getValue(String.class);
                if (driverName == null) {
                    driverName = "Driver";
                }
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(driverName);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DriverSupportActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(DriverSupportActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
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
        message.setSenderId(driverId);
        message.setSenderName(driverName);
        message.setSenderType("driver");
        message.setRead(false);
        message.setTimestamp(System.currentTimeMillis());

        // Save message under driverId/messageId
        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(DriverSupportActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show());
    }
} 