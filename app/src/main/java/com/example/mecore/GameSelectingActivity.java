package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class GameSelectingActivity extends AppCompatActivity {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchGame2, switchGame3, switchGame4, switchGame6,
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
        switchGame6 = findViewById(R.id.switchGame6);
        switchGame7 = findViewById(R.id.switchGame7);
        switchGame8 = findViewById(R.id.switchGame8);
        switchGame9 = findViewById(R.id.switchGame9);
        switchGame10 = findViewById(R.id.switchGame10);
        switchGame11 = findViewById(R.id.switchGame11);
        switchGame12 = findViewById(R.id.switchGame12);
        switchGame13 = findViewById(R.id.switchGame13);

        Button submitButton = findViewById(R.id.submitGamesSelection);

        submitButton.setOnClickListener(v -> {
            List<String> selectedGames = new ArrayList<>();

            // Add selected games to the list
            if (switchGame2.isChecked()) selectedGames.add("Minecraft");
            if (switchGame3.isChecked()) selectedGames.add("League of Legends");
            if (switchGame4.isChecked()) selectedGames.add("PUBG");
            if (switchGame6.isChecked()) selectedGames.add("Apex Legends");
            if (switchGame7.isChecked()) selectedGames.add("Call of Duty");
            if (switchGame8.isChecked()) selectedGames.add("Counter-Strike");
            if (switchGame9.isChecked()) selectedGames.add("Overwatch");
            if (switchGame10.isChecked()) selectedGames.add("Rocket League");
            if (switchGame11.isChecked()) selectedGames.add("Rainbow Six Siege");
            if (switchGame12.isChecked()) selectedGames.add("Among Us");
            if (switchGame13.isChecked()) selectedGames.add("Hearthstone");

            // Check if no games are selected
            if (selectedGames.isEmpty()) {
                Toast.makeText(this, "No games selected!", Toast.LENGTH_SHORT).show();
            } else {
                //Check if user is logged in.
                if (auth.getCurrentUser() != null) {
                    String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                    saveGamesToFirestore(userId, selectedGames);

                    // Navigate to the next activity (MainActivity)
                    Intent intent = new Intent(GameSelectingActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Save selected games to Firestore
    private void saveGamesToFirestore(String userId, List<String> selectedGames) {
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Create the document if it doesn't exist, then update the "selectedGames" field
        userDocRef.set(new HashMap<String, Object>()) // Create an empty document if it doesn't exist
                .addOnSuccessListener(aVoid -> userDocRef.update("selectedGames", selectedGames)
                        .addOnSuccessListener(aVoid2 -> Toast.makeText(GameSelectingActivity.this, "Games saved!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(GameSelectingActivity.this, "Error saving games: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(GameSelectingActivity.this, "Error creating document: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}