package com.example.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendRequestsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FriendRequestsAdapter adapter;
    private List<FriendRequest> friendRequests;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        // Initialize Firestore and RecyclerView
        db = FirebaseFirestore.getInstance();
        RecyclerView recyclerView = findViewById(R.id.friendRequestsRecyclerView);
        friendRequests = new ArrayList<>();
        adapter = new FriendRequestsAdapter(friendRequests);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load sent requests
        loadSentRequests();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadSentRequests() {
        // Get the current user's ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String currentUserID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Fetch the friend requests where the current user is the sender
        db.collection("friend_requests")
                .whereEqualTo("senderID", currentUserID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String receiverId = document.getString("receiverID");

                            // Fetch receiver's username from the 'users' collection
                            assert receiverId != null;
                            db.collection("users").document(receiverId).get()
                                    .addOnSuccessListener(userDoc -> {
                                        String username = userDoc.getString("username");

                                        // Create a FriendRequest object and add it to the list
                                        FriendRequest request = new FriendRequest(
                                                document.getId(),
                                                document.getString("senderID"),
                                                receiverId,
                                                username
                                        );

                                        friendRequests.add(request);
                                        adapter.notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(FriendRequestsActivity.this, "Error fetching username", Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(FriendRequestsActivity.this, "Error loading requests", Toast.LENGTH_SHORT).show());
    }
}
