package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AppointmentsFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView appointmentsRecyclerView;
    private TextView emptyStateText;
    private AppointmentsAdapter appointmentsAdapter;
    private List<Appointment> appointmentList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "upcoming"; // upcoming, past, cancelled

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        setupTabLayout();
        setupRecyclerView();
        loadAppointments();

        return view;
    }

    private void initializeViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        appointmentsRecyclerView = view.findViewById(R.id.appointments_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        tabLayout.addTab(tabLayout.newTab().setText("Past"));
        tabLayout.addTab(tabLayout.newTab().setText("Cancelled"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "upcoming";
                        break;
                    case 1:
                        currentFilter = "past";
                        break;
                    case 2:
                        currentFilter = "cancelled";
                        break;
                }
                loadAppointments();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        appointmentList = new ArrayList<>();
        appointmentsAdapter = new AppointmentsAdapter(appointmentList, appointment -> {
            // Click handler - open detail
            Intent intent = new Intent(getActivity(), AppointmentDetailActivity.class);
            intent.putExtra("appointment_id", appointment.getAppointmentId());
            intent.putExtra("doctor_name", appointment.getDoctorName());
            intent.putExtra("doctor_specialty", appointment.getDoctorSpecialty());
            intent.putExtra("date", appointment.getDate());
            intent.putExtra("time", appointment.getTime());
            intent.putExtra("type", appointment.getType());
            intent.putExtra("status", appointment.getStatus());
            intent.putExtra("notes", appointment.getNotes());
            intent.putExtra("hospital", appointment.getDoctorHospital());
            startActivity(intent);
        });

        appointmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appointmentsRecyclerView.setAdapter(appointmentsAdapter);
    }

    private void loadAppointments() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String patientId = currentUser.getUid();

        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    appointmentList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Appointment appointment = document.toObject(Appointment.class);
                        appointment.setAppointmentId(document.getId());

                        // Filter based on tab
                        boolean shouldAdd = false;
                        if (currentFilter.equals("upcoming") && appointment.isUpcoming()) {
                            shouldAdd = true;
                        } else if (currentFilter.equals("past") && !appointment.isUpcoming()
                                && !appointment.getStatus().equals("cancelled")) {
                            shouldAdd = true;
                        } else if (currentFilter.equals("cancelled")
                                && appointment.getStatus().equals("cancelled")) {
                            shouldAdd = true;
                        }

                        if (shouldAdd) {
                            // Fetch doctor details
                            fetchDoctorDetails(appointment);
                        }
                    }

                    if (appointmentList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    showEmptyState();
                });
    }

    private void fetchDoctorDetails(Appointment appointment) {
        db.collection("doctors").document(appointment.getDoctorId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        appointment.setDoctorName(documentSnapshot.getString("name"));
                        appointment.setDoctorSpecialty(documentSnapshot.getString("specialty"));
                        appointment.setDoctorHospital(documentSnapshot.getString("hospital"));
                    }
                    appointmentList.add(appointment);
                    appointmentsAdapter.notifyDataSetChanged();

                    if (!appointmentList.isEmpty()) {
                        hideEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    appointmentList.add(appointment);
                    appointmentsAdapter.notifyDataSetChanged();
                });
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        appointmentsRecyclerView.setVisibility(View.GONE);

        String message;
        switch (currentFilter) {
            case "upcoming":
                message = "No upcoming appointments\nBook a doctor to get started";
                break;
            case "past":
                message = "No past appointments";
                break;
            case "cancelled":
                message = "No cancelled appointments";
                break;
            default:
                message = "No appointments found";
        }
        emptyStateText.setText(message);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        appointmentsRecyclerView.setVisibility(View.VISIBLE);
    }
}