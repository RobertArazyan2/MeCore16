package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView resultTextView;
    private EditText searchEditText;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<User> allUsers = new ArrayList<>();
    private List<String> selectedGames = new ArrayList<>();
    private boolean isGamesLoaded = false;
    private String lastSkippedUserId = null;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private final ActivityResultLauncher<Intent> userProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String action = data.getStringExtra("action");
                    if ("skip".equals(action)) {
                        lastSkippedUserId = data.getStringExtra("userId");
                        Log.d(TAG, "userProfileLauncher: Skipped userId: " + lastSkippedUserId);
                        findMatchingUser();
                    } else if ("sent".equals(action)) {
                        lastSkippedUserId = null;
                        findMatchingUser();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Setting content view");
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: Initializing views");
        Button findUserButton = findViewById(R.id.findUserButton);
        Button searchUserButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
        resultTextView = findViewById(R.id.resultTextView);

        Log.d(TAG, "onCreate: Setting up BottomNavigationView");
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile clicked (not implemented)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_friends) {
                Toast.makeText(this, "Friends clicked (not implemented)", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        Log.d(TAG, "onCreate: Initializing Firebase");
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "onCreate: Checking if user is logged in");
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "onCreate: User not logged in");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "onCreate: Loading selected games");
        loadSelectedGames();
        Log.d(TAG, "onCreate: Loading all users");
        loadAllUsers();

        Log.d(TAG, "onCreate: Setting up findUserButton click listener");
        findUserButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Finding user...", Toast.LENGTH_SHORT).show();
            findMatchingUser();
        });

        Log.d(TAG, "onCreate: Setting up searchUserButton click listener");
        searchUserButton.setOnClickListener(v -> {
            String searchUsername = searchEditText.getText().toString().trim();
            if (searchUsername.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a username to search", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUserByUsername(searchUsername);
        });
        Log.d(TAG, "onCreate: Completed");
    }

    private void loadSelectedGames() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "loadSelectedGames: Loading games for userId: " + userId);
        executorService.execute(() -> db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            Object gamesObject = task.getResult().get("selectedGames");
                            selectedGames = parseGameList(gamesObject);
                            isGamesLoaded = true;
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Games loaded: " + selectedGames.size(), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "loadSelectedGames: Games loaded: " + selectedGames.size());
                                checkIfReady();
                            });
                        } else {
                            Log.w(TAG, "loadSelectedGames: User document does not exist for userId: " + userId);
                            runOnUiThread(() -> Toast.makeText(this, "User document does not exist for userId: " + userId, Toast.LENGTH_LONG).show());
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "loadSelectedGames: Error loading games: " + errorMessage);
                        runOnUiThread(() -> Toast.makeText(this, "Error loading games: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    private void loadAllUsers() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        Log.d(TAG, "loadAllUsers: Loading all users, currentUserId: " + currentUserId);
        executorService.execute(() -> db.collection("users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allUsers.clear();
                        int totalUsers = task.getResult().size();
                        int skippedUsersCount = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String userId = doc.getId();
                            String username = doc.getString("username");
                            List<String> games = parseGameList(doc.get("selectedGames"));
                            if (username == null || username.trim().isEmpty()) {
                                Log.d(TAG, "loadAllUsers: Skipping userId: " + userId + " because username is null or empty");
                                skippedUsersCount++;
                                continue;
                            }
                            if (userId.equals(currentUserId)) {
                                Log.d(TAG, "loadAllUsers: Skipping userId: " + userId + " because it matches currentUserId");
                                skippedUsersCount++;
                                continue;
                            }
                            allUsers.add(new User(userId, username, games));
                        }
                        int finalSkippedUsersCount = skippedUsersCount;
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Loaded " + allUsers.size() + " users. Total: " + totalUsers + ", Skipped: " + finalSkippedUsersCount, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "loadAllUsers: Loaded " + allUsers.size() + " users. Total: " + totalUsers + ", Skipped: " + finalSkippedUsersCount);
                            checkIfReady();
                        });
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "loadAllUsers: Load failed: " + errorMessage);
                        runOnUiThread(() -> Toast.makeText(this, "Load failed: " + errorMessage, Toast.LENGTH_LONG).show());
                    }
                }));
    }

    private void checkIfReady() {
        // No implementation needed
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

    @SuppressLint({"StringFormatInvalid", "SetTextI18n"})
    private void findMatchingUser() {
        Log.d(TAG, "findMatchingUser: Checking if data is loaded");
        if (!isGamesLoaded || allUsers.isEmpty()) {
            Log.w(TAG, "findMatchingUser: Data not loaded. isGamesLoaded: " + isGamesLoaded + ", allUsers size: " + allUsers.size());
            resultTextView.setText("Data not loaded");
            resultTextView.setVisibility(View.VISIBLE);
            return;
        }

        List<User> availableUsers = new ArrayList<>();
        for (User user : allUsers) {
            if (!user.getUserId().equals(lastSkippedUserId)) {
                availableUsers.add(user);
            }
        }

        if (availableUsers.isEmpty()) {
            Log.w(TAG, "findMatchingUser: No users available after filtering last skipped user");
            resultTextView.setText("No users available");
            resultTextView.setVisibility(View.VISIBLE);
            lastSkippedUserId = null;
            return;
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

        Log.d(TAG, "findMatchingUser: Navigating to UserProfileActivity for user: " + selectedUser.getUsername());
        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", selectedUser.getUserId());
        intent.putExtra("username", selectedUser.getUsername());
        userProfileLauncher.launch(intent);

        lastSkippedUserId = null;
    }

    private void searchUserByUsername(String searchUsername) {
        Log.d(TAG, "searchUserByUsername: Searching for username: " + searchUsername);
        if (allUsers.isEmpty()) {
            Log.w(TAG, "searchUserByUsername: No users loaded");
            Toast.makeText(this, "No users loaded", Toast.LENGTH_LONG).show();
            return;
        }

        User foundUser = null;
        for (User user : allUsers) {
            if (user.getUsername().equalsIgnoreCase(searchUsername)) {
                foundUser = user;
                break;
            }
        }

        if (foundUser == null) {
            Log.w(TAG, "searchUserByUsername: User not found: " + searchUsername);
            Toast.makeText(this, "User not found: " + searchUsername, Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "searchUserByUsername: Navigating to UserProfileActivity for user: " + foundUser.getUsername());
        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", foundUser.getUserId());
        intent.putExtra("username", foundUser.getUsername());
        userProfileLauncher.launch(intent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}