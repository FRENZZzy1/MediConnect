package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Doctor_Dashboard extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView tvDoctorName;
    private TextView tvAppointmentCount, tvPendingCount, tvPatientCount;
    private TextView tvAvailabilityLabel, tvViewAll;
    private SwitchMaterial switchAvailability;
    private View viewStatusDot;
    private RecyclerView rvAppointments;
    private BottomNavigationView bottomNav;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;   // Realtime Database — for doctor profile
    private FirebaseFirestore firestore; // Firestore — for appointments

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        mAuth     = FirebaseAuth.getInstance();
        dbRef     = FirebaseDatabase.getInstance().getReference("Doctors");
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        loadDoctorName();      // ← loads firstname from Realtime DB
        loadDashboardStats();
        setupAvailabilityToggle();

        tvViewAll.setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, ScheduleActivity.class));
        });

        setupBottomNav();
    }

    // ─── Bind Views ─────────────────────────────────────────────────────────────

    private void bindViews() {
        tvDoctorName        = findViewById(R.id.tvDoctorName);
        tvAppointmentCount  = findViewById(R.id.tvAppointmentCount);
        tvPendingCount      = findViewById(R.id.tvPendingCount);
        tvPatientCount      = findViewById(R.id.tvPatientCount);
        tvAvailabilityLabel = findViewById(R.id.tvAvailabilityLabel);
        tvViewAll           = findViewById(R.id.tvViewAll);
        switchAvailability  = findViewById(R.id.switchAvailability);
        viewStatusDot       = findViewById(R.id.viewStatusDot);
        rvAppointments      = findViewById(R.id.rvAppointments);
        bottomNav           = findViewById(R.id.bottomNav);

        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setNestedScrollingEnabled(false);
    }

    // ─── Load Doctor Name from Realtime Database ─────────────────────────────────

    private void loadDoctorName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        dbRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get fullName field from Realtime Database
                    String fullName = snapshot.child("fullName").getValue(String.class);

                    if (fullName != null && !fullName.isEmpty()) {
                        // Extract first name only (e.g. "Dr. Sherilyn Paller" → "Sherilyn")
                        // fullName already has "Dr." prefix based on your DB — strip it first
                        String cleaned = fullName.replace("Dr.", "").trim();
                        String firstName = cleaned.split(" ")[0]; // get first word
                        tvDoctorName.setText("Dr. " + firstName);
                    } else {
                        tvDoctorName.setText("Doctor");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Doctor_Dashboard.this,
                        "Failed to load profile: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─── Load Stats from Firestore ───────────────────────────────────────────────

    private void loadDashboardStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Today's confirmed appointments
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(q -> tvAppointmentCount.setText(String.valueOf(q.size())));

        // Pending appointments
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(q -> tvPendingCount.setText(String.valueOf(q.size())));

        // Unique patients
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .get()
                .addOnSuccessListener(q -> {
                    java.util.Set<String> patientIds = new java.util.HashSet<>();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        String pid = doc.getString("patientId");
                        if (pid != null) patientIds.add(pid);
                    }
                    tvPatientCount.setText(String.valueOf(patientIds.size()));
                });
    }

    // ─── Availability Toggle ─────────────────────────────────────────────────────

    private void setupAvailabilityToggle() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Load current availability from Realtime DB
        dbRef.child(user.getUid()).child("isAvailable")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean isAvailable = snapshot.getValue(Boolean.class);
                        boolean available = isAvailable == null || isAvailable; // default true
                        updateAvailabilityUI(available);
                        switchAvailability.setChecked(available);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAvailabilityUI(isChecked);
            // Save to Realtime DB
            dbRef.child(user.getUid()).child("isAvailable").setValue(isChecked)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this,
                                    isChecked ? "Now accepting patients." : "Availability turned off.",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update.", Toast.LENGTH_SHORT).show());
        });
    }

    private void updateAvailabilityUI(boolean isAvailable) {
        if (isAvailable) {
            tvAvailabilityLabel.setText("Accepting new patients");
            viewStatusDot.setBackgroundResource(R.drawable.circle_status_active);
        } else {
            tvAvailabilityLabel.setText("Not accepting new patients");
            viewStatusDot.setBackgroundResource(R.drawable.circle_status_inactive);
        }
    }

    // ─── Bottom Navigation ───────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_schedule) {
                // TODO: startActivity(new Intent(this, ScheduleActivity.class));
                return true;
            } else if (id == R.id.nav_patients) {
                // TODO: startActivity(new Intent(this, PatientsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                // TODO: startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }
}