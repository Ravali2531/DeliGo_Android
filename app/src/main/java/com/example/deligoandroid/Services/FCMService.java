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

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCMService", "Received message: " + remoteMessage.getMessageId());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String message = remoteMessage.getNotification().getBody();
            String orderId = remoteMessage.getData().get("orderId");
            
            Log.d("FCMService", "Notification received - Title: " + title + ", Message: " + message + ", OrderId: " + orderId);
            sendNotification(title, message, orderId);
        } else {
            Log.d("FCMService", "Message received without notification payload");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCMService", "New FCM token received: " + token);
        
        // Store the new token in Firebase for the current user
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId != null) {
            Log.d("FCMService", "Storing new FCM token for user: " + userId);
            FirebaseDatabase.getInstance().getReference("customers")
                    .child(userId)
                    .child("fcmToken")
                    .setValue(token)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FCMService", "New FCM token stored successfully in Firebase");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FCMService", "Failed to store new FCM token in Firebase", e);
                    });
        } else {
            Log.e("FCMService", "User ID is null, cannot store new FCM token");
        }
    }

    private void sendNotification(String title, String messageBody, String orderId) {
        Log.d("FCMService", "Creating notification - Title: " + title + ", Message: " + messageBody + ", OrderId: " + orderId);
        
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("orderId", orderId);
        intent.putExtra("navigate_to", "orders");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
        Log.d("FCMService", "Notification sent successfully");
    }
} 