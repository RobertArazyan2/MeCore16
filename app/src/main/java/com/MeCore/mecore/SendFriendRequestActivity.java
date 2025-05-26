package com.MeCore.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SendFriendRequestActivity extends AppCompatActivity {

    private final List<User> userList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_friend_request);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        loadUsersFromFirestore(recyclerView);
    }

    private void loadUsersFromFirestore(RecyclerView recyclerView) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w("SendFriendRequest", "User not authenticated.");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getId();
                            String username = document.getString("username");
                            if (document.get("selectedGames") instanceof List<?>) {
                                @SuppressWarnings("unchecked")
                                List<String> selectedGames = (List<String>) document.get("selectedGames");

                                if (username != null && selectedGames != null && !userId.equals(currentUser.getUid())) {
                                    userList.add(new User(userId, username, selectedGames));
                                }
                            } else {
                                Log.e("SendFriendRequest", "selectedGames is not a List or is null for document: " + document.getId());
                            }
                        }

                        setupRecyclerView(recyclerView);
                    } else {
                        Log.e("SendFriendRequest", "Error getting users: ", task.getException());
                        Toast.makeText(this, "Error loading users.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        UserAdapter userAdapter = new UserAdapter(userList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);
    }

    public void sendFriendRequest(String targetUserId) {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        db.collection("friend_requests").document(targetUserId).collection("requests").document(currentUserId)
                .set(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send request!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}