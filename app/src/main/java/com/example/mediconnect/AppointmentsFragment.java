package com.example.mediconnect;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView tabUpcoming, tabPast, tabCancelled;
    private RecyclerView recyclerView;
    private View loadingSpinner;
    private LinearLayout emptyStateContainer;
    private TextView tvEmptyTitle, tvEmptySub;

    // ── Data ───────────────────────────────────────────────────────────────────
    private AppointmentsAdapter adapter;
    private final List<Appointment> appointmentList = new ArrayList<>();
    private String currentFilter = "upcoming";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bindViews(view);
        setupRecyclerView();
        setupSegmentedTabs();
        loadAppointments();
        return view;
    }

    private void bindViews(View view) {
        tabUpcoming        = view.findViewById(R.id.tab_upcoming);
        tabPast            = view.findViewById(R.id.tab_past);
        tabCancelled       = view.findViewById(R.id.tab_cancelled);
        recyclerView       = view.findViewById(R.id.appointments_recycler_view);
        loadingSpinner     = view.findViewById(R.id.appointments_loading);
        emptyStateContainer= view.findViewById(R.id.empty_state_container);
        tvEmptyTitle       = view.findViewById(R.id.empty_state_text);
        tvEmptySub         = view.findViewById(R.id.empty_state_sub);
    }

    // ── Custom segmented tab logic ─────────────────────────────────────────────

    private void setupSegmentedTabs() {
        // Set initial active state
        setTabActive(tabUpcoming);
        setTabInactive(tabPast);
        setTabInactive(tabCancelled);

        tabUpcoming.setOnClickListener(v -> selectTab("upcoming"));
        tabPast.setOnClickListener(    v -> selectTab("past"));
        tabCancelled.setOnClickListener(v -> selectTab("cancelled"));
    }

    private void selectTab(String filter) {
        currentFilter = filter;
        setTabInactive(tabUpcoming);
        setTabInactive(tabPast);
        setTabInactive(tabCancelled);

        switch (filter) {
            case "upcoming":  setTabActive(tabUpcoming);  break;
            case "past":      setTabActive(tabPast);      break;
            case "cancelled": setTabActive(tabCancelled); break;
        }
        loadAppointments();
    }

    private void setTabActive(TextView tab) {
        if (tab == null || !isAdded()) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(8));
        bg.setColor(0xFFFFFFFF);   // white pill on the teal header
        tab.setBackground(bg);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_teal_dark));
    }

    private void setTabInactive(TextView tab) {
        if (tab == null || !isAdded()) return;
        tab.setBackground(null);
        tab.setTextColor(0xCCFFFFFF);  // semi-transparent white
    }

    private float dpToPx(int dp) {
        return dp * requireContext().getResources().getDisplayMetrics().density;
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new AppointmentsAdapter(appointmentList, appt -> {
            Intent i = new Intent(getActivity(), AppointmentDetailActivity.class);
            i.putExtra("appointment_id",   appt.getAppointmentId());
            i.putExtra("doctor_name",      appt.getDoctorName());
            i.putExtra("doctor_specialty", appt.getDoctorSpecialty());
            i.putExtra("date",             appt.getDate());
            i.putExtra("time",             appt.getTime());
            i.putExtra("type",             appt.getType());
            i.putExtra("status",           appt.getStatus());
            i.putExtra("notes",            appt.getNotes());
            i.putExtra("hospital",         appt.getDoctorHospital());
            startActivity(i);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // ── Firestore load ─────────────────────────────────────────────────────────

    private void loadAppointments() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { showEmpty(); return; }

        showLoading();

        db.collection("appointments")
            .whereEqualTo("patientId", user.getUid())
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!isAdded()) return;
                appointmentList.clear();
                List<Appointment> filtered = new ArrayList<>();

                for (QueryDocumentSnapshot doc : snapshots) {
                    Appointment appt = doc.toObject(Appointment.class);
                    appt.setAppointmentId(doc.getId());

                    boolean add = false;
                    switch (currentFilter) {
                        case "upcoming":
                            add = appt.isUpcoming();
                            break;
                        case "past":
                            add = !appt.isUpcoming()
                                    && !"cancelled".equalsIgnoreCase(appt.getStatus());
                            break;
                        case "cancelled":
                            add = "cancelled".equalsIgnoreCase(appt.getStatus());
                            break;
                    }
                    if (add) filtered.add(appt);
                }

                if (filtered.isEmpty()) { showEmpty(); return; }

                // Fetch doctor names for each, then render
                final int[] pending = {filtered.size()};
                for (Appointment appt : filtered) {
                    String doctorId = appt.getDoctorId();
                    if (doctorId == null || doctorId.isEmpty()) {
                        appointmentList.add(appt);
                        pending[0]--;
                        if (pending[0] == 0) finishLoading();
                        continue;
                    }
                    FirebaseDatabase.getInstance()
                        .getReference("Doctors").child(doctorId).get()
                        .addOnCompleteListener(task -> {
                            if (!isAdded()) return;
                            if (task.isSuccessful() && task.getResult() != null
                                    && task.getResult().exists()) {
                                appt.setDoctorName(
                                        task.getResult().child("fullName").getValue(String.class));
                                appt.setDoctorSpecialty(
                                        task.getResult().child("specialization").getValue(String.class));
                                appt.setDoctorHospital(
                                        task.getResult().child("clinicName").getValue(String.class));
                            }
                            appointmentList.add(appt);
                            pending[0]--;
                            if (pending[0] == 0) finishLoading();
                        });
                }
            })
            .addOnFailureListener(e -> { if (isAdded()) showEmpty(); });
    }

    private void finishLoading() {
        if (!isAdded()) return;
        hideLoading();
        if (appointmentList.isEmpty()) { showEmpty(); return; }
        emptyStateContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    // ── State helpers ──────────────────────────────────────────────────────────

    private void showLoading() {
        loadingSpinner.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingSpinner.setVisibility(View.GONE);
    }

    private void showEmpty() {
        hideLoading();
        recyclerView.setVisibility(View.GONE);
        emptyStateContainer.setVisibility(View.VISIBLE);
        switch (currentFilter) {
            case "upcoming":
                tvEmptyTitle.setText("No upcoming appointments");
                tvEmptySub.setText("Book a doctor to get started");
                break;
            case "past":
                tvEmptyTitle.setText("No past appointments");
                tvEmptySub.setText("Your completed visits will appear here");
                break;
            case "cancelled":
                tvEmptyTitle.setText("No cancelled appointments");
                tvEmptySub.setText("You have no cancelled bookings");
                break;
        }
    }
}
