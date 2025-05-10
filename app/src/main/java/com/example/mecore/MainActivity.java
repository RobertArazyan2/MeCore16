package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button findUserButton = findViewById(R.id.findUserButton);
        Button searchUserButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
        resultTextView = findViewById(R.id.resultTextView);

        BottomNavigationView bottomNavigationMenu = findViewById(R.id.bottom_navigation);
        NavigationUtil.setupBottomNavigationMenu(this, bottomNavigationMenu, R.id.navigation_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                String username = "user_" + userId.substring(0, 5);
                Map<String, Object> usernameData = new HashMap<>();
                usernameData.put("userId", userId);

                db.collection("usernames").document(username).set(usernameData)
                        .addOnSuccessListener(unused -> {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("status", "online");
                            userData.put("lookingForGame", false);
                            userData.put("selectedGames", new ArrayList<String>());

                            db.collection("users").document(userId).set(userData)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User created successfully"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to create user doc", e));
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to create username doc", e));
            }
        });

        loadSelectedGames();
        loadAllUsers();

        findUserButton.setOnClickListener(v -> findMatchingUser());

        searchUserButton.setOnClickListener(v -> {
            String searchUsername = searchEditText.getText().toString().trim();
            if (!searchUsername.isEmpty()) {
                searchUserByUsername(searchUsername);
            }
        });
    }

    private void loadSelectedGames() {
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        executorService.execute(() -> db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Object gamesObject = task.getResult().get("selectedGames");
                        selectedGames = parseGameList(gamesObject);
                        isGamesLoaded = true;
                        runOnUiThread(this::checkIfReady);
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
                            if (username == null || username.trim().isEmpty() || userId.equals(currentUserId)) {
                                continue;
                            }
                            allUsers.add(new User(userId, username, games));
                        }
                        runOnUiThread(this::checkIfReady);
                    }
                }));
    }

    private void checkIfReady() {}

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

        User selectedUser;
        if (matches.isEmpty()) {
            Collections.shuffle(availableUsers);
            selectedUser = availableUsers.get(0);
        } else {
            Collections.shuffle(matches);
            selectedUser = matches.get(0);
        }

        Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
        intent.putExtra("userId", selectedUser.getUserId());
        intent.putExtra("username", selectedUser.getUsername());
        userProfileLauncher.launch(intent);

        lastSkippedUserId = null;
    }

    private void searchUserByUsername(String searchUsername) {
        if (allUsers.isEmpty()) return;

        User foundUser = null;
        for (User user : allUsers) {
            if (user.getUsername().equalsIgnoreCase(searchUsername)) {
                foundUser = user;
                break;
            }
        }

        if (foundUser == null) return;

        Intent intent = new Intent(MainActivity.this, SearchUserProfileActivity.class);
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
