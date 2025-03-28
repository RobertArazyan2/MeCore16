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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendRequestsActivity extends AppCompatActivity implements FriendRequestAdapter.OnFriendRequestActionListener {

    private static final String TAG = "FriendRequestsActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FriendRequestAdapter adapter;
    private final List<FriendRequest> activity_friend_requests = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Starting FriendRequestsActivity");
        try {
            setContentView(R.layout.activity_friend_requests);
            Log.d(TAG, "onCreate: Layout set successfully");

            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) RecyclerView friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView);
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "onCreate: Firebase initialized");

            // Set up BottomNavigationView
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            bottomNavigationView.setSelectedItemId(R.id.navigation_friends);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                String itemName;
                try {
                    itemName = getResources().getResourceEntryName(itemId);
                } catch (Exception e) {
                    itemName = "Unknown";
                }
                Log.d("BottomNav", "Clicked ID: " + itemId + ", Resource: " + itemName);

                if (itemId == R.id.navigation_friends) {
                    Log.d("BottomNav", "Already on FriendRequestsActivity (navigation_friends)");
                    return true;
                } else if (itemId == R.id.navigation_main) {
                    Log.d("BottomNav", "Opening MainActivity");
                    try {
                        Intent intent = new Intent(FriendRequestsActivity.this, MainActivity.class);
                        startActivity(intent);
                        Log.d("BottomNav", "MainActivity started successfully");
                    } catch (Exception e) {
                        Log.e("BottomNav", "Failed to start MainActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to open Main: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else if (itemId == R.id.navigation_chat) {
                    Log.d("BottomNav", "Opening ChatListActivity");
                    try {
                        Intent intent = new Intent(FriendRequestsActivity.this, ChatListActivity.class);
                        startActivity(intent);
                        Log.d("BottomNav", "ChatListActivity started successfully");
                    } catch (Exception e) {
                        Log.e("BottomNav", "Failed to start ChatListActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to open Chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    Log.d("BottomNav", "Opening ProfileActivity");
                    try {
                        Intent intent = new Intent(FriendRequestsActivity.this, ProfileActivity.class);
                        startActivity(intent);
                        Log.d("BottomNav", "ProfileActivity started successfully");
                    } catch (Exception e) {
                        Log.e("BottomNav", "Failed to start ProfileActivity: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to open Profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                Log.w("BottomNav", "Unknown item ID: " + itemId);
                return false;
            });

            if (mAuth.getCurrentUser() == null) {
                Log.w(TAG, "onCreate: User not logged in");
                Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            Log.d(TAG, "onCreate: User is logged in: " + mAuth.getCurrentUser().getUid());

            // Set up RecyclerView
            friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new FriendRequestAdapter(activity_friend_requests, this);
            friendRequestsRecyclerView.setAdapter(adapter);
            Log.d(TAG, "onCreate: RecyclerView set up");

            // Load friend requests
            loadFriendRequests();
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Failed to initialize FriendRequestsActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Error in FriendRequestsActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFriendRequests() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "loadFriendRequests: Loading friend requests for userId: " + currentUserId);

        executorService.execute(() -> db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        activity_friend_requests.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String requestId = doc.getId();
                            String senderId = doc.getString("friendId");
                            if (senderId == null) continue;

                            Log.d(TAG, "Found pending friend request: " + requestId + " from " + senderId);

                            db.collection("users").document(senderId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String username = userDoc.getString("username");
                                            if (username != null && !username.trim().isEmpty()) {
                                                activity_friend_requests.add(new FriendRequest(requestId, senderId, username));
                                                Collections.reverse(activity_friend_requests);
                                                runOnUiThread(() -> adapter.notifyDataSetChanged());
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to load user " + senderId + ": " + e.getMessage()));
                        }
                        runOnUiThread(() -> {
                            if (activity_friend_requests.isEmpty()) {
                                Toast.makeText(this, "No friend requests found", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Loaded " + activity_friend_requests.size() + " friend requests", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "loadFriendRequests: Failed to load friend requests: " + errorMessage);
                        runOnUiThread(() -> Toast.makeText(this, "Failed to load friend requests: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    @Override
    public void onAccept(FriendRequest friendRequest, int position) {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String senderId = friendRequest.getSenderId();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "accepted");

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friendRequest.getRequestId())
                .update(updateData)
                .addOnSuccessListener(aVoid -> db.collection("users")
                        .document(senderId)
                        .collection("friends")
                        .whereEqualTo("friendId", currentUserId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                doc.getReference().update("status", "accepted");
                            }
                            activity_friend_requests.remove(position);
                            runOnUiThread(() -> adapter.notifyItemRemoved(position));
                            Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "onAccept: Failed to update sender's status: " + e.getMessage());
                            Toast.makeText(this, "Failed to update sender's status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onAccept: Failed to accept friend request: " + e.getMessage());
                    Toast.makeText(this, "Failed to accept friend request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onDecline(FriendRequest friendRequest, int position) {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        String senderId = friendRequest.getSenderId();

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "declined");

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(friendRequest.getRequestId())
                .update(updateData)
                .addOnSuccessListener(aVoid -> db.collection("users")
                        .document(senderId)
                        .collection("friends")
                        .whereEqualTo("friendId", currentUserId)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (QueryDocumentSnapshot doc : querySnapshot) {
                                doc.getReference().update("status", "declined");
                            }
                            activity_friend_requests.remove(position);
                            runOnUiThread(() -> adapter.notifyItemRemoved(position));
                            Toast.makeText(this, "Friend request declined", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "onDecline: Failed to update sender's status: " + e.getMessage());
                            Toast.makeText(this, "Failed to update sender's status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onDecline: Failed to decline friend request: " + e.getMessage());
                    Toast.makeText(this, "Failed to decline friend request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
