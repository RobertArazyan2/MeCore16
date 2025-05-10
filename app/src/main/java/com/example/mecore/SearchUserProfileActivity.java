package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchUserProfileActivity extends AppCompatActivity {

    private static final String TAG = "SearchUserProfileActivity";
    private FirebaseFirestore db;
    private String currentUserId;
    private String searchedUserId;
    private String searchedUsername;
    private String currentUsername;
    private TextView mutualGamesTextView;

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
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button sendFriendRequestButton = findViewById(R.id.sendFriendRequestButton);
        ImageButton goBackButton = findViewById(R.id.goBackButton);
        mutualGamesTextView = findViewById(R.id.mutualGamesTextView);

        usernameTextView.setText(searchedUsername);

        // Fetch current user's data (username and games)
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        if (currentUsername == null) {
                            Log.e(TAG, "Current user's username is null in Firestore");
                            Toast.makeText(this, "Error: Could not fetch current user's username", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        // Fetch current user's selected games
                        List<String> currentUserGames = parseGameList(documentSnapshot.get("selectedGames"));
                        // Fetch searched user's data (including games)
                        fetchSearchedUserGames(currentUserGames);
                    } else {
                        Log.e(TAG, "Current user's document does not exist in Firestore");
                        Toast.makeText(this, "Error: Current user profile not found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch current user's data: " + e.getMessage(), e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("recipientId", searchedUserId);
            intent.putExtra("otherUsername", searchedUsername);
            intent.putExtra("source", "SearchUserProfileActivity");
            startActivity(intent);
        });

        sendFriendRequestButton.setOnClickListener(v -> {
            if (currentUsername == null) {
                Toast.makeText(this, "Error: Current user's username not loaded yet", Toast.LENGTH_LONG).show();
                return;
            }

            // Check if the users are already friends
            db.collection("users").document(currentUserId).collection("friends").document(searchedUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(this, searchedUsername + " is already your friend", Toast.LENGTH_SHORT).show();
                        } else {
                            // Check if a friend request has already been sent
                            db.collection("users").document(searchedUserId).collection("friendRequests").document(currentUserId).get()
                                    .addOnSuccessListener(requestSnapshot -> {
                                        if (requestSnapshot.exists()) {
                                            Toast.makeText(this, "Friend request already sent to " + searchedUsername, Toast.LENGTH_SHORT).show();
                                        } else {
                                            // Send the friend request
                                            Map<String, Object> requestData = new HashMap<>();
                                            requestData.put("senderId", currentUserId);
                                            requestData.put("senderUsername", currentUsername);
                                            requestData.put("timestamp", System.currentTimeMillis());

                                            db.collection("users")
                                                    .document(searchedUserId)
                                                    .collection("friendRequests")
                                                    .document(currentUserId)
                                                    .set(requestData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(this, "Friend request sent to " + searchedUsername, Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Failed to send friend request: " + e.getMessage(), e);
                                                        Toast.makeText(this, "Failed to send friend request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to check existing friend request: " + e.getMessage(), e);
                                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check friend status: " + e.getMessage(), e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchUserProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void fetchSearchedUserGames(List<String> currentUserGames) {
        db.collection("users").document(searchedUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> searchedUserGames = parseGameList(documentSnapshot.get("selectedGames"));
                        List<String> mutualGames = findMutualGames(currentUserGames, searchedUserGames);
                        displayMutualGames(mutualGames);
                    } else {
                        Log.e(TAG, "Searched user's document does not exist in Firestore");
                        Toast.makeText(this, "Error: Searched user profile not found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch searched user's games: " + e.getMessage(), e);
                    Toast.makeText(this, "Error fetching games: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private List<String> parseGameList(Object gamesObject) {
        List<String> games = new ArrayList<>();
        if (gamesObject instanceof List<?>) {
            for (Object obj : (List<?>) gamesObject) {
                if (obj instanceof String) {
                    games.add((String) obj);
                }
            }
        }
        return games;
    }

    private List<String> findMutualGames(List<String> currentUserGames, List<String> searchedUserGames) {
        List<String> mutualGames = new ArrayList<>();
        if (currentUserGames != null && searchedUserGames != null) {
            for (String game : currentUserGames) {
                if (searchedUserGames.contains(game)) {
                    mutualGames.add(game);
                }
            }
        }
        return mutualGames;
    }

    private void displayMutualGames(List<String> mutualGames) {
        if (mutualGames.isEmpty()) {
            mutualGamesTextView.setText("Mutual Games: None");
        } else {
            mutualGamesTextView.setText("Mutual Games: " + String.join(", ", mutualGames));
        }
    }
}