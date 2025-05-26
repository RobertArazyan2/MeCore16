package com.MeCore.mecore;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText mUsername, mEmail, mPassword;
    private Button mRegisterBtn;
    private TextView loginTextView, forgotPasswordTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(RegisterActivity.this, GameSelectingActivity.class));
            finish();
            return;
        }

        // Initialize Views
        mUsername = findViewById(R.id.editTextUsername);
        mEmail = findViewById(R.id.editTextEmail);
        mPassword = findViewById(R.id.editTextPassword);
        mRegisterBtn = findViewById(R.id.buttonRegister);
        loginTextView = findViewById(R.id.textLogin);
        forgotPasswordTextView = findViewById(R.id.textResetPassword);

        loginTextView.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        forgotPasswordTextView.setOnClickListener(v ->
                startActivity(new Intent(RegisterActivity.this, ForgotPasswordActivity.class))
        );

        mRegisterBtn.setOnClickListener(v -> {
            String username = mUsername.getText().toString().trim();
            String email = mEmail.getText().toString().trim();
            String password = mPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.length() < 3) {
                Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if username already exists
            db.collection("usernames").document(username).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                        } else {
                            registerUser(username, email, password);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error checking username: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", username);
                            userData.put("email", email);
                            userData.put("status", "online");
                            userData.put("lookingForGame", false);
                            userData.put("selectedGames", new ArrayList<String>());

                            Map<String, Object> usernameData = new HashMap<>();
                            usernameData.put("userId", userId);

                            db.runBatch(batch -> {
                                batch.set(db.collection("users").document(userId), userData);
                                batch.set(db.collection("usernames").document(username), usernameData);
                            }).addOnSuccessListener(aVoid -> {
                                user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                    if (sendTask.isSuccessful()) {
                                        Toast.makeText(this, "Verification email sent. Please verify before logging in.", Toast.LENGTH_LONG).show();
                                        FirebaseAuth.getInstance().signOut();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }).addOnFailureListener(e -> {
                                user.delete(); // delete incomplete user
                                Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
