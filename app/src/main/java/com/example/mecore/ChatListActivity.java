package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private String currentUsername; // Added to pass to FriendRequestsActivity
    private List<Friend> friendList;
    private FriendAdapter friendAdapter;
    private RecyclerView recyclerView;
    private ListenerRegistration friendListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Fetch current username from Firestore
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        if (currentUsername == null) {
                            Log.e(TAG, "Current user's username is null in Firestore");
                            Toast.makeText(this, "Error: Could not fetch current user's username", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Current user's document does not exist in Firestore");
                        Toast.makeText(this, "Error: Current user profile not found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch current user's username: " + e.getMessage(), e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        recyclerView = findViewById(R.id.friendsRecyclerView);
        friendList = new ArrayList<>();
        friendAdapter = new FriendAdapter(friendList, friend -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("recipientId", friend.getUserId());
            intent.putExtra("otherUsername", friend.getUsername());
            intent.putExtra("source", "ChatListActivity");
            startActivity(intent);
        }, friend -> {
            // Delete friend from Firestore
            db.collection("users")
                    .document(currentUserId)
                    .collection("friends")
                    .document(friend.getUserId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Removed " + friend.getUsername() + " from friends", Toast.LENGTH_SHORT).show();
                        // Also remove the current user from the friend's friends list
                        db.collection("users")
                                .document(friend.getUserId())
                                .collection("friends")
                                .document(currentUserId)
                                .delete();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove friend: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(friendAdapter);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button viewFriendRequestsButton = findViewById(R.id.viewFriendRequestsButton);
        viewFriendRequestsButton.setOnClickListener(v -> {
            if (currentUsername == null) {
                Toast.makeText(this, "Error: Username not loaded yet", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(ChatListActivity.this, FriendRequestsActivity.class);
            intent.putExtra("currentUsername", currentUsername);
            startActivity(intent);
        });

        BottomNavigationView bottomNavigationMenu = findViewById(R.id.bottom_navigation);
        if (bottomNavigationMenu != null) {
            NavigationUtil.setupBottomNavigationMenu(this, bottomNavigationMenu, R.id.navigation_chat);
        } else {
            Log.e(TAG, "BottomNavigationMenu is null! Check activity_chat_list.xml for ID bottom_navigation");
        }

        setupFriendListener();
    }

    private void setupFriendListener() {
        friendListener = db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: " + e.getMessage());
                        Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                            Friend friend = new Friend();
                            friend.setUserId(dc.getDocument().getString("userId"));
                            friend.setUsername(dc.getDocument().getString("username"));

                            if (friend.getUserId() == null || friend.getUsername() == null) {
                                continue;
                            }

                            switch (dc.getType()) {
                                case ADDED:
                                    friendList.add(friend);
                                    friendAdapter.notifyItemInserted(friendList.size() - 1);
                                    break;
                                case MODIFIED:
                                    for (int i = 0; i < friendList.size(); i++) {
                                        if (friendList.get(i).getUserId().equals(friend.getUserId())) {
                                            friendList.set(i, friend);
                                            friendAdapter.notifyItemChanged(i);
                                            break;
                                        }
                                    }
                                    break;
                                case REMOVED:
                                    for (int i = 0; i < friendList.size(); i++) {
                                        if (friendList.get(i).getUserId().equals(friend.getUserId())) {
                                            friendList.remove(i);
                                            friendAdapter.notifyItemRemoved(i);
                                            break;
                                        }
                                    }
                                    break;
                            }
                        }

                        if (friendList.isEmpty()) {
                            Toast.makeText(this, "No friends added yet", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendListener != null) {
            friendListener.remove();
        }
    }
}