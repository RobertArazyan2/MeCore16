package com.example.mecore;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private TextView resultTextView;
    private Button friendRequestButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private final List<User> allUsers = new ArrayList<>();
    private User selectedRandomUser = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);
        resultTextView = findViewById(R.id.resultTextView);
        friendRequestButton = findViewById(R.id.friendRequestButton);
        Button searchButton = findViewById(R.id.searchButton);
        Button randomUserButton = findViewById(R.id.randomUserButton);

        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        mAuth = FirebaseAuth.getInstance();

        loadAllUsers();

        searchButton.setOnClickListener(v -> searchUser());
        randomUserButton.setOnClickListener(v -> findRandomUser());
        friendRequestButton.setOnClickListener(v -> sendFriendRequest());

        friendRequestButton.setVisibility(View.GONE); // Hide by default
    }

    private void searchUser() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a username!", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.orderByChild("username").equalTo(query).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String username = snapshot.child("username").getValue(String.class);
                        resultTextView.setText("Found: " + username);
                        return;
                    }
                } else {
                    resultTextView.setText("User not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error fetching data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void findRandomUser() {
        if (allUsers.isEmpty()) {
            resultTextView.setText("No users found.");
            friendRequestButton.setVisibility(View.GONE);
            return;
        }

        Random random = new Random();
        selectedRandomUser = allUsers.get(random.nextInt(allUsers.size()));
        resultTextView.setText("Random User: " + selectedRandomUser.username);
        friendRequestButton.setVisibility(View.VISIBLE);
    }

    private void sendFriendRequest() {
        if (selectedRandomUser == null) {
            Toast.makeText(this, "No user selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to send a friend request!", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser().getUid();
        String targetUserId = selectedRandomUser.userId;

        DatabaseReference requestRef = FirebaseDatabase.getInstance()
                .getReference("friend_requests")
                .child(targetUserId)
                .child(currentUserId);

        // Check if request already exists
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "Friend request already sent!", Toast.LENGTH_SHORT).show();
                } else {
                    // Send request
                    requestRef.setValue(true).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Friend request sent!", Toast.LENGTH_SHORT).show();
                            friendRequestButton.setVisibility(View.GONE);
                            loadAllUsers(); // Refresh user list after sending
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to send request!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error checking friend request!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllUsers() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allUsers.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String userId = snapshot.getKey();
                    String username = snapshot.child("username").getValue(String.class);
                    if (userId != null && username != null) {
                        allUsers.add(new User(userId, username));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error loading users!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class User {
        String userId;
        String username;

        public User(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}
