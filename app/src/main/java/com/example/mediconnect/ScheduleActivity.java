package com.example.mediconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ScheduleActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView rvSchedule;
    private View         layoutEmpty;
    private TextView     tvSelectedDate, tvTotalCount;

    // ── Tab date buttons ───────────────────────────────────────────────────────
    private TextView tabYesterday, tabToday, tabTomorrow, tabCustom;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore firestore;
    private FirebaseAuth      mAuth;

    // ── Adapter ────────────────────────────────────────────────────────────────
    private ScheduleAdapter          adapter;
    private final List<AppointmentItem> appointmentList = new ArrayList<>();

    // ── Date helpers ───────────────────────────────────────────────────────────
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displaySdf =
            new SimpleDateFormat("EEEE, MMMM d yyyy", Locale.getDefault());

    private String selectedDate;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        mAuth     = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupTabs();

        // Default: today
        selectTab(tabToday, sdf.format(new Date()));
        setupBottomNav();
    }

    // ── Bind ──────────────────────────────────────────────────────────────────

    private void bindViews() {
        rvSchedule    = findViewById(R.id.rvSchedule);
        layoutEmpty   = findViewById(R.id.layoutEmpty);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvTotalCount  = findViewById(R.id.tvTotalCount);

        tabYesterday  = findViewById(R.id.tabYesterday);
        tabToday      = findViewById(R.id.tabToday);
        tabTomorrow   = findViewById(R.id.tabTomorrow);
        tabCustom     = findViewById(R.id.tabCustom);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // RecyclerView
        adapter = new ScheduleAdapter(appointmentList);
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        rvSchedule.setAdapter(adapter);
        rvSchedule.setNestedScrollingEnabled(false);
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    private void setupTabs() {
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = sdf.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, 1);
        String today = sdf.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = sdf.format(cal.getTime());

        tabYesterday.setOnClickListener(v -> selectTab(tabYesterday, yesterday));
        tabToday.setOnClickListener(v    -> selectTab(tabToday,     today));
        tabTomorrow.setOnClickListener(v -> selectTab(tabTomorrow,  tomorrow));

        // "Custom" — open a DatePickerDialog
        tabCustom.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new android.app.DatePickerDialog(this,
                    (dp, year, month, day) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day);
                        selectTab(tabCustom, sdf.format(picked.getTime()));
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void selectTab(TextView selected, String date) {
        // Reset all tab styles
        int inactive = getResources().getColor(R.color.tab_inactive_bg, null);
        int inactiveTxt = getResources().getColor(R.color.tab_inactive_text, null);
        int active  = getResources().getColor(R.color.teal_primary, null);
        int activeTxt = getResources().getColor(android.R.color.white, null);

        for (TextView tab : new TextView[]{tabYesterday, tabToday, tabTomorrow, tabCustom}) {
            tab.setBackgroundResource(R.drawable.tab_inactive_bg);
            tab.setTextColor(inactiveTxt);
        }
        selected.setBackgroundResource(R.drawable.tab_active_bg);
        selected.setTextColor(activeTxt);

        // Load data
        selectedDate = date;
        try {
            Date parsed = sdf.parse(date);
            tvSelectedDate.setText(parsed != null ? displaySdf.format(parsed) : date);
        } catch (Exception e) {
            tvSelectedDate.setText(date);
        }

        loadSchedule(date);
    }

    // ── Firestore Query ───────────────────────────────────────────────────────

    private void loadSchedule(String date) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Show loading state
        tvTotalCount.setText("Loading…");
        layoutEmpty.setVisibility(View.GONE);
        rvSchedule.setVisibility(View.GONE);

        firestore.collection("appointments")
                .whereEqualTo("doctorId", user.getUid())
                .whereEqualTo("date",     date)
                .whereEqualTo("status",   "confirmed")
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    appointmentList.clear();

                    if (snapshot.isEmpty()) {
                        tvTotalCount.setText("0 appointments");
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvSchedule.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        AppointmentItem item = new AppointmentItem();
                        item.appointmentId = doc.getString("appointmentId");
                        item.patientId     = doc.getString("patientId");
                        item.patientName   = doc.getString("patientName");
                        item.time          = doc.getString("time");
                        item.type          = doc.getString("type");
                        item.status        = doc.getString("status");
                        item.notes         = doc.getString("notes");
                        item.date          = doc.getString("date");
                        appointmentList.add(item);
                    }

                    tvTotalCount.setText(appointmentList.size() + " confirmed");
                    layoutEmpty.setVisibility(View.GONE);
                    rvSchedule.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvTotalCount.setText("Error");
                    Toast.makeText(this,
                            "Failed to load schedule: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_schedule); // highlight Schedule tab
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile)  {
                startActivity(new Intent(this, DoctorPersonalProfile.class));
                return true;}
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

                finish();
                return true;
            }
            return false;
        });
    }




}