package com.example.mediconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DoctorPersonalProfile extends AppCompatActivity {

    // ── Dark-mode prefs (doctor-side only) ────────────────────────────────────
    public static final String DOCTOR_PREFS_NAME = "doctor_prefs";
    public static final String DOCTOR_KEY_DARK_MODE = "doctor_dark_mode";

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView        tvInitials;
    private EditText        etFullName, etEmail, etSpecialization;
    private EditText        etClinicName, etLocation, etConsultationFee;
    private EditText        etConsultationHours, etAvailableDays, etPrcLicense;
    private MaterialButton  btnSaveProfile, btnChangePassword;
    private SwitchMaterial  switchDarkMode;
    private BottomNavigationView bottomNav;
    private MaterialButton btnLogout;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseAuth      mAuth;
    private DatabaseReference doctorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_personal_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        doctorRef = FirebaseDatabase.getInstance()
                .getReference("Doctors")
                .child(user.getUid());

        bindViews();
        loadProfile();
        setupDarkModeToggle();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmation());

        setupBottomNav();
    }

    // ─── Bind Views ───────────────────────────────────────────────────────────

    private void bindViews() {
        tvInitials          = findViewById(R.id.tvInitials);
        etFullName          = findViewById(R.id.etFullName);
        etEmail             = findViewById(R.id.etEmail);
        etSpecialization    = findViewById(R.id.etSpecialization);
        etClinicName        = findViewById(R.id.etClinicName);
        etLocation          = findViewById(R.id.etLocation);
        etConsultationFee   = findViewById(R.id.etConsultationFee);
        etConsultationHours = findViewById(R.id.etConsultationHours);
        etAvailableDays     = findViewById(R.id.etAvailableDays);
        etPrcLicense        = findViewById(R.id.etPrcLicense);
        btnSaveProfile      = findViewById(R.id.btnSaveProfile);
        btnChangePassword   = findViewById(R.id.btnChangePassword);
        switchDarkMode      = findViewById(R.id.switchDarkMode);
        bottomNav           = findViewById(R.id.bottomNav);
        btnLogout = findViewById(R.id.btnLogout);
    }

    // ─── Dark Mode ────────────────────────────────────────────────────────────

    private void setupDarkModeToggle() {
        SharedPreferences prefs = getSharedPreferences(DOCTOR_PREFS_NAME, MODE_PRIVATE);
        int savedMode = prefs.getInt(DOCTOR_KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Reflect current state on the switch (checked = dark mode ON)
        switchDarkMode.setChecked(savedMode == AppCompatDelegate.MODE_NIGHT_YES);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO;

            // Persist the preference using the doctor-specific key
            prefs.edit().putInt(DOCTOR_KEY_DARK_MODE, newMode).apply();

            // Apply immediately — all doctor activities will pick this up
            AppCompatDelegate.setDefaultNightMode(newMode);
        });
    }

    // ─── Load Profile ─────────────────────────────────────────────────────────

    private void loadProfile() {
        doctorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String fullName          = snapshot.child("fullName").getValue(String.class);
                String email             = snapshot.child("email").getValue(String.class);
                String specialization    = snapshot.child("specialization").getValue(String.class);
                String clinicName        = snapshot.child("clinicName").getValue(String.class);
                String location          = snapshot.child("location").getValue(String.class);
                String consultationFee   = snapshot.child("consultationFee").getValue(String.class);
                String consultationHours = snapshot.child("consultationHours").getValue(String.class);
                String availableDays     = snapshot.child("availableDays").getValue(String.class);
                String prcLicense        = snapshot.child("prcLicense").getValue(String.class);

                etFullName.setText(fullName != null ? fullName : "");
                etEmail.setText(email != null ? email : "");
                etSpecialization.setText(specialization != null ? specialization : "");
                etClinicName.setText(clinicName != null ? clinicName : "");
                etLocation.setText(location != null ? location : "");
                etConsultationFee.setText(consultationFee != null ? consultationFee : "");
                etConsultationHours.setText(consultationHours != null ? consultationHours : "");
                etAvailableDays.setText(availableDays != null ? availableDays : "");
                etPrcLicense.setText(prcLicense != null ? prcLicense : "");

                // Set avatar initials
                if (fullName != null && !fullName.isEmpty()) {
                    String cleaned = fullName.replace("Dr.", "").trim();
                    String[] parts = cleaned.split(" ");
                    String initials = "";
                    if (parts.length >= 1) initials += parts[0].charAt(0);
                    if (parts.length >= 2) initials += parts[parts.length - 1].charAt(0);
                    tvInitials.setText(initials.toUpperCase());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(DoctorPersonalProfile.this,
                        "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Save Profile ─────────────────────────────────────────────────────────

    private void saveProfile() {
        String fullName          = etFullName.getText().toString().trim();
        String specialization    = etSpecialization.getText().toString().trim();
        String clinicName        = etClinicName.getText().toString().trim();
        String location          = etLocation.getText().toString().trim();
        String consultationFee   = etConsultationFee.getText().toString().trim();
        String consultationHours = etConsultationHours.getText().toString().trim();
        String availableDays     = etAvailableDays.getText().toString().trim();
        String prcLicense        = etPrcLicense.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName",          fullName);
        updates.put("specialization",    specialization);
        updates.put("clinicName",        clinicName);
        updates.put("location",          location);
        updates.put("consultationFee",   consultationFee);
        updates.put("consultationHours", consultationHours);
        updates.put("availableDays",     availableDays);
        updates.put("prcLicense",        prcLicense);

        btnSaveProfile.setEnabled(false);

        doctorRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                    // Refresh initials
                    String cleaned = fullName.replace("Dr.", "").trim();
                    String[] parts = cleaned.split(" ");
                    String initials = "";
                    if (parts.length >= 1) initials += parts[0].charAt(0);
                    if (parts.length >= 2) initials += parts[parts.length - 1].charAt(0);
                    tvInitials.setText(initials.toUpperCase());
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ─── Change Password Dialog ───────────────────────────────────────────────

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_change_password, null);

        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword     = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String current = etCurrentPassword.getText().toString().trim();
                    String newPass  = etNewPassword.getText().toString().trim();
                    String confirm  = etConfirmPassword.getText().toString().trim();

                    if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                        Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPass.equals(confirm)) {
                        Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updatePassword(current, newPass);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider
                .getCredential(user.getEmail(), currentPassword);

        // Re-authenticate first, then update
        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(u ->
                                        Toast.makeText(this,
                                                "Password updated successfully!",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed to update password: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Current password is incorrect.",
                                Toast.LENGTH_SHORT).show());
    }

    // ─── Bottom Navigation ────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile)  return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Doctor_Dashboard.class));
                finish();
                return true;
            }
            if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_patients) {
                startActivity(new Intent(this, PatientListActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}