package com.example.mediconnect; // ← Change to your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    // Views
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        etEmail        = findViewById(R.id.etEmail);
        etPassword     = findViewById(R.id.etPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister     = findViewById(R.id.tvRegister);
        progressBar    = findViewById(R.id.progressBar);

        // Login button
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Navigate to Register
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Register.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user is already signed in, go directly to MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        }
    }

    // ─── Login Logic ────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Show loading state
        setLoading(true);

        // Firebase sign-in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(Login.this,
                                    "Welcome back, " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }
                    } else {
                        // Map Firebase errors to user-friendly messages
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Forgot Password ────────────────────────────────────────────────────────

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email first");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Login.this,
                                "Failed to send reset email. Check the address and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Signing in..." : "Sign In");
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, Dashboard.class);
        startActivity(intent);
    }

    /**
     * Converts Firebase exceptions into readable messages for the user.
     */
    private String getFirebaseErrorMessage(Exception exception) {
        if (exception == null) return "Login failed. Please try again.";

        String message = exception.getMessage();
        if (message == null) return "An unexpected error occurred.";

        if (message.contains("no user record") || message.contains("user-not-found")) {
            return "No account found with this email.";
        } else if (message.contains("password is invalid") || message.contains("wrong-password")) {
            return "Incorrect password. Please try again.";
        } else if (message.contains("badly formatted") || message.contains("invalid-email")) {
            return "Invalid email format.";
        } else if (message.contains("too many requests") || message.contains("blocked")) {
            return "Too many failed attempts. Account temporarily locked. Try later or reset your password.";
        } else if (message.contains("network")) {
            return "Network error. Please check your internet connection.";
        } else {
            return "Login failed: " + message;
        }
    }
}