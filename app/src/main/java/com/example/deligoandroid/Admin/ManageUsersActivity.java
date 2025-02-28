package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.deligoandroid.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {
    private Button btnCustomers, btnDrivers, btnRestaurants;
    private RecyclerView userListRecyclerView;
    private TextView userTypeTitle;
    private LinearLayout userDetailsContainer;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        btnCustomers = findViewById(R.id.btnCustomers);
        btnDrivers = findViewById(R.id.btnDrivers);
        btnRestaurants = findViewById(R.id.btnRestaurants);
        userTypeTitle = findViewById(R.id.userTypeTitle);
        userListRecyclerView = findViewById(R.id.userListRecyclerView);
        
        // Set up RecyclerView
        userListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set click listeners
        btnCustomers.setOnClickListener(v -> loadUsers("customers"));
        btnDrivers.setOnClickListener(v -> loadUsers("drivers"));
        btnRestaurants.setOnClickListener(v -> loadUsers("restaurants"));
    }

    private void loadUsers(String userType) {
        userTypeTitle.setText(userType.substring(0, 1).toUpperCase() + userType.substring(1));
        
        mDatabase.child(userType).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<UserData> userList = new ArrayList<>();
                
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String email = userSnapshot.child("email").getValue(String.class);
                    String fullName = userSnapshot.child("fullName").getValue(String.class);
                    String phone = userSnapshot.child("phone").getValue(String.class);
                    
                    UserData userData = new UserData(userId, email, fullName, phone);
                    userList.add(userData);
                }
                
                UserListAdapter adapter = new UserListAdapter(userList);
                userListRecyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ManageUsersActivity.this, 
                    "Error loading users: " + databaseError.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Data class for user information
    private static class UserData {
        String userId;
        String email;
        String fullName;
        String phone;

        UserData(String userId, String email, String fullName, String phone) {
            this.userId = userId;
            this.email = email;
            this.fullName = fullName;
            this.phone = phone;
        }
    }

    // RecyclerView Adapter
    private class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {
        private List<UserData> userList;

        UserListAdapter(List<UserData> userList) {
            this.userList = userList;
        }

        @Override
        public UserViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            UserData userData = userList.get(position);
            holder.nameText.setText("Name: " + userData.fullName);
            holder.emailText.setText("Email: " + userData.email);
            holder.phoneText.setText("Phone: " + userData.phone);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, emailText, phoneText;

            UserViewHolder(android.view.View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.userName);
                emailText = itemView.findViewById(R.id.userEmail);
                phoneText = itemView.findViewById(R.id.userPhone);
            }
        }
    }
} 