package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String username;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        TextView usernameTextView = findViewById(R.id.userProfileUsername);
        Button sendFriendRequestButton = findViewById(R.id.sendFriendRequestButton);
        Button skipButton = findViewById(R.id.skipButton);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        username = intent.getStringExtra("username");

        if (userId == null || username == null) {
            Toast.makeText(this, "Error: No user data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        usernameTextView.setText(username);

        sendFriendRequestButton.setOnClickListener(v -> {
            Toast.makeText(this, "Sending friend request to " + username, Toast.LENGTH_SHORT).show();
            sendFriendRequest();
        });

        skipButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "skip");
            resultIntent.putExtra("userId", userId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void sendFriendRequest() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        executorService.execute(() -> db.collection("friend_requests").document(userId)
                .collection("requests").document(currentUserId)
                .set(Collections.singletonMap("status", "pending"))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Friend request successfully sent to " + username + "!", Toast.LENGTH_LONG).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("action", "sent");
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        runOnUiThread(() -> Toast.makeText(this, "Failed to send friend request: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}