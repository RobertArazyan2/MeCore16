package com.example.mecore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class UserProfileActivity extends AppCompatActivity {

    private String currentUserId;
    private String matchedUserId;
    private String matchedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        currentUserId = mAuth.getCurrentUser().getUid();

        matchedUserId = getIntent().getStringExtra("userId");
        matchedUsername = getIntent().getStringExtra("username");

        if (matchedUserId == null || matchedUsername == null) {
            Toast.makeText(this, "Error: No user data provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        Button skipButton = findViewById(R.id.skipButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button chatButton = findViewById(R.id.chatButton);
        ImageButton goBackButton = findViewById(R.id.goBackButton);

        usernameTextView.setText(matchedUsername);

        skipButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("action", "skip");
            resultIntent.putExtra("userId", matchedUserId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("recipientId", matchedUserId);
            intent.putExtra("otherUsername", matchedUsername);
            intent.putExtra("source", "UserProfileActivity");
            startActivity(intent);
            finish();
        });

        goBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}