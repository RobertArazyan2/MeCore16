package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.Objects;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(() -> {
            if (auth.getCurrentUser() != null) {
                // User is logged in, check if games are selected
                String userId = auth.getCurrentUser().getUid();
                DocumentReference userDocRef = db.collection("users").document(userId);
                userDocRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().contains("selectedGames")) {
                            // User has selected games, go to MainActivity
                            Log.d("SplashActivity", "User has selected games");
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            // User hasn't selected games, go to GameSelectingActivity
                            Log.d("SplashActivity", "User has not selected games");
                            startActivity(new Intent(SplashActivity.this, GameSelectingActivity.class));
                        }
                    } else {
                        // Error fetching data
                        Log.e("SplashActivity", "Error fetching user data: " + Objects.requireNonNull(task.getException()).getMessage());
                        Toast.makeText(SplashActivity.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // User not logged in, go to LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish(); // Finish the splash activity
        }, 2000); // Wait for 2 seconds before navigating
    }
}
