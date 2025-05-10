package com.example.mecore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(this, GameSelectingActivity.class));
            finish();
            return;
        }

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.buttonLogin);
        TextView registerTextView = findViewById(R.id.textRegister);
        TextView forgotPasswordTextView = findViewById(R.id.textResetPassword);

        registerTextView.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        forgotPasswordTextView.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                startActivity(new Intent(this, GameSelectingActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            Toast.makeText(this, "Login failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
