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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
    private boolean isUsersLoaded = false;
    private Set<String> skippedUserIds = new HashSet<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView usernameTextView = findViewById(R.id.userProfileUsername);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button chatNowButton = findViewById(R.id.chatNowButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button skipButton = findViewById(R.id.skipButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ImageButton goBackButton = findViewById(R.id.goBackButton); // Removed "WrongViewCast"

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

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

        loadDataAndSetup();

        chatNowButton.setOnClickListener(v -> {
            Toast.makeText(this, "Starting chat with " + username, Toast.LENGTH_SHORT).show();
            startChat();
        });

        skipButton.setOnClickListener(v -> {
            skippedUserIds.add(userId);
            findNextUser();
        });

        goBackButton.setOnClickListener(v -> {
            Intent mainIntent = new Intent(UserProfileActivity.this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
        });
    }

    private void loadDataAndSetup() {
        CountDownLatch latch = new CountDownLatch(2);

        executorService.execute(() -> db.collection("users").document(currentUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Object gamesObject = task.getResult().get("selectedGames");
                        selectedGames = parseGameList(gamesObject);
                        isGamesLoaded = true;
                    }
                    latch.countDown();
                }));

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
                        isUsersLoaded = true;
                    }
                    latch.countDown();
                }));

        new Thread(() -> {
            try {
                latch.await();
                runOnUiThread(() -> {
                    if (!isGamesLoaded || !isUsersLoaded) {
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            } catch (InterruptedException e) {
                Log.e("UserProfileActivity", "Error waiting for data load", e);
            }
        }).start();
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
        if (!isGamesLoaded || !isUsersLoaded || allUsers.isEmpty()) {
            Toast.makeText(this, "Data not loaded, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        List<User> availableUsers = new ArrayList<>();
        for (User user : allUsers) {
            if (!skippedUserIds.contains(user.getUserId())) {
                availableUsers.add(user);
            }
        }

        List<User> matches = new ArrayList<>();
        for (User user : availableUsers) {
            if (hasMatchingGame(user.getSelectedGames())) {
                matches.add(user);
            }
        }

        // If no matches are left, reset skippedUserIds and use all matching users again
        if (matches.isEmpty()) {
            Toast.makeText(this, "No more matching users, restarting the list", Toast.LENGTH_SHORT).show();
            skippedUserIds.clear(); // Reset the skipped users list
            for (User user : allUsers) {
                if (hasMatchingGame(user.getSelectedGames())) {
                    matches.add(user);
                }
            }
        }

        // If there are still no matches after resetting (e.g., no users share games), exit
        if (matches.isEmpty()) {
            Toast.makeText(this, "No users with matching games found", Toast.LENGTH_LONG).show();
            Intent mainIntent = new Intent(UserProfileActivity.this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(mainIntent);
            finish();
            return;
        }

        Collections.shuffle(matches); // Shuffle the matches for random order
        User selectedUser = matches.get(0);

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
        chatIntent.putExtra("recipientId", userId);
        chatIntent.putExtra("currentUserId", currentUserId);
        chatIntent.putExtra("otherUsername", username);
        startActivity(chatIntent);

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

    private static class User {
        private final String userId;
        private final String username;
        private final List<String> selectedGames;

        User(String userId, String username, List<String> selectedGames) {
            this.userId = userId;
            this.username = username;
            this.selectedGames = selectedGames;
        }

        String getUserId() {
            return userId;
        }

        String getUsername() {
            return username;
        }

        List<String> getSelectedGames() {
            return selectedGames;
        }
    }
}