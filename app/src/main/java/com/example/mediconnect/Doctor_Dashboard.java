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
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Doctor_Dashboard extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView tvDoctorName;
    private TextView tvAppointmentCount, tvPendingCount, tvPatientCount;
    private TextView tvAvailabilityLabel, tvViewAll;
    private SwitchMaterial switchAvailability;
    private View viewStatusDot;
    private View layoutEmptySchedule;
    private RecyclerView rvAppointments;
    private BottomNavigationView bottomNav;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestore;

    // ── Adapter ────────────────────────────────────────────────────────────────
    private AppointmentScheduleAdapter scheduleAdapter;
    private final List<AppointmentItem> scheduleList = new ArrayList<>();

    // ── Today's date string (must match Firestore format) ──────────────────────
    // Change pattern to match whatever format you store in Firestore, e.g. "yyyy-MM-dd"
    private final String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_dashboard);

        mAuth     = FirebaseAuth.getInstance();
        dbRef     = FirebaseDatabase.getInstance().getReference("Doctors");
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        loadDoctorName();
        loadDashboardStats();
        loadTodaySchedule();
        setupAvailabilityToggle();

        tvViewAll.setOnClickListener(v -> {
            // TODO: startActivity(new Intent(this, ScheduleActivity.class));
        });

        setupBottomNav();
    }

    // ─── Bind Views ──────────────────────────────────────────────────────────────

    private void bindViews() {
        tvDoctorName        = findViewById(R.id.tvDoctorName);
        tvAppointmentCount  = findViewById(R.id.tvAppointmentCount);
        tvPendingCount      = findViewById(R.id.tvPendingCount);
        tvPatientCount      = findViewById(R.id.tvPatientCount);
        tvAvailabilityLabel = findViewById(R.id.tvAvailabilityLabel);
        tvViewAll           = findViewById(R.id.tvViewAll);
        switchAvailability  = findViewById(R.id.switchAvailability);
        viewStatusDot        = findViewById(R.id.viewStatusDot);
        layoutEmptySchedule  = findViewById(R.id.layoutEmptySchedule);
        rvAppointments       = findViewById(R.id.rvAppointments);
        bottomNav            = findViewById(R.id.bottomNav);

        // Set up RecyclerView with adapter
        scheduleAdapter = new AppointmentScheduleAdapter(scheduleList);
        rvAppointments.setLayoutManager(new LinearLayoutManager(this));
        rvAppointments.setAdapter(scheduleAdapter);
        rvAppointments.setNestedScrollingEnabled(false);
    }

    // ─── Load Doctor Name ────────────────────────────────────────────────────────

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
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    if (fullName != null && !fullName.isEmpty()) {
                        String cleaned   = fullName.replace("Dr.", "").trim();
                        String firstName = cleaned.split(" ")[0];
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

    // ─── Load Dashboard Stats ────────────────────────────────────────────────────

    private void loadDashboardStats() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // ── Card 1: Today's confirmed appointments ──────────────────────────────
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("date", todayDate)          // filter by today
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(q ->
                        tvAppointmentCount.setText(String.valueOf(q.size())))
                .addOnFailureListener(e ->
                        tvAppointmentCount.setText("0"));

        // ── Card 2: Pending appointments (all time, or filter today if preferred) ─
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(q ->
                        tvPendingCount.setText(String.valueOf(q.size())))
                .addOnFailureListener(e ->
                        tvPendingCount.setText("0"));

        // ── Card 3: Unique patients (all time) ──────────────────────────────────
        firestore.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .get()
                .addOnSuccessListener(q -> {
                    Set<String> patientIds = new HashSet<>();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        String pid = doc.getString("patientId");
                        if (pid != null) patientIds.add(pid);
                    }
                    tvPatientCount.setText(String.valueOf(patientIds.size()));
                })
                .addOnFailureListener(e ->
                        tvPatientCount.setText("0"));
    }

    // ─── Load Today's Schedule ───────────────────────────────────────────────────

    private void loadTodaySchedule() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        firestore.collection("appointments")
                .whereEqualTo("doctorId", user.getUid())
                .whereEqualTo("date", todayDate)
                .orderBy("time", Query.Direction.ASCENDING)   // sort by time field
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    scheduleList.clear();

                    if (querySnapshot.isEmpty()) {
                        layoutEmptySchedule.setVisibility(View.VISIBLE);
                        rvAppointments.setVisibility(View.GONE);
                        return;
                    }

                    layoutEmptySchedule.setVisibility(View.GONE);
                    rvAppointments.setVisibility(View.VISIBLE);

                    // Sa loadTodaySchedule() method
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        AppointmentItem item = new AppointmentItem();
                        item.appointmentId = doc.getString("appointmentId");
                        item.patientId = doc.getString("patientId");
                        item.patientName = doc.getString("patientName");  // ← KUNIN DIN
                        item.time = doc.getString("time");
                        item.type = doc.getString("type");
                        item.status = doc.getString("status");
                        item.notes = doc.getString("notes");
                        item.date = doc.getString("date");
                        scheduleList.add(item);
                    }
                    scheduleAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load schedule: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─── Availability Toggle ─────────────────────────────────────────────────────

    private void setupAvailabilityToggle() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        dbRef.child(user.getUid()).child("isAvailable")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean isAvailable = snapshot.getValue(Boolean.class);
                        boolean available = isAvailable == null || isAvailable;
                        updateAvailabilityUI(available);
                        switchAvailability.setChecked(available);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateAvailabilityUI(isChecked);
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
            if (id == R.id.nav_home)     return true;
            if (id == R.id.nav_schedule) { /* TODO */ return true; }
            if (id == R.id.nav_patients) { /* TODO */ return true; }
            if (id == R.id.nav_profile)  { /* TODO */ return true; }
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