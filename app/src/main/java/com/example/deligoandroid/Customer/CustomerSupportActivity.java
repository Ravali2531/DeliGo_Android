package com.example.deligoandroid.Customer;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import java.util.List;

public class CustomerSupportActivity extends AppCompatActivity {
    private static final String TAG = "CustomerSupportActivity";
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private DatabaseReference dbRef;
    private DatabaseReference customerRef;
    private FirebaseAuth mAuth;
    private ChatDetailAdapter chatAdapter;
    private String customerId;
    private String customerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        customerId = mAuth.getCurrentUser().getUid();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Support Chat");
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatDetailAdapter(true);
        recyclerView.setAdapter(chatAdapter);

        // Set up Firebase references
        dbRef = FirebaseDatabase.getInstance().getReference()
                .child("chat_management")
                .child("messages")
                .child(customerId);

        customerRef = FirebaseDatabase.getInstance().getReference()
                .child("customers")
                .child(customerId);

        // Get customer name and load messages
        loadCustomerName();

        // Set up send button
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadCustomerName() {
        customerRef.child("fullName").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                customerName = task.getResult().getValue(String.class);
                if (customerName == null || customerName.isEmpty()) {
                    customerName = "Customer";
                }
                loadMessages();
            } else {
                customerName = "Customer";
                loadMessages();
            }
        });
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
                Toast.makeText(CustomerSupportActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Create new message
        Chat message = new Chat();
        message.setMessage(messageText);
        message.setSenderId(customerId);
        message.setSenderName(customerName);
        message.setSenderType("customer");
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);

        // Push message to Firebase
        dbRef.push().setValue(message)
                .addOnSuccessListener(aVoid -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message: " + e.getMessage());
                    Toast.makeText(CustomerSupportActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 