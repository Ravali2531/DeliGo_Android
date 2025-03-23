package com.example.deligoandroid.Restaurant;

import android.os.Bundle;
import android.view.MenuItem;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoreHoursActivity extends AppCompatActivity {
    private DatabaseReference storeHoursRef;
    private RecyclerView recyclerView;
    private StoreHoursAdapter adapter;
    private final List<DaySchedule> schedules = new ArrayList<>();
    private String restaurantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_hours);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Store Hours");
        }

        // Initialize Firebase
        restaurantId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        storeHoursRef = FirebaseDatabase.getInstance().getReference()
                .child("restaurants")
                .child(restaurantId)
                .child("store_hours");

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize days
        initializeDays();
        
        // Set up adapter
        adapter = new StoreHoursAdapter(schedules, this::saveScheduleForDay);
        recyclerView.setAdapter(adapter);

        // Load store hours
        loadStoreHours();

        // Set up save button
        findViewById(R.id.saveButton).setOnClickListener(v -> saveAllHours());
    }

    private void initializeDays() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            schedules.add(new DaySchedule(day, "09:00", "22:00", true));
        }
    }

    private void loadStoreHours() {
        storeHoursRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DaySchedule schedule : schedules) {
                    DataSnapshot daySnapshot = snapshot.child(schedule.day.toLowerCase());
                    if (daySnapshot.exists()) {
                        schedule.openTime = daySnapshot.child("openTime").getValue(String.class);
                        schedule.closeTime = daySnapshot.child("closeTime").getValue(String.class);
                        schedule.isOpen = daySnapshot.child("isOpen").getValue(Boolean.class);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StoreHoursActivity.this, 
                    "Failed to load store hours", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveScheduleForDay(DaySchedule schedule) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("openTime", schedule.openTime);
        updates.put("closeTime", schedule.closeTime);
        updates.put("isOpen", schedule.isOpen);

        storeHoursRef.child(schedule.day.toLowerCase())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Hours updated for " + schedule.day, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to update " + schedule.day, Toast.LENGTH_SHORT).show());
    }

    private void saveAllHours() {
        Map<String, Object> updates = new HashMap<>();
        for (DaySchedule schedule : schedules) {
            Map<String, Object> dayUpdates = new HashMap<>();
            dayUpdates.put("openTime", schedule.openTime);
            dayUpdates.put("closeTime", schedule.closeTime);
            dayUpdates.put("isOpen", schedule.isOpen);
            updates.put(schedule.day.toLowerCase(), dayUpdates);
        }

        storeHoursRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Store hours updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Failed to update store hours", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class DaySchedule {
        String day;
        String openTime;
        String closeTime;
        boolean isOpen;

        DaySchedule(String day, String openTime, String closeTime, boolean isOpen) {
            this.day = day;
            this.openTime = openTime;
            this.closeTime = closeTime;
            this.isOpen = isOpen;
        }
    }
} 