package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestsActivity extends AppCompatActivity {

    private static final String TAG = "FriendRequestsActivity";
    private FirebaseFirestore db;
    private String currentUserId;
    private RecyclerView friendRequestsRecyclerView;
    private FriendRequestAdapter adapter;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        friendRequestsRecyclerView = findViewById(R.id.friendRequestsRecyclerView);
        ImageButton goBackButton = findViewById(R.id.goBackButton);

        // Set up RecyclerView
        adapter = new FriendRequestAdapter(new ArrayList<>(), this::onAcceptClick, this::onDeclineClick);
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequestsRecyclerView.setAdapter(adapter);

        // Listen for real-time updates to friend requests
        listenForFriendRequests();

        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(FriendRequestsActivity.this, ChatListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void listenForFriendRequests() {
        firestoreListener = db.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Listen failed: " + e.getMessage(), e);
                        Toast.makeText(this, "Error loading friend requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<FriendRequest> requests = new ArrayList<>();
                    if (snapshots != null && snapshots.getDocumentChanges() != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                String senderId = dc.getDocument().getId();
                                String senderUsername = dc.getDocument().getString("senderUsername");
                                Long timestampObj = dc.getDocument().getLong("timestamp");
                                long timestamp = (timestampObj != null) ? timestampObj : 0L; // Default to 0 if null
                                if (senderUsername != null) {
                                    requests.add(new FriendRequest(senderId, senderUsername, timestamp));
                                }
                            }
                        }
                    }
                    adapter.updateRequests(requests);
                });
    }

    private void onAcceptClick(String senderId, String senderUsername) {
        String currentUsername = getIntent().getStringExtra("currentUsername");
        if (currentUsername == null) {
            Toast.makeText(this, "Error: Current username not provided", Toast.LENGTH_LONG).show();
            return;
        }

        // Accept the friend request
        Map<String, Object> friendData = new HashMap<>();
        friendData.put("userId", senderId);
        friendData.put("username", senderUsername);
        friendData.put("timestamp", System.currentTimeMillis());

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(senderId)
                .set(friendData)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> currentUserData = new HashMap<>();
                    currentUserData.put("userId", currentUserId);
                    currentUserData.put("username", currentUsername);
                    currentUserData.put("timestamp", System.currentTimeMillis());

                    db.collection("users")
                            .document(senderId)
                            .collection("friends")
                            .document(currentUserId)
                            .set(currentUserData)
                            .addOnSuccessListener(aVoid2 -> {
                                // Remove the friend request
                                db.collection("users")
                                        .document(currentUserId)
                                        .collection("friendRequests")
                                        .document(senderId)
                                        .delete()
                                        .addOnSuccessListener(aVoid3 -> {
                                            Toast.makeText(this, "Friend request accepted from " + senderUsername, Toast.LENGTH_SHORT).show();
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to accept friend request: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to accept friend request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onDeclineClick(String senderId) {
        // Decline the friend request
        db.collection("users")
                .document(currentUserId)
                .collection("friendRequests")
                .document(senderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Friend request declined", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to decline friend request: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to decline friend request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove(); // Clean up listener
        }
    }

    // Inner class for FriendRequest data
    private static class FriendRequest {
        String senderId;
        String senderUsername;
        long timestamp;

        FriendRequest(String senderId, String senderUsername, long timestamp) {
            this.senderId = senderId;
            this.senderUsername = senderUsername;
            this.timestamp = timestamp;
        }
    }

    // Inner adapter class
    private static class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

        private List<FriendRequest> requests;
        private final OnAcceptClickListener acceptClickListener;
        private final OnDeclineClickListener declineClickListener;

        interface OnAcceptClickListener {
            void onAcceptClick(String senderId, String senderUsername);
        }

        interface OnDeclineClickListener {
            void onDeclineClick(String senderId);
        }

        FriendRequestAdapter(List<FriendRequest> requests, OnAcceptClickListener acceptListener, OnDeclineClickListener declineListener) {
            this.requests = new ArrayList<>(requests); // Defensive copy
            this.acceptClickListener = acceptListener;
            this.declineClickListener = declineListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_request_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FriendRequest request = requests.get(position);
            holder.senderUsernameTextView.setText(request.senderUsername);
            holder.acceptButton.setOnClickListener(v -> acceptClickListener.onAcceptClick(request.senderId, request.senderUsername));
            holder.declineButton.setOnClickListener(v -> declineClickListener.onDeclineClick(request.senderId));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        void updateRequests(List<FriendRequest> newRequests) {
            requests.clear();
            requests.addAll(newRequests);
            notifyDataSetChanged(); // Can optimize with DiffUtil if needed
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView senderUsernameTextView;
            Button acceptButton;
            Button declineButton;

            ViewHolder(View itemView) {
                super(itemView);
                senderUsernameTextView = itemView.findViewById(R.id.senderUsernameTextView);
                acceptButton = itemView.findViewById(R.id.acceptButton);
                declineButton = itemView.findViewById(R.id.declineButton);
            }
        }
    }
}