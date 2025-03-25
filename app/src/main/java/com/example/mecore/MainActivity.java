package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity started");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not logged in");
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
            return;
        }

        Button findUserButton = findViewById(R.id.findUserButton);
        Button searchUserButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
        resultTextView = findViewById(R.id.resultTextView);

        findUserButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Finding user...", Toast.LENGTH_SHORT).show();
            findMatchingUser();
        });

        searchUserButton.setOnClickListener(v -> {
            String searchUsername = searchEditText.getText().toString().trim();
            if (searchUsername.isEmpty()) {
                Toast.makeText(MainActivity.this, "Enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            searchUserByUsername(searchUsername);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onNavigationItemSelected);
        bottomNavigationView.setSelectedItemId(R.id.navigation_main); // Highlight Main by default

        loadSelectedGames();
        loadAllUsers();
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.navigation_main) {
            return true; // Stay on MainActivity
        } else if (item.getItemId() == R.id.navigation_chat) {
            startActivity(new Intent(this, ChatActivity.class));
            return true;
        } else if (item.getItemId() == R.id.navigation_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return false;
    }

    private void loadSelectedGames() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        executorService.execute(() -> db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            Object gamesObject = task.getResult().get("selectedGames");
                            selectedGames = parseGameList(gamesObject);
                            isGamesLoaded = true;
                        }
                    }
                }));
    }

    private void loadAllUsers() {
        String currentUserId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        executorService.execute(() -> db.collection("users").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allUsers.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String userId = doc.getId();
                            String username = doc.getString("username");
                            List<String> games = parseGameList(doc.get("selectedGames"));
                            if (username != null && !userId.equals(currentUserId)) {
                                allUsers.add(new User(userId, username, games));
                            }
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

    private void findMatchingUser() {
        if (!isGamesLoaded || allUsers.isEmpty()) {
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

        User selectedUser = matches.isEmpty() ? availableUsers.get(0) : matches.get(0);
        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", selectedUser.getUserId());
        intent.putExtra("username", selectedUser.getUsername());
        startActivity(intent);
        lastSkippedUserId = null;
    }

    private void searchUserByUsername(String searchUsername) {
        if (allUsers.isEmpty()) {
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
            Toast.makeText(this, "User not found: " + searchUsername, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", foundUser.getUserId());
        intent.putExtra("username", foundUser.getUsername());
        startActivity(intent);
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
