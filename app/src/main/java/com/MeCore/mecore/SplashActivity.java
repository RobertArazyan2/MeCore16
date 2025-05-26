package com.MeCore.mecore;

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

        new Handler().postDelayed(this::checkUserState, 2000); // 2-second delay
    }

    private void checkUserState() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);

            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().contains("selectedGames")) {
                        Log.d("SplashActivity", "User has selected games");
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        Log.d("SplashActivity", "User has not selected games");
                        startActivity(new Intent(this, GameSelectingActivity.class));
                    }
                } else {
                    Log.e("SplashActivity", "Error fetching user data: " +
                            Objects.requireNonNull(task.getException()).getMessage());
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                }
                finish();
            });
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
