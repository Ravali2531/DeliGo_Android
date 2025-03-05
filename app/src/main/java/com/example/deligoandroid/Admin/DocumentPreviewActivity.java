package com.example.deligoandroid.Admin;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.deligoandroid.R;

public class DocumentPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_preview);

        // Get image URL from intent
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (imageUrl == null) {
            finish();
            return;
        }

        // Initialize views
        ImageView documentImage = findViewById(R.id.documentImage);
        findViewById(R.id.doneButton).setOnClickListener(v -> finish());

        // Load image using Glide
        Glide.with(this)
            .load(imageUrl)
            .into(documentImage);
    }
} 