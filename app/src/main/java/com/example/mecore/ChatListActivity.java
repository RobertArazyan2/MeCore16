package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ChatListAdapter adapter;
    private final List<User> friendsList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting ChatListActivity");
        setContentView(R.layout.activity_chat_list);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RecyclerView chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "onCreate: User not logged in");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "onCreate: User is logged in: " + mAuth.getCurrentUser().getUid());

        // Set up RecyclerView
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(friendsList, user -> Toast.makeText(this, "Clicked on " + user.getUsername(), Toast.LENGTH_SHORT).show());
        chatListRecyclerView.setAdapter(adapter);
        Log.d(TAG, "onCreate: RecyclerView and ChatListAdapter set up");

        // Set up Bottom Navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_chat_list); // Highlight "Chats" as selected
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat_list) {
                // Already in ChatListActivity, do nothing
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(ChatListActivity.this, ProfileActivity.class));
                finish(); // Close ChatListActivity to prevent stacking
                return true;
            }
            return false;
        });

        // Load friends list
        loadFriendsList();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFriendsList() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "loadFriendsList: Loading friends for userId: " + currentUserId);
        Log.d(TAG, "loadFriendsList: Authentication state - UID: " + mAuth.getCurrentUser().getUid() + ", Email: " + mAuth.getCurrentUser().getEmail());

        executorService.execute(() -> {
            try {
                db.collection("users")
                        .document(currentUserId)
                        .collection("friends")
                        .whereEqualTo("status", "accepted")
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                friendsList.clear();
                                Log.d(TAG, "loadFriendsList: Successfully fetched friends collection");
                                for (QueryDocumentSnapshot doc : task.getResult()) {
                                    String friendId = doc.getString("friendId");
                                    if (friendId == null) {
                                        Log.w(TAG, "loadFriendsList: friendId is null for document: " + doc.getId());
                                        continue;
                                    }
                                    Log.d(TAG, "loadFriendsList: Found friendId: " + friendId);

                                    db.collection("users").document(friendId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc.exists()) {
                                                    String username = userDoc.getString("username");
                                                    List<String> selectedGames = (List<String>) userDoc.get("selectedGames");
                                                    if (username != null && selectedGames != null) {
                                                        friendsList.add(new User(friendId, username, selectedGames));
                                                        Log.d(TAG, "loadFriendsList: Added friend: " + username);
                                                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                                                    } else {
                                                        Log.w(TAG, "loadFriendsList: Missing username or selectedGames for friendId: " + friendId);
                                                    }
                                                } else {
                                                    Log.w(TAG, "loadFriendsList: Friend document does not exist for friendId: " + friendId);
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e(TAG, "loadFriendsList: Failed to load friend " + friendId + ": " + e.getMessage()));
                                }
                                runOnUiThread(() -> {
                                    if (friendsList.isEmpty()) {
                                        Toast.makeText(this, "No friends found", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(this, "Loaded " + friendsList.size() + " friends", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                Log.e(TAG, "loadFriendsList: Failed to load friends: " + errorMessage);
                                runOnUiThread(() -> Toast.makeText(this, "Failed to load friends: " + errorMessage, Toast.LENGTH_LONG).show());
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "loadFriendsList: Unexpected error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Unexpected error loading friends: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}