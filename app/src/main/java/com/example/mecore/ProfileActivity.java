package com.example.mecore;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private ImageView profileImageView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String currentUsername; // To store the current username
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        usernameEditText = findViewById(R.id.editTextUsername);
        Button saveButton = findViewById(R.id.buttonSave);
        Button changeGamesButton = findViewById(R.id.buttonChangeGames);
        profileImageView = findViewById(R.id.profileImageView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        loadUserProfile();

        profileImageView.setOnClickListener(v -> openFileChooser());

        saveButton.setOnClickListener(v -> saveUserProfile());

        changeGamesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, GameSelectingActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DocumentReference userRef = db.collection("users").document(user.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentUsername = documentSnapshot.getString("username"); // Store the current username
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                usernameEditText.setText(currentUsername);

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).into(profileImageView);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

        // Check if the username has changed
        if (newUsername.equals(currentUsername)) {
            Toast.makeText(this, "No changes to username", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the new username is already taken
        db.collection("usernames").document(newUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "Username already taken, please choose a different one", Toast.LENGTH_LONG).show();
                    } else {
                        // Username is available, proceed with the update
                        updateUsername(user.getUid(), newUsername);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to check username availability: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void updateUsername(String userId, String newUsername) {
        // Use a batch write to update both the users and usernames collections atomically
        db.runBatch(batch -> {
            // Update the users collection
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", newUsername);
            batch.update(db.collection("users").document(userId), userData);

            // Delete the old username entry from usernames collection
            if (currentUsername != null && !currentUsername.isEmpty()) {
                batch.delete(db.collection("usernames").document(currentUsername));
            }

            // Create a new username entry in usernames collection
            Map<String, Object> usernameData = new HashMap<>();
            usernameData.put("userId", userId);
            batch.set(db.collection("usernames").document(newUsername), usernameData);
        }).addOnSuccessListener(aVoid -> {
            currentUsername = newUsername; // Update the current username
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
}