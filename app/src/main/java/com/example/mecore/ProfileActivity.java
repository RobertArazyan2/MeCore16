package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private EditText usernameEditText;
    private TextView selectedGamesTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUsername;
    private List<String> currentSelectedGames;
    private List<String> selectedGames = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    @SuppressLint("SetTextI18n")
    private final ActivityResultLauncher<Intent> gameSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedGames = result.getData().getStringArrayListExtra("updatedGames");
                    if (selectedGames == null) {
                        selectedGames = new ArrayList<>();
                    }
                    if (selectedGamesTextView != null) {
                        selectedGamesTextView.setText("Selected Games: " + (selectedGames.isEmpty() ? "None" : String.join(", ", selectedGames)));
                    } else {
                        Log.e(TAG, "selectedGamesTextView is null in gameSelectionLauncher");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usernameEditText = findViewById(R.id.editTextUsername);
        selectedGamesTextView = findViewById(R.id.selectedGamesTextView);
        Button saveButton = findViewById(R.id.buttonSave);
        Button changeGamesButton = findViewById(R.id.buttonChangeGames);
        Button logoutButton = findViewById(R.id.buttonLogout);

        if (selectedGamesTextView == null) {
            Log.e(TAG, "selectedGamesTextView is null after findViewById. Check activity_profile.xml for ID selectedGamesTextView");
            Toast.makeText(this, "Error: selectedGamesTextView not found in layout", Toast.LENGTH_LONG).show();
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up BottomNavigationMenu
        Log.d(TAG, "onCreate: Setting up BottomNavigationMenu");
        BottomNavigationView bottomNavigationMenu = findViewById(R.id.bottom_navigation);
        if (bottomNavigationMenu == null) {
            Log.e(TAG, "BottomNavigationMenu is null! Check activity_profile.xml for ID bottom_navigation");
            Toast.makeText(this, "BottomNavigationMenu not found in layout!", Toast.LENGTH_LONG).show();
            return;
        } else {
            Log.d(TAG, "BottomNavigationMenu found successfully");
        }

        bottomNavigationMenu.setSelectedItemId(R.id.navigation_profile);
        bottomNavigationMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            String itemName;
            try {
                itemName = getResources().getResourceEntryName(itemId);
                Log.d("BottomNav", "Item selected: ID=" + itemId + ", Name=" + itemName);
            } catch (Exception e) {
                itemName = "Unknown";
                Log.e("BottomNav", "Failed to get resource name for ID: " + itemId, e);
            }

            if (itemId == R.id.navigation_main) {
                Log.d("BottomNav", "Navigation Main tab clicked, opening MainActivity");
                try {
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.putExtra("selectedTabId", R.id.navigation_main);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    Log.d("BottomNav", "MainActivity started successfully");
                    // Apply slide-left animation (Profile → Main)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                } catch (Exception e) {
                    Log.e("BottomNav", "Failed to start MainActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to open Main: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return true;
            } else if (itemId == R.id.navigation_profile) {
                Log.d("BottomNav", "Already on ProfileActivity (navigation_profile)");
                return true;
            } else if (itemId == R.id.navigation_chat) {
                Log.d("BottomNav", "Opening ChatListActivity");
                try {
                    Intent intent = new Intent(ProfileActivity.this, ChatListActivity.class);
                    intent.putExtra("selectedTabId", R.id.navigation_chat);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    Log.d("BottomNav", "ChatListActivity started successfully");
                    // Apply slide-left animation (Profile → Chats)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                } catch (Exception e) {
                    Log.e("BottomNav", "Failed to start ChatListActivity: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to open Chat: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                return true;
            }
            Log.w("BottomNav", "Unknown item ID: " + itemId);
            return false;
        });

        loadUserProfile();

        saveButton.setOnClickListener(v -> saveUserProfile());

        changeGamesButton.setOnClickListener(v -> {
            Log.d(TAG, "Launching GameSelectingActivity with selectedGames: " + (selectedGames != null ? selectedGames.toString() : "null"));
            Intent intent = new Intent(ProfileActivity.this, GameSelectingActivity.class);
            intent.putStringArrayListExtra("selectedGames", new ArrayList<>(selectedGames));
            intent.putExtra("fromProfile", true);
            gameSelectionLauncher.launch(intent);
        });

        // Set up logout button
        if (logoutButton == null) {
            Log.e(TAG, "logoutButton is null! Check activity_profile.xml for ID buttonLogout");
            Toast.makeText(this, "Error: Logout button not found in layout", Toast.LENGTH_LONG).show();
        } else {
            logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void logout() {
        Log.d(TAG, "Logging out user");
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Apply fade animation for logout
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
        finish();
    }

    @SuppressLint("SetTextI18n")
    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            runOnUiThread(() -> Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show());
            return;
        }

        executorService.execute(() -> {
            DocumentReference userRef = db.collection("users").document(user.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String loadedUsername = documentSnapshot.getString("username");
                    currentSelectedGames = parseGameList(documentSnapshot.get("selectedGames"));
                    selectedGames = new ArrayList<>(currentSelectedGames != null ? currentSelectedGames : new ArrayList<>());
                    Log.d(TAG, "Loaded user profile from Firestore: username=" + loadedUsername + ", selectedGames=" + selectedGames.toString());
                    if (loadedUsername != null && !loadedUsername.equals(currentUsername)) {
                        Log.w(TAG, "Username mismatch! Firestore username=" + loadedUsername + ", currentUsername=" + currentUsername);
                    }
                    currentUsername = loadedUsername;
                    if (selectedGames != null && selectedGames.contains("Warzone")) {
                        Log.d(TAG, "Warzone is in selectedGames loaded from Firestore");
                    } else {
                        Log.d(TAG, "Warzone is NOT in selectedGames loaded from Firestore");
                    }

                    runOnUiThread(() -> {
                        usernameEditText.setText(currentUsername);
                        if (selectedGamesTextView != null) {
                            selectedGamesTextView.setText("Selected Games: " + (selectedGames.isEmpty() ? "None" : String.join(", ", selectedGames)));
                        } else {
                            Log.e(TAG, "selectedGamesTextView is null in loadUserProfile");
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show());
                }
            }).addOnFailureListener(e -> runOnUiThread(() -> {
                Log.e(TAG, "Failed to load profile: " + e.getMessage(), e);
                Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }));
        });
    }

    private void saveUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = usernameEditText.getText().toString().trim();
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newUsername.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean usernameChanged = !newUsername.equals(currentUsername);
        boolean gamesChanged = !areGamesEqual(selectedGames, currentSelectedGames);

        if (!usernameChanged && !gamesChanged) {
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (usernameChanged) {
            db.collection("usernames").document(newUsername).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(this, "Username already taken, please choose a different one", Toast.LENGTH_LONG).show();
                        } else {
                            if (currentUsername != null && !currentUsername.isEmpty()) {
                                db.collection("usernames").document(currentUsername).get()
                                        .addOnSuccessListener(oldUsernameSnapshot -> {
                                            if (oldUsernameSnapshot.exists() && oldUsernameSnapshot.getString("userId").equals(user.getUid())) {
                                                updateUserProfile(user.getUid(), newUsername);
                                            } else {
                                                Log.w(TAG, "Old username " + currentUsername + " does not belong to user " + user.getUid());
                                                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_LONG).show();
                                                updateUserProfileWithoutUsernames(user.getUid(), newUsername);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to verify old username: " + e.getMessage(), e);
                                            Toast.makeText(this, "Failed to verify old username: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            updateUserProfileWithoutUsernames(user.getUid(), newUsername);
                                        });
                            } else {
                                updateUserProfile(user.getUid(), newUsername);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to check username availability: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to check username availability: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            updateUserProfileWithoutUsernames(user.getUid(), newUsername);
        }
    }

    private void updateUserProfile(String userId, String newUsername) {
        db.runBatch(batch -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", newUsername);
            userData.put("selectedGames", selectedGames);
            Log.d(TAG, "Updating user document with data: " + userData.toString());
            batch.update(db.collection("users").document(userId), userData);

            if (currentUsername != null && !currentUsername.isEmpty()) {
                Log.d(TAG, "Deleting old username: " + currentUsername);
                batch.delete(db.collection("usernames").document(currentUsername));
                Map<String, Object> usernameData = new HashMap<>();
                usernameData.put("userId", userId);
                Log.d(TAG, "Creating new username entry: " + newUsername + " with data: " + usernameData.toString());
                batch.set(db.collection("usernames").document(newUsername), usernameData);
            }
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Batch write succeeded");
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> savedGames = parseGameList(documentSnapshot.get("selectedGames"));
                    Log.d(TAG, "Games in Firestore after save: " + savedGames.toString());
                    if (!areGamesEqual(savedGames, selectedGames)) {
                        Log.e(TAG, "Games not saved correctly! Expected: " + selectedGames + ", Got: " + savedGames);
                        Toast.makeText(this, "Error: Games not saved correctly", Toast.LENGTH_LONG).show();
                    }
                }
            });
            currentUsername = newUsername;
            currentSelectedGames = new ArrayList<>(selectedGames);
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            String errorMessage = e.getMessage();
            Log.e(TAG, "Failed to update profile: " + errorMessage, e);
            if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                Toast.makeText(this, "Failed to update profile: Permission denied", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateUserProfileWithoutUsernames(String userId, String newUsername) {
        Map<String, Object> userData = new HashMap<>();
        boolean usernameChanged = !newUsername.equals(currentUsername);
        boolean gamesChanged = !areGamesEqual(selectedGames, currentSelectedGames);

        if (usernameChanged) {
            userData.put("username", newUsername);
        }
        if (gamesChanged) {
            userData.put("selectedGames", selectedGames);
        }

        if (userData.isEmpty()) {
            Log.d(TAG, "No fields to update in user document");
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Updating user document without usernames collection changes: " + userData.toString());
        db.collection("users").document(userId).update(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document updated successfully");
                    if (usernameChanged) {
                        currentUsername = newUsername;
                    }
                    if (gamesChanged) {
                        currentSelectedGames = new ArrayList<>(selectedGames);
                    }
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage();
                    Log.e(TAG, "Failed to update profile without usernames: " + errorMessage, e);
                    if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                        Toast.makeText(this, "Failed to update profile: Permission denied", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean areGamesEqual(List<String> games1, List<String> games2) {
        if (games1 == null && games2 == null) return true;
        if (games1 == null || games2 == null) return false;
        return games1.size() == games2.size() && new HashSet<>(games1).containsAll(games2) && new HashSet<>(games2).containsAll(games1);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}