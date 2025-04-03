package com.example.deligoandroid.Admin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.deligoandroid.R;
import com.example.deligoandroid.databinding.ActivityOtherActivitiesBinding;

public class OtherActivitiesActivity extends AppCompatActivity {
    private ActivityOtherActivitiesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtherActivitiesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.other_activities);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 