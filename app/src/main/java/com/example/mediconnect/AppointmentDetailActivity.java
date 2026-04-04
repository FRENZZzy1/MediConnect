package com.example.mediconnect;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class AppointmentDetailActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView doctorNameText, specialtyText, hospitalText, dateText, timeText,
            typeText, statusText, notesText;
    private View statusIndicator;
    private MaterialButton cancelButton, rescheduleButton;
    private LinearLayout actionButtonsContainer;
    private CardView joinCallButton;

    private String appointmentId;
    private String currentStatus;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        appointmentId = getIntent().getStringExtra("appointment_id");
        String doctorName = getIntent().getStringExtra("doctor_name");
        String doctorSpecialty = getIntent().getStringExtra("doctor_specialty");
        String date = getIntent().getStringExtra("date");
        String time = getIntent().getStringExtra("time");
        String type = getIntent().getStringExtra("type");
        currentStatus = getIntent().getStringExtra("status");
        String notes = getIntent().getStringExtra("notes");
        String hospital = getIntent().getStringExtra("hospital");

        initializeViews();
        populateData(doctorName, doctorSpecialty, hospital, date, time, type, currentStatus, notes);
        setupClickListeners();
        setupButtonsBasedOnStatus();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        doctorNameText = findViewById(R.id.detail_doctor_name);
        specialtyText = findViewById(R.id.detail_specialty);
        hospitalText = findViewById(R.id.detail_hospital);
        dateText = findViewById(R.id.detail_date);
        timeText = findViewById(R.id.detail_time);
        typeText = findViewById(R.id.detail_type);
        statusText = findViewById(R.id.detail_status);
        statusIndicator = findViewById(R.id.detail_status_indicator);
        notesText = findViewById(R.id.detail_notes);
        cancelButton = findViewById(R.id.cancel_button);
        rescheduleButton = findViewById(R.id.reschedule_button);
        actionButtonsContainer = findViewById(R.id.action_buttons_container);
        joinCallButton = findViewById(R.id.join_call_button);
    }

    private void populateData(String doctorName, String specialty, String hospital,
                              String date, String time, String type, String status, String notes) {
        doctorNameText.setText(doctorName != null ? doctorName : "Doctor");
        specialtyText.setText(specialty != null ? specialty : "");
        hospitalText.setText(hospital != null ? hospital : "");
        dateText.setText(date != null ? date : "");
        timeText.setText(time != null ? time : "");
        typeText.setText(type != null ? type : "Consultation");
        notesText.setText(notes != null && !notes.isEmpty() ? notes : "No additional notes");

        // Status styling
        if (status != null) {
            switch (status.toLowerCase()) {
                case "confirmed":
                    statusText.setText("Confirmed");
                    statusText.setTextColor(getResources().getColor(R.color.status_available));
                    statusIndicator.setBackgroundResource(R.drawable.bg_status_available);
                    break;
                case "completed":
                    statusText.setText("Completed");
                    statusText.setTextColor(getResources().getColor(R.color.text_tertiary));
                    statusIndicator.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case "cancelled":
                    statusText.setText("Cancelled");
                    statusText.setTextColor(getResources().getColor(R.color.status_busy));
                    statusIndicator.setBackgroundResource(R.drawable.bg_status_busy);
                    break;
                default:
                    statusText.setText("Pending");
                    statusText.setTextColor(getResources().getColor(R.color.accent_orange));
                    statusIndicator.setBackgroundResource(R.drawable.bg_status_away);
            }
        }
    }

    private void setupButtonsBasedOnStatus() {
        if (currentStatus == null) return;

        switch (currentStatus.toLowerCase()) {
            case "confirmed":
                // Show cancel, reschedule, and join call (if video)
                actionButtonsContainer.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                rescheduleButton.setVisibility(View.VISIBLE);

                // Show join call button for video consultations
                String type = getIntent().getStringExtra("type");
                if (type != null && type.toLowerCase().contains("video")) {
                    joinCallButton.setVisibility(View.VISIBLE);
                } else {
                    joinCallButton.setVisibility(View.GONE);
                }
                break;

            case "completed":
                // Hide action buttons, show review option (future feature)
                actionButtonsContainer.setVisibility(View.GONE);
                joinCallButton.setVisibility(View.GONE);
                break;

            case "cancelled":
                // Only show reschedule
                actionButtonsContainer.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.GONE);
                rescheduleButton.setVisibility(View.VISIBLE);
                joinCallButton.setVisibility(View.GONE);
                break;
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        cancelButton.setOnClickListener(v -> showCancelConfirmation());

        rescheduleButton.setOnClickListener(v -> {
            Toast.makeText(this, "Reschedule feature coming soon!", Toast.LENGTH_SHORT).show();
            // TODO: Open reschedule flow
        });

        joinCallButton.setOnClickListener(v -> {
            Toast.makeText(this, "Joining video call...", Toast.LENGTH_SHORT).show();
            // TODO: Start video call activity
        });
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you want to cancel this appointment?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelAppointment())
                .setNegativeButton("No, Keep It", null)
                .show();
    }

    private void cancelAppointment() {
        if (appointmentId == null) return;

        db.collection("appointments").document(appointmentId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                    currentStatus = "cancelled";
                    setupButtonsBasedOnStatus();
                    statusText.setText("Cancelled");
                    statusText.setTextColor(getResources().getColor(R.color.status_busy));
                    statusIndicator.setBackgroundResource(R.drawable.bg_status_busy);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}