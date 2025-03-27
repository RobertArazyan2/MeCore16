package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
    private final List<User> friendsList = new ArrayList<>();
    private ChatListAdapter adapter;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        RecyclerView chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        FloatingActionButton friendRequestsButton = findViewById(R.id.friendRequestsButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set up RecyclerView
        chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(friendsList);
        chatListRecyclerView.setAdapter(adapter);

        // Load friends
        loadFriends();

        // Navigate to FriendRequestsActivity
        friendRequestsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, FriendRequestsActivity.class);
            startActivity(intent);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFriends() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "loadFriends: Loading friends for userId: " + currentUserId);

        executorService.execute(() -> db.collection("users").document(currentUserId)
                .collection("friends")
                .whereEqualTo("status", "accepted")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendsList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String friendId = doc.getString("friendId");
                            if (friendId == null) continue;

                            // Fetch the friend's username from the users collection
                            db.collection("users").document(friendId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String username = userDoc.getString("username");
                                            if (username != null && !username.trim().isEmpty()) {
                                                friendsList.add(new User(friendId, username, new ArrayList<>()));
                                                runOnUiThread(() -> adapter.notifyDataSetChanged());
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "loadFriends: Failed to load user " + friendId + ": " + e.getMessage()));
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
                        Log.e(TAG, "loadFriends: Failed to load friends: " + errorMessage);
                        runOnUiThread(() -> Toast.makeText(this, "Failed to load friends: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    // RecyclerView Adapter for the friends list
    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

        private final List<User> friends;

        public ChatListAdapter(List<User> friends) {
            this.friends = friends;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            User friend = friends.get(position);
            holder.usernameTextView.setText(friend.getUsername());

            // Navigate to ChatActivity when a friend is clicked
            holder.itemView.setOnClickListener(v -> {
                String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                String chatId = generateChatId(currentUserId, friend.getUserId());
                Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("friendId", friend.getUserId());
                intent.putExtra("friendUsername", friend.getUsername());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        public class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView usernameTextView;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                usernameTextView = itemView.findViewById(R.id.usernameTextView);
            }
        }
    }

    // Generate a unique chatId by combining the two user IDs in a consistent order
    private String generateChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}