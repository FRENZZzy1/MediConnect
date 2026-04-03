package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class FindFragment extends Fragment {

    private EditText searchEditText;
    private ChipGroup specialtyChipGroup;
    private RecyclerView doctorsRecyclerView;
    private DoctorsAdapter doctorsAdapter;
    private List<Doctor> doctorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find, container, false);

        initializeViews(view);
        setupSpecialtyChips();
        setupDoctorsList();
        setupSearch();

        return view;
    }

    private void initializeViews(View view) {
        searchEditText = view.findViewById(R.id.search_edit_text);
        specialtyChipGroup = view.findViewById(R.id.specialty_chip_group);
        doctorsRecyclerView = view.findViewById(R.id.doctors_recycler_view);
    }

    private void setupSpecialtyChips() {
        String[] specialties = {"All", "Cardio", "Derm", "Neuro", "Pedia", "Ortho"};

        for (String specialty : specialties) {
            Chip chip = new Chip(getContext());
            chip.setText(specialty);
            chip.setCheckable(true);
            chip.setClickable(true);

            if (specialty.equals("All")) {
                chip.setChecked(true);
            }

            chip.setOnClickListener(v -> filterDoctors(specialty));
            specialtyChipGroup.addView(chip);
        }
    }

    private void setupDoctorsList() {
        doctorList = new ArrayList<>();
        doctorList.add(new Doctor("Dr. Juan Reyes", "Cardiologist", "St. Luke's Hospital",
                4.9f, 12, 1.2, true));
        doctorList.add(new Doctor("Dr. Ana Cruz", "Dermatologist", "Medical City",
                4.8f, 8, 0.8, true));
        doctorList.add(new Doctor("Dr. Miguel Tan", "Neurologist", "Asian Hospital",
                4.7f, 15, 2.1, false));
        doctorList.add(new Doctor("Dr. Sofia Lim", "Pediatrician", "Makati Med",
                4.9f, 10, 3.5, true));

        doctorsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        doctorsAdapter = new DoctorsAdapter(doctorList);
        doctorsRecyclerView.setAdapter(doctorsAdapter);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                doctorsAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterDoctors(String specialty) {
        doctorsAdapter.filterBySpecialty(specialty);
    }

    private void openDoctorProfile(Doctor doctor) {
        Intent intent = new Intent(getActivity(), DoctorProfileActivity.class);
        intent.putExtra("doctor_name", doctor.name);
        intent.putExtra("doctor_specialty", doctor.specialty);
        intent.putExtra("doctor_hospital", doctor.hospital);
        intent.putExtra("doctor_rating", doctor.rating);
        intent.putExtra("doctor_experience", doctor.experience);
        intent.putExtra("doctor_available", doctor.isAvailable);
        startActivity(intent);
    }

    public static class Doctor {
        String name, specialty, hospital;
        float rating;
        int experience;
        double distance;
        boolean isAvailable;

        public Doctor(String name, String specialty, String hospital, float rating,
                      int experience, double distance, boolean isAvailable) {
            this.name = name;
            this.specialty = specialty;
            this.hospital = hospital;
            this.rating = rating;
            this.experience = experience;
            this.distance = distance;
            this.isAvailable = isAvailable;
        }
    }

    public class DoctorsAdapter extends RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder> {
        private List<Doctor> doctors;
        private List<Doctor> doctorsFull;

        public DoctorsAdapter(List<Doctor> doctors) {
            this.doctors = new ArrayList<>(doctors);
            this.doctorsFull = new ArrayList<>(doctors);
        }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_doctor_card, parent, false);
            return new DoctorViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
            Doctor doctor = doctors.get(position);

            holder.nameText.setText(doctor.name);
            holder.specialtyText.setText(doctor.specialty + " • " + doctor.hospital);
            holder.ratingText.setText(String.format("★ %.1f", doctor.rating));
            holder.experienceText.setText(doctor.experience + " yrs exp");
            holder.distanceText.setText(String.format("%.1f km away", doctor.distance));

            if (doctor.isAvailable) {
                holder.statusDot.setBackgroundResource(R.drawable.bg_status_available);
                holder.statusText.setText("Available");
                holder.statusText.setTextColor(getResources().getColor(R.color.status_available));
            } else {
                holder.statusDot.setBackgroundResource(R.drawable.bg_status_busy);
                holder.statusText.setText("Busy");
                holder.statusText.setTextColor(getResources().getColor(R.color.status_busy));
            }

            holder.bookButton.setOnClickListener(v -> openDoctorProfile(doctor));
            holder.itemView.setOnClickListener(v -> openDoctorProfile(doctor));
        }

        @Override
        public int getItemCount() {
            return doctors.size();
        }

        public void filter(String text) {
            doctors.clear();
            if (text.isEmpty()) {
                doctors.addAll(doctorsFull);
            } else {
                String filterPattern = text.toLowerCase().trim();
                for (Doctor doctor : doctorsFull) {
                    if (doctor.name.toLowerCase().contains(filterPattern) ||
                            doctor.specialty.toLowerCase().contains(filterPattern)) {
                        doctors.add(doctor);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void filterBySpecialty(String specialty) {
            doctors.clear();
            if (specialty.equals("All")) {
                doctors.addAll(doctorsFull);
            } else {
                for (Doctor doctor : doctorsFull) {
                    if (doctor.specialty.toLowerCase().contains(specialty.toLowerCase())) {
                        doctors.add(doctor);
                    }
                }
            }
            notifyDataSetChanged();
        }

        class DoctorViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, specialtyText, ratingText, experienceText, distanceText, statusText;
            View statusDot;
            CardView bookButton;

            public DoctorViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.doctor_name);
                specialtyText = itemView.findViewById(R.id.doctor_specialty);
                ratingText = itemView.findViewById(R.id.doctor_rating);
                experienceText = itemView.findViewById(R.id.doctor_experience);
                distanceText = itemView.findViewById(R.id.doctor_distance);
                statusText = itemView.findViewById(R.id.doctor_status);
                statusDot = itemView.findViewById(R.id.status_dot);
                bookButton = itemView.findViewById(R.id.book_button);
            }
        }
    }
}