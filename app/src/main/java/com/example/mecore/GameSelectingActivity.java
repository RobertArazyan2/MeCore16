package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameSelectingActivity extends AppCompatActivity {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchGame2, switchGame3, switchGame4, switchGame6,
            switchGame7, switchGame8, switchGame9, switchGame10, switchGame11, switchGame12,
            switchGame13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_selecting);

        // Initialize the Switches
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

        // Initialize the Submit Button
        Button submitButton = findViewById(R.id.submitGamesSelection);

        // Handle the Submit Button click
        submitButton.setOnClickListener(v -> {
            StringBuilder selectedGames = new StringBuilder("Selected Games: ");

            if (switchGame2.isChecked()) selectedGames.append("Minecraft, ");
            if (switchGame3.isChecked()) selectedGames.append("League of Legends, ");
            if (switchGame4.isChecked()) selectedGames.append("PUBG, ");
            if (switchGame6.isChecked()) selectedGames.append("Apex Legends, ");
            if (switchGame7.isChecked()) selectedGames.append("Call of Duty, ");
            if (switchGame8.isChecked()) selectedGames.append("Counter-Strike, ");
            if (switchGame9.isChecked()) selectedGames.append("Overwatch, ");
            if (switchGame10.isChecked()) selectedGames.append("Rocket League, ");
            if (switchGame11.isChecked()) selectedGames.append("Rainbow Six Siege, ");
            if (switchGame12.isChecked()) selectedGames.append("Among Us, ");
            if (switchGame13.isChecked()) selectedGames.append("Hearthstone, ");

            // If no games are selected
            if (selectedGames.toString().equals("Selected Games: ")) {
                Toast.makeText(this, "No games selected!", Toast.LENGTH_SHORT).show();
            } else {
                // Remove the trailing comma
                selectedGames.setLength(selectedGames.length() - 2);
                Toast.makeText(this, selectedGames.toString(), Toast.LENGTH_LONG).show();

                // Navigate to the next page (e.g., MainActivity)
                Intent intent = new Intent(GameSelectingActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Optional: if you want to remove the current activity from the back stack
            }
        });
    }
}
