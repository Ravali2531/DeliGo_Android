package com.example.deligoandroid.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.deligoandroid.Admin.models.Chat;

public class DriverSupportFragment extends Fragment implements ChatAdapter.OnChatClickListener {
    private static final String TAG = "DriverSupportFragment";
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DatabaseReference dbRef;
    private DatabaseReference driversRef;
    private ChatAdapter chatAdapter;
    private Map<String, Chat> lastMessages = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_driver_support, container, false);
            
            recyclerView = view.findViewById(R.id.recyclerView);
            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout");
                return view;
            }

            emptyView = view.findViewById(R.id.emptyView);
            if (emptyView == null) {
                Log.e(TAG, "EmptyView not found in layout");
                return view;
            }
            
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatAdapter = new ChatAdapter(this);
            recyclerView.setAdapter(chatAdapter);
            
            dbRef = FirebaseDatabase.getInstance().getReference().child("chat_management").child("messages");
            driversRef = FirebaseDatabase.getInstance().getReference().child("drivers");
            loadDriverChats();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: ", e);
            Toast.makeText(getContext(), "Error loading driver support", Toast.LENGTH_SHORT).show();
            return inflater.inflate(R.layout.fragment_driver_support, container, false);
        }
    }

    private void loadDriverChats() {
        try {
            dbRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    lastMessages.clear();
                    
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        final String driverId = userSnapshot.getKey();
                        
                        // First check if this ID exists in drivers node
                        driversRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot driverSnapshot) {
                                if (driverSnapshot.exists()) {
                                    String driverName = driverSnapshot.child("fullName").getValue(String.class);
                                    if (driverName != null) {
                                        // Now get the last message from their conversation
                                        loadLastMessage(driverId, driverName);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error loading driver data: " + error.getMessage());
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading chats: " + error.getMessage());
                    Toast.makeText(getContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in loadDriverChats: ", e);
            Toast.makeText(getContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLastMessage(final String driverId, final String driverName) {
        dbRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot conversationSnapshot) {
                Chat lastMessage = null;
                
                // Find the last message in the conversation
                for (DataSnapshot messageSnapshot : conversationSnapshot.getChildren()) {
                    Chat message = messageSnapshot.getValue(Chat.class);
                    if (message != null) {
                        message.setMessageId(messageSnapshot.getKey());
                        if (lastMessage == null || message.getTimestamp() > lastMessage.getTimestamp()) {
                            lastMessage = message;
                        }
                    }
                }
                
                if (lastMessage != null) {
                    lastMessage.setSenderName(driverName); // Set the driver's name
                    lastMessage.setUserId(driverId); // Set the driver ID for the conversation
                    lastMessages.put(driverId, lastMessage);
                    updateChatList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading conversation: " + error.getMessage());
            }
        });
    }

    private void updateChatList() {
        List<Chat> chatList = new ArrayList<>(lastMessages.values());
        if (!chatList.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            chatAdapter.updateChats(chatList);
        } else {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onChatClick(Chat chat) {
        Intent intent = new Intent(getContext(), ChatDetailActivity.class);
        intent.putExtra("userId", chat.getUserId()); // Use the stored driver ID instead of senderId
        intent.putExtra("userName", chat.getSenderName());
        intent.putExtra("userType", "driver");
        startActivity(intent);
    }
} 