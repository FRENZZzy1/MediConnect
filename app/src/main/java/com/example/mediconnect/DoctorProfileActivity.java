package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class DoctorProfileActivity extends AppCompatActivity {

    private LinearLayout backButton;
    private TextView doctorName, doctorSpecialty, doctorHospital, doctorRating,
            doctorExperience, doctorStatus, aboutText;
    private LinearLayout videoCallButton, inPersonButton;
    private CardView confirmButton;
    private View statusDot;

    private String selectedDate = "Feb 26";
    private String selectedTime = "10:30 AM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);

        initializeViews();
        loadDoctorData();
        setupCalendar();
        setupTimeSlots();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        doctorName = findViewById(R.id.doctor_name_large);
        doctorSpecialty = findViewById(R.id.doctor_specialty_detail);
        doctorHospital = findViewById(R.id.doctor_hospital);
        doctorRating = findViewById(R.id.doctor_rating_detail);
        doctorExperience = findViewById(R.id.doctor_experience_detail);
        doctorStatus = findViewById(R.id.doctor_status_detail);
        statusDot = findViewById(R.id.status_dot_large);
        aboutText = findViewById(R.id.about_doctor_text);
        videoCallButton = findViewById(R.id.video_call_button);
        inPersonButton = findViewById(R.id.in_person_button);
        confirmButton = findViewById(R.id.confirm_appointment_button);
    }

    private void loadDoctorData() {
        Intent intent = getIntent();
        String name = intent.getStringExtra("doctor_name");
        String specialty = intent.getStringExtra("doctor_specialty");
        String hospital = intent.getStringExtra("doctor_hospital");
        float rating = intent.getFloatExtra("doctor_rating", 0);
        int experience = intent.getIntExtra("doctor_experience", 0);
        boolean isAvailable = intent.getBooleanExtra("doctor_available", false);

        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.doctor_profile_default_name);
        }
        if (specialty == null || specialty.trim().isEmpty()) {
            specialty = getString(R.string.doctor_profile_default_specialty);
        }
        if (hospital == null || hospital.trim().isEmpty()) {
            hospital = getString(R.string.doctor_profile_default_hospital);
        }

        doctorName.setText(name);
        doctorSpecialty.setText(specialty);
        doctorHospital.setText(hospital);
        doctorRating.setText(String.format(Locale.getDefault(), "★ %.1f", rating));
        doctorExperience.setText(getString(R.string.doctor_profile_experience, experience));

        if (isAvailable) {
            statusDot.setBackgroundResource(R.drawable.bg_status_dot);
            doctorStatus.setText(R.string.doctor_profile_status_available);
            doctorStatus.setTextColor(ContextCompat.getColor(this, R.color.status_normal));
        } else {
            statusDot.setBackgroundResource(R.drawable.circle_status_inactive);
            doctorStatus.setText(R.string.doctor_profile_status_busy);
            doctorStatus.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        }

        // About section text
        String displayName = name.startsWith("Dr. ") ? name.substring(4) : name;
        aboutText.setText(getString(R.string.doctor_profile_about,
                displayName,
                specialty.toLowerCase(Locale.getDefault()),
                experience,
                hospital));
    }

    private void setupCalendar() {
        // Calendar days
        String[] days = {"23", "24", "25", "26", "27", "28"};
        String[] weekdays = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

        LinearLayout calendarContainer = findViewById(R.id.calendar_days_container);

        for (int i = 0; i < days.length; i++) {
            final String dayNumberText = days[i];
            View dayView = getLayoutInflater().inflate(R.layout.item_calendar_day, calendarContainer, false);
            TextView dayNumber = dayView.findViewById(R.id.day_number);
            TextView dayWeek = dayView.findViewById(R.id.day_week);

            dayNumber.setText(dayNumberText);
            dayWeek.setText(weekdays[(i + 2) % 7]); // Start from Thursday

            // Highlight selected date (26)
            if (dayNumberText.equals("26")) {
                dayView.setBackgroundResource(R.drawable.bg_calendar_selected);
                dayNumber.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                dayView.setBackgroundResource(R.drawable.bg_calendar_unselected);
            }

            final String date = getString(R.string.doctor_profile_selected_date, dayNumberText);
            dayView.setOnClickListener(v -> {
                selectedDate = date;
                Toast.makeText(this, date, Toast.LENGTH_SHORT).show();
                // Update UI to show selection
            });

            calendarContainer.addView(dayView);
        }
    }

    private void setupTimeSlots() {
        String[] times = {"9:00 AM", "9:30 AM", "10:00 AM", "10:30 AM", "11:00 AM",
                "2:00 PM", "2:30 PM", "3:00 PM"};

        LinearLayout slotsContainer = findViewById(R.id.time_slots_container);

        for (String time : times) {
            TextView slotView = new TextView(this);
            slotView.setText(time);
            slotView.setPadding(24, 16, 24, 16);

            if (time.equals("10:30 AM")) {
                // Selected slot
                slotView.setBackgroundResource(R.drawable.bg_time_slot_selected);
                slotView.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                slotView.setBackgroundResource(R.drawable.bg_time_slot_unselected);
                slotView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 12, 12);
            slotView.setLayoutParams(params);

            slotView.setOnClickListener(v -> {
                selectedTime = time;
                Toast.makeText(this, getString(R.string.doctor_profile_selected_time, time), Toast.LENGTH_SHORT).show();
            });

            slotsContainer.addView(slotView);
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        videoCallButton.setOnClickListener(v -> {
            videoCallButton.setBackgroundResource(R.drawable.bg_consult_selected);
            inPersonButton.setBackgroundResource(R.drawable.bg_consult_unselected);
            Toast.makeText(this, R.string.doctor_profile_video_call_selected, Toast.LENGTH_SHORT).show();
        });

        inPersonButton.setOnClickListener(v -> {
            inPersonButton.setBackgroundResource(R.drawable.bg_consult_selected);
            videoCallButton.setBackgroundResource(R.drawable.bg_consult_unselected);
            Toast.makeText(this, R.string.doctor_profile_in_person_selected, Toast.LENGTH_SHORT).show();
        });

        confirmButton.setOnClickListener(v -> {
            Toast.makeText(this,
                    getString(R.string.doctor_profile_confirmation, selectedDate, selectedTime),
                    Toast.LENGTH_LONG).show();
            // TODO: Save appointment to database
            finish();
        });
    }
}