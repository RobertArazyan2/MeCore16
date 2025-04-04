package com.example.mecore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private EditText usernameEditText;
    private ImageView profileImageView;
    private TextView selectedGamesTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentUsername;
    private List<String> selectedGames;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        profileImageView.setImageURI(imageUri);
                        uploadProfileImage(imageUri, Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                    }
                }
            });
    private final ActivityResultLauncher<Intent> gameSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedGames = result.getData().getStringArrayListExtra("updatedGames");
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
        profileImageView = findViewById(R.id.profileImageView);

        if (selectedGamesTextView == null) {
            Log.e(TAG, "selectedGamesTextView is null after findViewById. Check activity_profile.xml for ID selectedGamesTextView");
            Toast.makeText(this, "Error: selectedGamesTextView not found in layout", Toast.LENGTH_LONG).show();
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

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

        profileImageView.setOnClickListener(v -> openFileChooser());

        saveButton.setOnClickListener(v -> saveUserProfile());

        changeGamesButton.setOnClickListener(v -> {
            Log.d(TAG, "Launching GameSelectingActivity with selectedGames: " + (selectedGames != null ? selectedGames.toString() : "null"));
            Intent intent = new Intent(ProfileActivity.this, GameSelectingActivity.class);
            intent.putStringArrayListExtra("selectedGames", new ArrayList<>(selectedGames));
            intent.putExtra("fromProfile", true);
            gameSelectionLauncher.launch(intent);
        });
    }

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
                    currentUsername = documentSnapshot.getString("username");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                    selectedGames = parseGameList(documentSnapshot.get("selectedGames"));
                    Log.d(TAG, "Loaded selectedGames from Firestore: " + (selectedGames != null ? selectedGames.toString() : "null"));
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

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(ProfileActivity.this).load(profileImageUrl).into(profileImageView);
                        }
                    });
                }
            }).addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
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

        if (newUsername.equals(currentUsername)) {
            Toast.makeText(this, "No changes to username", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("usernames").document(newUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "Username already taken, please choose a different one", Toast.LENGTH_LONG).show();
                    } else {
                        updateUsername(user.getUid(), newUsername);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check username availability: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateUsername(String userId, String newUsername) {
        db.runBatch(batch -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", newUsername);
            batch.update(db.collection("users").document(userId), userData);

            if (currentUsername != null && !currentUsername.isEmpty()) {
                batch.delete(db.collection("usernames").document(currentUsername));
            }

            Map<String, Object> usernameData = new HashMap<>();
            usernameData.put("userId", userId);
            batch.set(db.collection("usernames").document(newUsername), usernameData);
        }).addOnSuccessListener(aVoid -> {
            currentUsername = newUsername;
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("PERMISSION_DENIED")) {
                Toast.makeText(this, "Failed to update profile: Permission denied", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
    }

    private void uploadProfileImage(Uri imageUri, String userId) {
        StorageReference fileRef = storage.getReference("profile_pictures").child(userId + ".jpg");
        fileRef.putFile(imageUri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
            return fileRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            db.collection("users").document(userId).update("profileImageUrl", uri.toString());
            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show());
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