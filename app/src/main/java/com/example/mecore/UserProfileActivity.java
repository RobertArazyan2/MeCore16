package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String username;
    private String currentUserId;
    private final List<User> allUsers = new ArrayList<>();
    private List<String> selectedGames = new ArrayList<>();
    private boolean isGamesLoaded = false;
    private Set<String> skippedUserIds = new HashSet<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        TextView usernameTextView = findViewById(R.id.userProfileUsername);
        Button chatNowButton = findViewById(R.id.chatNowButton);
        Button skipButton = findViewById(R.id.skipButton);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        // Get userId and username from intent
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        username = intent.getStringExtra("username");
        skippedUserIds = new HashSet<>(intent.getStringArrayListExtra("skippedUserIds") != null ?
                Objects.requireNonNull(intent.getStringArrayListExtra("skippedUserIds")) : new ArrayList<>());

        if (userId == null || username == null) {
            Toast.makeText(this, "Error: No user data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        usernameTextView.setText(username);

        // Load users and games for matching
        loadSelectedGames();
        loadAllUsers();

        chatNowButton.setOnClickListener(v -> {
            Toast.makeText(this, "Starting chat with " + username, Toast.LENGTH_SHORT).show();
            startChat();
        });

        skipButton.setOnClickListener(v -> {
            skippedUserIds.add(userId);
            findNextUser();
        });
    }

    private void loadSelectedGames() {
        executorService.execute(() -> db.collection("users").document(currentUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Object gamesObject = task.getResult().get("selectedGames");
                        selectedGames = parseGameList(gamesObject);
                        isGamesLoaded = true;
                    }
                }));
    }

    private void loadAllUsers() {
        executorService.execute(() -> db.collection("users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allUsers.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String userId = doc.getId();
                            String username = doc.getString("username");
                            List<String> games = parseGameList(doc.get("selectedGames"));
                            if (username == null || username.trim().isEmpty() || userId.equals(currentUserId)) {
                                continue;
                            }
                            allUsers.add(new User(userId, username, games));
                        }
                    }
                }));
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

    private void findNextUser() {
        if (!isGamesLoaded || allUsers.isEmpty()) {
            Toast.makeText(this, "Data not loaded, restarting user list", Toast.LENGTH_SHORT).show();
            skippedUserIds.clear();
            loadAllUsers();
            findNextUser();
            return;
        }

        List<User> availableUsers = new ArrayList<>();
        for (User user : allUsers) {
            if (!skippedUserIds.contains(user.getUserId())) {
                availableUsers.add(user);
            }
        }

        if (availableUsers.isEmpty()) {
            Toast.makeText(this, "No more users available, restarting user list", Toast.LENGTH_SHORT).show();
            skippedUserIds.clear();
            availableUsers.addAll(allUsers);
        }

        List<User> matches = new ArrayList<>();
        for (User user : availableUsers) {
            if (hasMatchingGame(user.getSelectedGames())) {
                matches.add(user);
            }
        }

        User selectedUser;
        if (matches.isEmpty()) {
            Collections.shuffle(availableUsers);
            selectedUser = availableUsers.get(0);
        } else {
            Collections.shuffle(matches);
            selectedUser = matches.get(0);
        }

        Intent intent = new Intent(UserProfileActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", selectedUser.getUserId());
        intent.putExtra("username", selectedUser.getUsername());
        intent.putStringArrayListExtra("skippedUserIds", new ArrayList<>(skippedUserIds));
        startActivity(intent);
        finish();
    }

    private boolean hasMatchingGame(List<String> userGames) {
        if (userGames == null || selectedGames == null) return false;
        for (String game : userGames) {
            if (selectedGames.contains(game)) {
                return true;
            }
        }
        return false;
    }

    private void startChat() {
        Intent chatIntent = new Intent(UserProfileActivity.this, ChatActivity.class);
        chatIntent.putExtra("recipientId", userId); // Pass the recipientId (userId of the other user)
        chatIntent.putExtra("currentUserId", currentUserId); // Optional: Pass current user ID if needed
        chatIntent.putExtra("otherUsername", username); // Optional: Pass username if needed in ChatActivity
        startActivity(chatIntent);

        // Return result to MainActivity indicating chat was started
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "sent");
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}