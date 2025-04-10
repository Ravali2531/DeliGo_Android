package com.example.deligoandroid.Authentication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.deligoandroid.Admin.AdminActivity;
import com.example.deligoandroid.Customer.Activities.CustomerHomeActivity;
import com.example.deligoandroid.Driver.DocumentsUnderReviewActivity;
import com.example.deligoandroid.Driver.DriverDocumentsActivity;
import com.example.deligoandroid.Driver.DriverHomeActivity;
import com.example.deligoandroid.MainActivity;
import com.example.deligoandroid.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.example.deligoandroid.Restaurant.RestaurantDocumentsActivity;
import com.example.deligoandroid.Restaurant.RestaurantHomeActivity;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPassword, signupText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        signupText = findViewById(R.id.signupText);

        // Login Button Click
        loginButton.setOnClickListener(v -> handleLogin());

        // Forgot Password Click
        forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Signup Click
        signupText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void handleLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading progress
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Get user role and redirect accordingly
                        String userId = mAuth.getCurrentUser().getUid();
                        checkUserRoleAndRedirect(userId);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + 
                                     task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                    }
                });
    }

    private void checkUserRoleAndRedirect(String userId) {
        // First check if user is admin
        mDatabase.child("admins").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    redirectUser("Admin");
                    return;
                }
                // If not admin, check other roles
                checkInCustomers(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInCustomers(String userId) {
        mDatabase.child("customers").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isBlocked = dataSnapshot.child("blocked").getValue(Boolean.class);
                    if (isBlocked != null && isBlocked) {
                        handleBlockedUser();
                        return;
                    }
                    redirectUser("Customer");
                    return;
                }
                checkInDrivers(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInDrivers(String userId) {
        mDatabase.child("drivers").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isBlocked = dataSnapshot.child("blocked").getValue(Boolean.class);
                    if (isBlocked != null && isBlocked) {
                        handleBlockedUser();
                        return;
                    }
                    // Check if documents are submitted
                    Boolean documentsSubmitted = dataSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    String status = String.valueOf(dataSnapshot.child("documents/status").getValue());
                    if (documentsSubmitted == null || !documentsSubmitted) {
                        // Documents not submitted, redirect to upload page
                        Intent intent = new Intent(LoginActivity.this, DriverDocumentsActivity.class);
                        startActivity(intent);
                        finish();
                    } else if(status.equals("approved")) {
                        // Documents submitted, go to driver home
                        Intent intent = new Intent(LoginActivity.this, DriverHomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(LoginActivity.this, DocumentsUnderReviewActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    return;
                }
                // Check in restaurants if not a driver
                checkInRestaurants(userId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void checkInRestaurants(String userId) {
        mDatabase.child("restaurants").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean isBlocked = dataSnapshot.child("blocked").getValue(Boolean.class);
                    if (isBlocked != null && isBlocked) {
                        handleBlockedUser();
                        return;
                    }
                    // Check if documents are submitted
                    Boolean documentsSubmitted = dataSnapshot.child("documentsSubmitted").getValue(Boolean.class);
                    if (documentsSubmitted == null || !documentsSubmitted) {
                        // Documents not submitted, redirect to upload page
                        Intent intent = new Intent(LoginActivity.this, RestaurantDocumentsActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Documents submitted, go to restaurant home
                        Intent intent = new Intent(LoginActivity.this, RestaurantHomeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                    return;
                }
                // User not found in any role
                Toast.makeText(LoginActivity.this, "User role not found", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                loginButton.setEnabled(true);
                loginButton.setText("Login");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                handleDatabaseError(databaseError);
            }
        });
    }

    private void redirectUser(String role) {
        Toast.makeText(this, "Logged in successfully as " + role, Toast.LENGTH_SHORT).show();
        Intent intent;
        switch (role) {
            case "Admin":
                intent = new Intent(LoginActivity.this, AdminActivity.class);
                break;
            case "Customer":
                // Check for order status changes before redirecting
                checkOrderStatusChanges();
                intent = new Intent(LoginActivity.this, CustomerHomeActivity.class);
                break;
            case "Driver":
                intent = new Intent(LoginActivity.this, DriverHomeActivity.class);
                break;
            case "Restaurant":
                intent = new Intent(LoginActivity.this, RestaurantHomeActivity.class);
                break;
            default:
                return;
        }
        startActivity(intent);
        finish();
    }

    private void checkOrderStatusChanges() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("orders");
        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference()
                .child("notifications");

        // Query orders for this customer
        ordersRef.orderByChild("customerId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                            String status = orderSnapshot.child("status").getValue(String.class);
                            String orderStatus = orderSnapshot.child("order_status").getValue(String.class);
                            String orderId = orderSnapshot.getKey();

                            // Check various order statuses and show notifications if not already shown
                            if ("in_progress".equals(status) && "accepted".equals(orderStatus)) {
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "accepted",
                                    "Order Accepted",
                                    "Your order #" + orderId + " has been accepted by the restaurant"
                                );
                            }
                            
                            if ("in_progress".equals(status) && "driver_accepted".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ? 
                                    "Driver " + driverName + " has accepted your order #" + orderId :
                                    "A driver has accepted your order #" + orderId;
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "driver_accepted",
                                    "Driver Assigned",
                                    message
                                );
                            }
                            
                            if ("in_progress".equals(status) && "picked_up".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ? 
                                    "Driver " + driverName + " has picked up your order #" + orderId :
                                    "Your order #" + orderId + " has been picked up";
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "picked_up",
                                    "Order Picked Up",
                                    message
                                );
                            }

                            if ("delivered".equals(status) && "delivered".equals(orderStatus)) {
                                String driverName = orderSnapshot.child("driverName").getValue(String.class);
                                String message = driverName != null ?
                                    "Driver " + driverName + " has delivered your order #" + orderId :
                                    "Your order #" + orderId + " has been delivered";
                                
                                checkAndShowNotification(
                                    notificationsRef,
                                    orderId,
                                    "delivered",
                                    "Order Delivered",
                                    message
                                );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        handleDatabaseError(databaseError);
                    }
                });
    }

    private void checkAndShowNotification(DatabaseReference notificationsRef, String orderId, 
                                        String type, String title, String message) {
        // Check if this notification has already been shown
        notificationsRef.child(orderId).child(type)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists() || !Boolean.TRUE.equals(dataSnapshot.child("shown").getValue(Boolean.class))) {
                            // Show notification
                            showOrderNotification(orderId + "_" + type, title, message);

                            // Store notification data
                            Map<String, Object> notificationData = new HashMap<>();
                            notificationData.put("title", title);
                            notificationData.put("body", message);
                            notificationData.put("timestamp", ServerValue.TIMESTAMP);
                            notificationData.put("shown", true);
                            notificationData.put("read", false);

                            notificationsRef.child(orderId).child(type).setValue(notificationData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("LoginActivity", "Notification status stored successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("LoginActivity", "Failed to store notification status", e);
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("LoginActivity", "Error checking notification status", databaseError.toException());
                    }
                });
    }

    private void showOrderNotification(String orderId, String title, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "order_notifications",
                "Order Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Create intent for notification click
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("orderId", orderId);
        intent.putExtra("navigate_to", "orders");

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, "order_notifications")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(pendingIntent);

        // Show notification
        int notificationId = orderId != null ? orderId.hashCode() : 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void handleBlockedUser() {
        Toast.makeText(LoginActivity.this, "Your account has been blocked. Please contact support.", Toast.LENGTH_LONG).show();
        mAuth.signOut();
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }

    private void handleDatabaseError(DatabaseError databaseError) {
        Toast.makeText(LoginActivity.this, 
            "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }
}
