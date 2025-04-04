package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameSelectingActivity extends AppCompatActivity {

    private static final String TAG = "GameSelectingActivity";
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchGame2, switchGame3, switchGame4, switchGame5, switchGame6,
            switchGame7, switchGame8, switchGame9, switchGame10, switchGame11, switchGame12,
            switchGame13;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_selecting);

        // Initialize switches
        switchGame2 = findViewById(R.id.switchGame2);
        switchGame3 = findViewById(R.id.switchGame3);
        switchGame4 = findViewById(R.id.switchGame4);
        switchGame5 = findViewById(R.id.switchGame5);
        switchGame6 = findViewById(R.id.switchGame6);
        switchGame7 = findViewById(R.id.switchGame7);
        switchGame8 = findViewById(R.id.switchGame8);
        switchGame9 = findViewById(R.id.switchGame9);
        switchGame10 = findViewById(R.id.switchGame10);
        switchGame11 = findViewById(R.id.switchGame11);
        switchGame12 = findViewById(R.id.switchGame12);
        switchGame13 = findViewById(R.id.switchGame13);

        Button submitButton = findViewById(R.id.submitGamesSelection);

        // Get the previously selected games from the Intent (if coming from ProfileActivity)
        List<String> selectedGames = getIntent().getStringArrayListExtra("selectedGames");
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);

        // Log the selectedGames list to debug
        if (selectedGames != null) {
            Log.d(TAG, "Received selectedGames: " + selectedGames.toString());
            if (selectedGames.contains("Warzone")) {
                Log.d(TAG, "Warzone is in selectedGames, setting switchGame5 to true");
                switchGame5.setChecked(true);
            } else {
                Log.d(TAG, "Warzone is NOT in selectedGames, switchGame5 remains unchecked");
            }
            if (selectedGames.contains("Fortnite")) {
                Log.d(TAG, "Fortnite is in selectedGames, setting switchGame7 to true");
                switchGame7.setChecked(true);
            } else {
                Log.d(TAG, "Fortnite is NOT in selectedGames, switchGame7 remains unchecked");
            }

            // Pre-set the switches based on selected games
            if (selectedGames.contains("Minecraft")) switchGame2.setChecked(true);
            if (selectedGames.contains("League of Legends")) switchGame3.setChecked(true);
            if (selectedGames.contains("PUBG")) switchGame4.setChecked(true);
            if (selectedGames.contains("Apex Legends")) switchGame6.setChecked(true);
            if (selectedGames.contains("Counter-Strike: Global Offensive")) switchGame8.setChecked(true);
            if (selectedGames.contains("Overwatch")) switchGame9.setChecked(true);
            if (selectedGames.contains("Rocket League")) switchGame10.setChecked(true);
            if (selectedGames.contains("Roblox")) switchGame11.setChecked(true);
            if (selectedGames.contains("Among Us")) switchGame12.setChecked(true);
            if (selectedGames.contains("Hearthstone")) switchGame13.setChecked(true);
        } else {
            Log.d(TAG, "selectedGames is null");
        }

        submitButton.setOnClickListener(v -> {
            List<String> updatedGames = new ArrayList<>();

            // Add selected games to the list
            if (switchGame2.isChecked()) updatedGames.add("Minecraft");
            if (switchGame3.isChecked()) updatedGames.add("League of Legends");
            if (switchGame4.isChecked()) updatedGames.add("PUBG");
            if (switchGame5.isChecked()) updatedGames.add("Warzone");
            if (switchGame6.isChecked()) updatedGames.add("Apex Legends");
            if (switchGame7.isChecked()) updatedGames.add("Fortnite");
            if (switchGame8.isChecked()) updatedGames.add("Counter-Strike: Global Offensive");
            if (switchGame9.isChecked()) updatedGames.add("Overwatch");
            if (switchGame10.isChecked()) updatedGames.add("Rocket League");
            if (switchGame11.isChecked()) updatedGames.add("Roblox");
            if (switchGame12.isChecked()) updatedGames.add("Among Us");
            if (switchGame13.isChecked()) updatedGames.add("Hearthstone");

            // Log the updated games
            Log.d(TAG, "Submitting updatedGames: " + updatedGames.toString());

            // Check if no games are selected
            if (updatedGames.isEmpty()) {
                Toast.makeText(this, "No games selected!", Toast.LENGTH_SHORT).show();
            } else {
                // Check if user is logged in
                if (auth.getCurrentUser() != null) {
                    String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                    saveGamesToFirestore(userId, updatedGames);

                    if (fromProfile) {
                        // Return the updated games list to ProfileActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putStringArrayListExtra("updatedGames", new ArrayList<>(updatedGames));
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        // Navigate to MainActivity (initial game selection flow)
                        Intent intent = new Intent(GameSelectingActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Save selected games to Firestore
    private void saveGamesToFirestore(String userId, List<String> selectedGames) {
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.update("selectedGames", selectedGames)
                .addOnSuccessListener(aVoid -> Toast.makeText(GameSelectingActivity.this, "Games saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(GameSelectingActivity.this, "Error saving games: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}