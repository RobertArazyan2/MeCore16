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

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        searchedUserId = getIntent().getStringExtra("userId");
        searchedUsername = getIntent().getStringExtra("username");

        if (searchedUserId == null || searchedUsername == null) {
            Toast.makeText(this, "Error: No user data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView usernameTextView = findViewById(R.id.searchedUsernameTextView);
        Button chatButton = findViewById(R.id.chatButton);
        Button addFriendButton = findViewById(R.id.addFriendButton);
        ImageButton goBackButton = findViewById(R.id.goBackButton);

        usernameTextView.setText(searchedUsername);

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("recipientId", searchedUserId);
            intent.putExtra("otherUsername", searchedUsername);
            intent.putExtra("source", "SearchUserProfileActivity"); // Indicate the source
            startActivity(intent);
        });

        addFriendButton.setOnClickListener(v -> {
            Map<String, Object> friendData = new HashMap<>();
            friendData.put("userId", searchedUserId);
            friendData.put("username", searchedUsername);
            friendData.put("timestamp", System.currentTimeMillis());

            db.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .document(searchedUserId)
                    .set(friendData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Added " + searchedUsername + " to friends", Toast.LENGTH_SHORT).show();
                        Map<String, Object> currentUserData = new HashMap<>();
                        currentUserData.put("userId", currentUserId);
                        currentUserData.put("username", getIntent().getStringExtra("currentUsername"));
                        currentUserData.put("timestamp", System.currentTimeMillis());

                        db.collection("users")
                                .document(searchedUserId)
                                .collection("friends")
                                .document(currentUserId)
                                .set(currentUserData);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add friend: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}