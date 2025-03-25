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
import com.bumptech.glide.Glide; // Fixed Glide import
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
                String username = documentSnapshot.getString("username");
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                usernameEditText.setText(username);

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).into(profileImageView);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void saveUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String newUsername = usernameEditText.getText().toString().trim();
        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", newUsername);

        db.collection("users").document(user.getUid()).update(userData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed!", Toast.LENGTH_SHORT).show());
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