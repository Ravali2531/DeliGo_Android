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

public class RestaurantSupportFragment extends Fragment implements ChatAdapter.OnChatClickListener {
    private static final String TAG = "RestaurantSupportFragment";
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DatabaseReference dbRef;
    private DatabaseReference restaurantsRef;
    private ChatAdapter chatAdapter;
    private Map<String, Chat> lastMessages = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_restaurant_support, container, false);
            
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
            restaurantsRef = FirebaseDatabase.getInstance().getReference().child("restaurants");
            loadRestaurantChats();
            
            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: ", e);
            Toast.makeText(getContext(), "Error loading restaurant support", Toast.LENGTH_SHORT).show();
            return inflater.inflate(R.layout.fragment_restaurant_support, container, false);
        }
    }

    private void loadRestaurantChats() {
        try {
            dbRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    lastMessages.clear();
                    
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        final String restaurantId = userSnapshot.getKey();
                        
                        // First check if this ID exists in restaurants node
                        restaurantsRef.child(restaurantId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot restaurantSnapshot) {
                                if (restaurantSnapshot.exists()) {
                                    String restaurantName = restaurantSnapshot.child("store_info").child("name").getValue(String.class);
                                    if (restaurantName != null) {
                                        // Now get the last message from their conversation
                                        loadLastMessage(restaurantId, restaurantName);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Error loading restaurant data: " + error.getMessage());
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
            Log.e(TAG, "Error in loadRestaurantChats: ", e);
            Toast.makeText(getContext(), "Error loading chats", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLastMessage(final String restaurantId, final String restaurantName) {
        dbRef.child(restaurantId).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    lastMessage.setSenderName(restaurantName); // Set the restaurant's name
                    lastMessage.setUserId(restaurantId); // Set the restaurant ID for the conversation
                    lastMessages.put(restaurantId, lastMessage);
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
        intent.putExtra("userId", chat.getUserId());
        intent.putExtra("userName", chat.getSenderName());
        intent.putExtra("userType", "restaurant");
        startActivity(intent);
    }
} 