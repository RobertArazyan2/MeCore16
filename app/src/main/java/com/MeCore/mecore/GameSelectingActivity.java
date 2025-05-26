package com.MeCore.mecore;

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

        List<String> selectedGames = getIntent().getStringArrayListExtra("selectedGames");
        boolean fromProfile = getIntent().getBooleanExtra("fromProfile", false);

        if (selectedGames != null) {
            Log.d(TAG, "Received selectedGames: " + selectedGames.toString());

            if (switchGame2 != null && selectedGames.contains("Minecraft")) switchGame2.setChecked(true);
            if (switchGame3 != null && selectedGames.contains("League of Legends")) switchGame3.setChecked(true);
            if (switchGame4 != null && selectedGames.contains("PUBG")) switchGame4.setChecked(true);
            if (switchGame5 != null && selectedGames.contains("Warzone")) switchGame5.setChecked(true);
            if (switchGame6 != null && selectedGames.contains("Apex Legends")) switchGame6.setChecked(true);
            if (switchGame7 != null && selectedGames.contains("Fortnite")) switchGame7.setChecked(true);
            if (switchGame8 != null && selectedGames.contains("Counter-Strike: Global Offensive")) switchGame8.setChecked(true);
            if (switchGame9 != null && selectedGames.contains("Overwatch")) switchGame9.setChecked(true);
            if (switchGame10 != null && selectedGames.contains("Rocket League")) switchGame10.setChecked(true);
            if (switchGame11 != null && selectedGames.contains("Roblox")) switchGame11.setChecked(true);
            if (switchGame12 != null && selectedGames.contains("Among Us")) switchGame12.setChecked(true);
            if (switchGame13 != null && selectedGames.contains("Hearthstone")) switchGame13.setChecked(true);
        } else {
            Log.d(TAG, "selectedGames is null");
        }

        submitButton.setOnClickListener(v -> {
            List<String> updatedGames = new ArrayList<>();

            if (switchGame2 != null && switchGame2.isChecked()) updatedGames.add("Minecraft");
            if (switchGame3 != null && switchGame3.isChecked()) updatedGames.add("League of Legends");
            if (switchGame4 != null && switchGame4.isChecked()) updatedGames.add("PUBG");
            if (switchGame5 != null && switchGame5.isChecked()) updatedGames.add("Warzone");
            if (switchGame6 != null && switchGame6.isChecked()) updatedGames.add("Apex Legends");
            if (switchGame7 != null && switchGame7.isChecked()) updatedGames.add("Fortnite");
            if (switchGame8 != null && switchGame8.isChecked()) updatedGames.add("Counter-Strike: Global Offensive");
            if (switchGame9 != null && switchGame9.isChecked()) updatedGames.add("Overwatch");
            if (switchGame10 != null && switchGame10.isChecked()) updatedGames.add("Rocket League");
            if (switchGame11 != null && switchGame11.isChecked()) updatedGames.add("Roblox");
            if (switchGame12 != null && switchGame12.isChecked()) updatedGames.add("Among Us");
            if (switchGame13 != null && switchGame13.isChecked()) updatedGames.add("Hearthstone");

            Log.d(TAG, "Submitting updatedGames: " + updatedGames.toString());

            if (updatedGames.isEmpty()) {
                Toast.makeText(this, "No games selected!", Toast.LENGTH_SHORT).show();
            } else {
                if (auth.getCurrentUser() != null) {
                    String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                    saveGamesToFirestore(userId, updatedGames);

                    if (fromProfile) {
                        Intent resultIntent = new Intent();
                        resultIntent.putStringArrayListExtra("updatedGames", new ArrayList<>(updatedGames));
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
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

    private void saveGamesToFirestore(String userId, List<String> selectedGames) {
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.update("selectedGames", selectedGames)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(GameSelectingActivity.this, "Games saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(GameSelectingActivity.this, "Error saving games: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
