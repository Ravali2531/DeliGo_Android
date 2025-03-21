package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.example.deligoandroid.Admin.models.Chat;

public class ChatDetailActivity extends AppCompatActivity {
    private static final String TAG = "ChatDetailActivity";
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private DatabaseReference dbRef;
    private String userId;
    private String userName;
    private String userType;
    private Toolbar toolbar;
    private ChatDetailAdapter chatAdapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        mAuth = FirebaseAuth.getInstance();

        // Get data from intent
        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        userType = getIntent().getStringExtra("userType");

        if (userId == null || userName == null || userType == null || mAuth.getCurrentUser() == null) {
            Log.e(TAG, "Missing required data");
            finish();
            return;
        }

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(userName);
            getSupportActionBar().setSubtitle("Type: " + userType);
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatDetailAdapter();
        recyclerView.setAdapter(chatAdapter);

        // Set up Firebase
        dbRef = FirebaseDatabase.getInstance().getReference()
                .child("chat_management")
                .child("messages")
                .child(userId);

        // Load messages
        loadMessages();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Chat> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Chat message = messageSnapshot.getValue(Chat.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        messages.add(message);
                    }
                }
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading messages: " + error.getMessage());
                Toast.makeText(ChatDetailActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        String adminId = mAuth.getCurrentUser().getUid();

        // Get admin's name from Firebase
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference()
                .child("admins")
                .child(adminId)
                .child("fullName");

        adminRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String adminName = task.getResult().getValue(String.class);
                if (adminName == null || adminName.isEmpty()) {
                    adminName = "Admin"; // Fallback name if not found
                }

                // Create new message
                Chat message = new Chat();
                message.setMessage(messageText);
                message.setSenderId(adminId);
                message.setSenderName(adminName);
                message.setSenderType("admin");
                message.setTimestamp(System.currentTimeMillis());
                message.setRead(false);

                // Push message to Firebase
                dbRef.push().setValue(message)
                        .addOnSuccessListener(aVoid -> {
                            messageInput.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error sending message: " + e.getMessage());
                            Toast.makeText(ChatDetailActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Log.e(TAG, "Error getting admin name: " + (task.getException() != null ? task.getException().getMessage() : "unknown error"));
                // Send message with default admin name if we can't get the actual name
                Chat message = new Chat();
                message.setMessage(messageText);
                message.setSenderId(adminId);
                message.setSenderName("Admin");
                message.setSenderType("admin");
                message.setTimestamp(System.currentTimeMillis());
                message.setRead(false);

                dbRef.push().setValue(message)
                        .addOnSuccessListener(aVoid -> {
                            messageInput.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error sending message: " + e.getMessage());
                            Toast.makeText(ChatDetailActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 