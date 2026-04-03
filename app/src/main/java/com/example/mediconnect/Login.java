package com.example.mediconnect;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {

    // Views
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;  // ← Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        // ← Points to "Doctors" node in Realtime Database
        dbRef = FirebaseDatabase.getInstance().getReference("Doctors");

        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister       = findViewById(R.id.tvRegister);
        progressBar      = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, Register.class));
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            setLoading(true);
            navigateByRole(currentUser.getUid());
        }
    }

    // ─── Login Logic ────────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

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

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(Login.this,
                                    "Welcome back, " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                            navigateByRole(user.getUid());
                        }
                    } else {
                        setLoading(false);
                        String errorMessage = getFirebaseErrorMessage(task.getException());
                        Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Role Check using Realtime Database ─────────────────────────────────────

    private void navigateByRole(String uid) {
        dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    // ── DOCTOR ──
                    startActivity(new Intent(Login.this, Doctor_Dashboard.class));
                } else {
                    // ── PATIENT ──
                    startActivity(new Intent(Login.this, DashboardActivity.class));
                }
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                setLoading(false);
                Toast.makeText(Login.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
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