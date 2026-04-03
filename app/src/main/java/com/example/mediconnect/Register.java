package com.example.mediconnect;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class Register extends AppCompatActivity {

    // ── Common fields ──
    TextInputEditText etFirstName, etLastName, etEmail, etPhone, etPassword, etDOB;
    AutoCompleteTextView spinnerSex;
    TextInputLayout tilDOB, tilSex;

    // ── Doctor-only fields ──
    TextInputEditText etPrcLicense, etClinicName, etLocation, etConsultationHours, etConsultationFee;
    AutoCompleteTextView spinnerSpecialization, spinnerAvailableDays;
    LinearLayout layoutDoctorFields;

    // ── Role toggle & header label ──
    MaterialButton btnRolePatient, btnRoleDoctor, btnRegister;
    TextView tvRolePillLabel;

    // ── Loading ──
    ProgressBar progressBar;

    // ── State ──
    boolean isDoctor = false;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        // ── Bind common views ──
        etFirstName  = findViewById(R.id.etFirstName);
        etLastName   = findViewById(R.id.etLastName);
        etEmail      = findViewById(R.id.etEmail);
        etPhone      = findViewById(R.id.etPhone);
        etPassword   = findViewById(R.id.etPassword);
        etDOB        = findViewById(R.id.etDOB);
        spinnerSex   = findViewById(R.id.spinnerSex);
        tilDOB       = findViewById(R.id.tilDOB);
        tilSex       = findViewById(R.id.tilSex);
        progressBar  = findViewById(R.id.progressBar);
        btnRegister  = findViewById(R.id.btnRegister);

        // ── Bind doctor-only views ──
        layoutDoctorFields    = findViewById(R.id.layoutDoctorFields);
        etPrcLicense          = findViewById(R.id.etPrcLicense);
        spinnerSpecialization = findViewById(R.id.spinnerSpecialization);
        etClinicName          = findViewById(R.id.etClinicName);
        etLocation            = findViewById(R.id.etLocation);
        spinnerAvailableDays  = findViewById(R.id.spinnerAvailableDays);
        etConsultationHours   = findViewById(R.id.etConsultationHours);
        etConsultationFee     = findViewById(R.id.etConsultationFee);

        // ── Bind toggle & header ──
        btnRolePatient  = findViewById(R.id.btnRolePatient);
        btnRoleDoctor   = findViewById(R.id.btnRoleDoctor);
        tvRolePillLabel = findViewById(R.id.tvRolePillLabel);

        // ── Date picker ──
        etDOB.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                    etDOB.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ── Sex dropdown ──
        String[] sexOptions = {"Male", "Female", "Other"};
        spinnerSex.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, sexOptions));
        spinnerSex.setOnClickListener(v -> spinnerSex.showDropDown());

        // ── Specialization dropdown ──
        String[] specializations = {
                "General Practitioner", "Cardiologist", "Dermatologist",
                "Endocrinologist", "Gastroenterologist", "Neurologist",
                "Obstetrician / Gynecologist", "Ophthalmologist", "Orthopedic Surgeon",
                "Pediatrician", "Psychiatrist", "Pulmonologist",
                "Radiologist", "Surgeon", "Urologist", "Other"
        };
        spinnerSpecialization.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, specializations));
        spinnerSpecialization.setOnClickListener(v -> spinnerSpecialization.showDropDown());

        // ── Available Days dropdown ──
        String[] availableDayOptions = {
                "Monday – Friday",
                "Monday, Wednesday, Friday",
                "Tuesday, Thursday",
                "Saturday & Sunday",
                "Weekdays & Saturday",
                "By Appointment Only"
        };
        spinnerAvailableDays.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, availableDayOptions));
        spinnerAvailableDays.setOnClickListener(v -> spinnerAvailableDays.showDropDown());

        // ── Role toggle ──
        btnRolePatient.setOnClickListener(v -> switchRole(false));
        btnRoleDoctor.setOnClickListener(v -> switchRole(true));

        // ── Register button ──
        btnRegister.setOnClickListener(v -> registerUser());

        // ── Edge-to-edge padding ──
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ── Toggle between Patient and Doctor mode ────────────────────────────────

    private void switchRole(boolean doctorMode) {
        isDoctor = doctorMode;

        if (doctorMode) {
            btnRoleDoctor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0891B2")));
            btnRoleDoctor.setTextColor(Color.WHITE);
            btnRolePatient.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2F8")));
            btnRolePatient.setTextColor(Color.parseColor("#0891B2"));

            layoutDoctorFields.setVisibility(View.VISIBLE);
            tilDOB.setVisibility(View.GONE);
            tilSex.setVisibility(View.GONE);
            tvRolePillLabel.setText("👨‍⚕️  DOCTOR REGISTRATION");
        } else {
            btnRolePatient.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0891B2")));
            btnRolePatient.setTextColor(Color.WHITE);
            btnRoleDoctor.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0F2F8")));
            btnRoleDoctor.setTextColor(Color.parseColor("#0891B2"));

            layoutDoctorFields.setVisibility(View.GONE);
            tilDOB.setVisibility(View.VISIBLE);
            tilSex.setVisibility(View.VISIBLE);
            tvRolePillLabel.setText("🧑  PATIENT REGISTRATION");
        }
    }

    // ── Validate then create Firebase Auth account ────────────────────────────

    private void registerUser() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName  = etLastName.getText().toString().trim();
        String email     = etEmail.getText().toString().trim();
        String password  = etPassword.getText().toString().trim();
        String phone     = etPhone.getText().toString().trim();

        // ── Validation ──
        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }
        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (isDoctor) {
            if (etPrcLicense.getText().toString().trim().isEmpty()) {
                etPrcLicense.setError("PRC License Number is required");
                etPrcLicense.requestFocus();
                return;
            }
            if (spinnerSpecialization.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please select a specialization", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (etDOB.getText().toString().trim().isEmpty()) {
                etDOB.setError("Date of birth is required");
                etDOB.requestFocus();
                return;
            }
            if (spinnerSex.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please select your sex", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    saveData(userId, firstName, lastName, email, phone);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── Save to the correct Realtime Database node ────────────────────────────

    private void saveData(String userId, String firstName, String lastName,
                          String email, String phone) {

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        if (isDoctor) {
            String fullName          = "Dr. " + firstName + " " + lastName;
            String prcLicense        = etPrcLicense.getText().toString().trim();
            String specialization    = spinnerSpecialization.getText().toString().trim();
            String clinicName        = etClinicName.getText().toString().trim();
            String location          = etLocation.getText().toString().trim();
            String availableDays     = spinnerAvailableDays.getText().toString().trim();
            String consultationHours = etConsultationHours.getText().toString().trim();
            String consultationFee   = etConsultationFee.getText().toString().trim();

            Doctor doctor = new Doctor(
                    fullName, email, prcLicense, specialization,
                    availableDays, consultationHours, consultationFee,
                    clinicName, location,
                    true  // isAvailable default = true
            );

            dbRef.child("Doctors").child(userId).setValue(doctor)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(this, "Doctor account created!", Toast.LENGTH_SHORT).show();
                        // ── Go to Doctor Dashboard ──
                        Intent intent = new Intent(this, Doctor_Dashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            String dob = etDOB.getText().toString().trim();
            String sex = spinnerSex.getText().toString().trim();

            User user = new User(firstName, lastName, email, phone, dob, sex, "");

            dbRef.child("Users").child(userId).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(this, "Patient account created!", Toast.LENGTH_SHORT).show();
                        // ── Go to Patient Dashboard ──
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ── Loading state ─────────────────────────────────────────────────────────

    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Creating account..." : "Create Account");
    }

    // ── Back to login ─────────────────────────────────────────────────────────

    public void SplashAct(View view) {
        finish(); // just go back to Login
    }
}