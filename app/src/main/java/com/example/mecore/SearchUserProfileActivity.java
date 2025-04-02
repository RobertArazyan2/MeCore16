package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SearchUserProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String currentUserId;
    private String searchedUserId;
    private String searchedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Get searched user data from intent
        searchedUserId = getIntent().getStringExtra("userId");
        searchedUsername = getIntent().getStringExtra("username");

        if (searchedUserId == null || searchedUsername == null) {
            Toast.makeText(this, "Error: No user data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up UI
        TextView usernameTextView = findViewById(R.id.searchedUsernameTextView);
        Button chatButton = findViewById(R.id.chatButton);
        Button addFriendButton = findViewById(R.id.addFriendButton);
        ImageButton goBackButton = findViewById(R.id.goBackButton);

        // Display the searched user's username
        usernameTextView.setText(searchedUsername);

        // Chat button: Navigate to SearchChatActivity with correct recipientId key
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("recipientId", searchedUserId);
            intent.putExtra("otherUsername", searchedUsername);
            startActivity(intent);
        });

        // Add to Friends button: Send a friend request and save to Firestore
        addFriendButton.setOnClickListener(v -> {
            Map<String, Object> friendRequest = new HashMap<>();
            friendRequest.put("senderId", currentUserId);
            friendRequest.put("status", "pending");
            friendRequest.put("timestamp", System.currentTimeMillis());

            db.collection("friend_requests")
                    .document(searchedUserId)
                    .collection("requests")
                    .document(currentUserId)
                    .set(friendRequest)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Friend request sent to " + searchedUsername, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to send friend request: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        // Go Back button: Navigate back to MainActivity
        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}