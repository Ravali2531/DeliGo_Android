package com.example.deligoandroid.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.deligoandroid.Customer.Activities.CustomerHomeActivity;
import com.example.deligoandroid.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import android.util.Log;

public class FCMService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "order_notifications";
    private static final String CHANNEL_NAME = "Order Notifications";
    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Received message: " + remoteMessage.getMessageId());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();
            String orderId = remoteMessage.getData().get("orderId");
            String type = remoteMessage.getData().get("type");
            String navigateTo = remoteMessage.getData().get("navigate_to");
            
            Log.d(TAG, "Notification received - Title: " + title + ", Message: " + message + 
                      ", OrderId: " + orderId + ", Type: " + type + ", Navigate to: " + navigateTo);
            
            // Only show notification if user is logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                sendNotification(title, message, orderId, navigateTo);
            } else {
                Log.d(TAG, "User not logged in, skipping notification");
            }
        } else {
            Log.d(TAG, "Message received without notification payload");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);
        
        // Store the new token in Firebase for the current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Storing new FCM token for user: " + userId);
            FirebaseDatabase.getInstance().getReference("customers")
                    .child(userId)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "New FCM token stored successfully in Firebase");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to store new FCM token in Firebase", e);
                    });
        } else {
            Log.e(TAG, "User not logged in, cannot store FCM token");
        }
    }

    private void sendNotification(String title, String messageBody, String orderId, String navigateTo) {
        Log.d(TAG, "Creating notification - Title: " + title + ", Message: " + messageBody + 
                  ", OrderId: " + orderId + ", Navigate to: " + navigateTo);
        
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (orderId != null) {
            intent.putExtra("orderId", orderId);
        }
        if (navigateTo != null) {
            intent.putExtra("navigate_to", navigateTo);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Use orderId as notification ID to prevent duplicate notifications for the same order
        int notificationId = orderId != null ? orderId.hashCode() : 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
        Log.d(TAG, "Notification sent successfully with ID: " + notificationId);
    }
} 