package com.example.maptest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private ZoomImageView mapImageView;
    private Button buttonZoomIn, buttonZoomOut, buttonSnapToPlayer;

    private float scale = 1f;
    private final float minScale = 1f;
    private final float maxScale = 5f;

    // Example player position in game coordinates
    private float playerX = 500; // Change this to the actual player's X position
    private float playerY = 300; // Change this to the actual player's Y position

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views by ID
        mapImageView = findViewById(R.id.mapImageView);
        buttonZoomIn = findViewById(R.id.buttonZoomIn);
        buttonZoomOut = findViewById(R.id.buttonZoomOut);
        buttonSnapToPlayer = findViewById(R.id.buttonSnapToPlayer);

        // Set up button click listeners

        // Zoom In Button
        buttonZoomIn.setOnClickListener(v -> {
            scale *= 1.2f; // Increase zoom level
            if (scale > maxScale) scale = maxScale;
            mapImageView.zoomTo(scale);
        });

        // Zoom Out Button
        buttonZoomOut.setOnClickListener(v -> {
            scale *= 0.8f; // Decrease zoom level
            if (scale < minScale) scale = minScale;
            mapImageView.zoomTo(scale);
        });

        // Snap to Player Button
        buttonSnapToPlayer.setOnClickListener(v -> {
            mapImageView.snapToPlayerLocation(playerX, playerY);
        });
    }
}

