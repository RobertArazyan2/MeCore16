package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText mUsername, mEmail, mPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Redirect if user is already logged in
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(RegisterActivity.this, GameSelectingActivity.class));
            finish();
        }

        mUsername = findViewById(R.id.editTextUsername);
        mEmail = findViewById(R.id.editTextEmail);
        mPassword = findViewById(R.id.editTextPassword);
        Button mRegisterBtn = findViewById(R.id.buttonRegister);
        TextView loginTextView = findViewById(R.id.textLogin);
        TextView forgotPasswordTextView = findViewById(R.id.textResetPassword);

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        forgotPasswordTextView.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, ForgotPasswordActivity.class)));

        mRegisterBtn.setOnClickListener(v -> {
            String username = mUsername.getText().toString().trim();
            String email = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.length() < 3) {
                Toast.makeText(RegisterActivity.this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user with Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();

                                // Store user data in Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("username", username);
                                userData.put("email", email);
                                List<String> selectedGames = new ArrayList<>();
                                userData.put("selectedGames", selectedGames);

                                Map<String, Object> usernameData = new HashMap<>();
                                usernameData.put("userId", userId);

                                db.runBatch(batch -> {
                                    batch.set(db.collection("users").document(userId), userData);
                                    batch.set(db.collection("usernames").document(username), usernameData);
                                }).addOnSuccessListener(aVoid -> {
                                    Log.d("Register", "User data saved successfully!");

                                    // Send verification email
                                    user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                        if (sendTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Verification email sent. Please verify before logging in.", Toast.LENGTH_LONG).show();

                                            // Sign out user so they must verify before logging in
                                            FirebaseAuth.getInstance().signOut();

                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                }).addOnFailureListener(e -> {
                                    Log.e("Register", "Error saving user data", e);
                                    user.delete(); // Delete user from Auth if Firestore write fails
                                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
