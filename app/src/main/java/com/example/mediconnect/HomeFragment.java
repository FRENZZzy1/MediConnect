package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvUserName, tvUserAvatar, tvGreeting;
    private CardView actionVideoConsult, actionAIChat, actionFindNearby, actionMyRecords;
    private CardView appointmentCard, appointmentEmptyCard;
    private View     appointmentLoading;
    private TextView tvApptType, tvApptStatusText, tvApptDoctor, tvApptSpecialty, tvApptTime;
    private View     tvApptStatusDot;
    private MaterialButton joinButton, btnBookNow;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        bindViews(view);
        loadUserData();
        loadNextAppointment();
        setupClickListeners();
        return view;
    }

    private void bindViews(View view) {
        tvUserName           = view.findViewById(R.id.user_name);
        tvUserAvatar         = view.findViewById(R.id.user_avatar);
        tvGreeting           = view.findViewById(R.id.greeting_text);
        actionVideoConsult   = view.findViewById(R.id.action_video_consult);
        actionAIChat         = view.findViewById(R.id.action_ai_chat);
        actionFindNearby     = view.findViewById(R.id.action_find_nearby);
        actionMyRecords      = view.findViewById(R.id.action_my_records);
        appointmentCard      = view.findViewById(R.id.appointment_card);
        appointmentEmptyCard = view.findViewById(R.id.appointment_empty_card);
        appointmentLoading   = view.findViewById(R.id.appointment_loading);
        tvApptType           = view.findViewById(R.id.appt_type_badge);
        tvApptStatusText     = view.findViewById(R.id.appt_status_text);
        tvApptStatusDot      = view.findViewById(R.id.appt_status_dot);
        tvApptDoctor         = view.findViewById(R.id.appointment_doctor);
        tvApptSpecialty      = view.findViewById(R.id.appointment_specialty);
        tvApptTime           = view.findViewById(R.id.appointment_time);
        joinButton           = view.findViewById(R.id.join_button);
        btnBookNow           = view.findViewById(R.id.btn_book_now);
    }

    // ── Greeting + user name ──────────────────────────────────────────────────

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        int hour     = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Good morning,"
                     : hour < 18 ? "Good afternoon,"
                     : "Good evening,";
        tvGreeting.setText(greet);

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            setNameAndAvatar(user.getDisplayName());
        } else {
            FirebaseDatabase.getInstance()
                .getReference("Users").child(user.getUid()).get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    String first = snap.child("firstName").getValue(String.class);
                    String last  = snap.child("lastName").getValue(String.class);
                    String name  = ((first != null ? first : "") + " "
                                  + (last  != null ? last  : "")).trim();
                    if (name.isEmpty() && user.getEmail() != null)
                        name = user.getEmail().split("@")[0];
                    setNameAndAvatar(name.isEmpty() ? "User" : name);
                });
        }
    }

    private void setNameAndAvatar(String name) {
        if (!isAdded()) return;
        tvUserName.setText(name);
        if (!name.isEmpty())
            tvUserAvatar.setText(String.valueOf(Character.toUpperCase(name.charAt(0))));
    }

    // ── Load the next upcoming appointment from Firestore ─────────────────────

    private void loadNextAppointment() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { showEmptyAppointment(); return; }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("appointments")
            .whereEqualTo("patientId", user.getUid())
            .whereGreaterThanOrEqualTo("date", today)
            .orderBy("date", Query.Direction.ASCENDING)
            .limit(5)   // fetch a few; filter cancelled client-side
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!isAdded()) return;
                hideLoadingAppointment();

                // Find first non-cancelled
                QueryDocumentSnapshot found = null;
                for (QueryDocumentSnapshot doc : snapshots) {
                    String status = doc.getString("status");
                    if (!"cancelled".equalsIgnoreCase(status)) { found = doc; break; }
                }

                if (found == null) { showEmptyAppointment(); return; }

                Appointment appt = found.toObject(Appointment.class);
                appt.setAppointmentId(found.getId());

                String doctorId = appt.getDoctorId();
                if (doctorId == null || doctorId.isEmpty()) {
                    renderAppointmentCard(appt, "Doctor", "Specialist");
                    return;
                }
                FirebaseDatabase.getInstance()
                    .getReference("Doctors").child(doctorId).get()
                    .addOnSuccessListener(ds -> {
                        if (!isAdded()) return;
                        String n = ds.child("fullName").getValue(String.class);
                        String s = ds.child("specialization").getValue(String.class);
                        renderAppointmentCard(appt,
                                n != null ? n : "Doctor",
                                s != null ? s : "Specialist");
                    })
                    .addOnFailureListener(e -> renderAppointmentCard(appt, "Doctor", "Specialist"));
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                hideLoadingAppointment();
                showEmptyAppointment();
            });
    }

    private void renderAppointmentCard(Appointment appt, String doctorName, String specialty) {
        String displayName = doctorName.startsWith("Dr.") ? doctorName : "Dr. " + doctorName;
        tvApptDoctor.setText(displayName);
        tvApptSpecialty.setText(specialty);
        tvApptTime.setText(appt.getFormattedDate() + "  •  " + appt.getTime());

        String type = appt.getType();
        tvApptType.setText(type != null ? type : "Consultation");

        String status = appt.getStatus();
        if (status == null) status = "pending";
        switch (status.toLowerCase()) {
            case "confirmed":
                tvApptStatusText.setText("Confirmed");
                tvApptStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.status_available));
                tvApptStatusDot.setBackgroundResource(R.drawable.bg_status_available);
                break;
            case "pending":
                tvApptStatusText.setText("Pending");
                tvApptStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.accent_orange));
                tvApptStatusDot.setBackgroundResource(R.drawable.bg_status_away);
                break;
            default:
                tvApptStatusText.setText(status);
                tvApptStatusText.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }

        boolean isVideo = type != null &&
                (type.toLowerCase().contains("video") || type.toLowerCase().contains("online"));
        joinButton.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        Appointment finalAppt        = appt;
        String      finalDoctorName  = doctorName;
        String      finalSpecialty   = specialty;
        appointmentCard.setOnClickListener(v -> openDetail(finalAppt, finalDoctorName, finalSpecialty));
        joinButton.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Starting video call…", Toast.LENGTH_SHORT).show());

        appointmentCard.setVisibility(View.VISIBLE);
    }

    private void hideLoadingAppointment() {
        if (appointmentLoading != null) appointmentLoading.setVisibility(View.GONE);
    }

    private void showEmptyAppointment() {
        hideLoadingAppointment();
        if (appointmentEmptyCard != null) appointmentEmptyCard.setVisibility(View.VISIBLE);
        if (appointmentCard      != null) appointmentCard.setVisibility(View.GONE);
    }

    private void openDetail(Appointment appt, String doctorName, String specialty) {
        Intent i = new Intent(getActivity(), AppointmentDetailActivity.class);
        i.putExtra("appointment_id",   appt.getAppointmentId());
        i.putExtra("doctor_name",      doctorName);
        i.putExtra("doctor_specialty", specialty);
        i.putExtra("date",             appt.getDate());
        i.putExtra("time",             appt.getTime());
        i.putExtra("type",             appt.getType());
        i.putExtra("status",           appt.getStatus());
        i.putExtra("notes",            appt.getNotes());
        i.putExtra("hospital",         appt.getDoctorHospital());
        startActivity(i);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void setupClickListeners() {
        actionVideoConsult.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), Consultation_Schedule.class)));
        actionAIChat.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AiMedicalActivity.class)));
        actionFindNearby.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), FindNearbyActivity.class)));
        actionMyRecords.setOnClickListener(v ->
                Toast.makeText(getContext(), "📋 My Records — coming soon", Toast.LENGTH_SHORT).show());
        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> {
                if (getActivity() instanceof DashboardActivity)
                    ((DashboardActivity) getActivity()).switchToTab(1);
            });
        }
    }
}
