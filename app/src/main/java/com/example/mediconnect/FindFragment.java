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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FindFragment extends Fragment {

    // ─── Views ───────────────────────────────────────────────────────────────────
    private EditText searchEditText;
    private ChipGroup specialtyChipGroup;
    private RecyclerView doctorsRecyclerView;

    // ─── Data ────────────────────────────────────────────────────────────────────
    private DoctorsAdapter doctorsAdapter;
    private final List<Doctor> doctorList = new ArrayList<>();
    private String activeChip = "All";

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find, container, false);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        fetchDoctorsFromFirebase();

        return view;
    }

    // ─── View Setup ──────────────────────────────────────────────────────────────

    private void initViews(View view) {
        searchEditText      = view.findViewById(R.id.search_edit_text);
        specialtyChipGroup  = view.findViewById(R.id.specialty_chip_group);
        doctorsRecyclerView = view.findViewById(R.id.doctors_recycler_view);
    }

    private void setupRecyclerView() {
        doctorsAdapter = new DoctorsAdapter(doctorList);
        doctorsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        doctorsRecyclerView.setAdapter(doctorsAdapter);
    }

    // ─── Firebase ────────────────────────────────────────────────────────────────

    private void fetchDoctorsFromFirebase() {
        DatabaseReference doctorsRef = FirebaseDatabase.getInstance()
                .getReference("Doctors");

        doctorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Doctor doctor = child.getValue(Doctor.class);
                    if (doctor != null) {
                        doctor.uid = child.getKey();
                        doctorList.add(doctor);
                    }
                }

                buildSpecialtyChips(doctorList);
                doctorsAdapter.setFullList(doctorList);
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Failed to load doctors: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ─── Specialty Chips ─────────────────────────────────────────────────────────

    private void buildSpecialtyChips(List<Doctor> doctors) {
        if (getContext() == null) return;
        specialtyChipGroup.removeAllViews();

        List<String> specialties = new ArrayList<>();
        specialties.add("All");
        for (Doctor d : doctors) {
            // Only include specialties of available doctors
            if (d.isAvailable && d.specialization != null && !specialties.contains(d.specialization)) {
                specialties.add(d.specialization);
            }
        }

        for (String specialty : specialties) {
            Chip chip = new Chip(getContext());
            chip.setText(specialty);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(specialty.equals(activeChip));

            chip.setOnClickListener(v -> {
                activeChip = specialty;
                applyFilters();
            });

            specialtyChipGroup.addView(chip);
        }
    }

    // ─── Search ──────────────────────────────────────────────────────────────────

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
        });
    }

    // ─── Combined Filter ─────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = searchEditText.getText().toString().toLowerCase().trim();
        doctorsAdapter.applyFilters(activeChip, query);
    }

    // ─── Navigate to Doctor Profile ──────────────────────────────────────────────

    private void openDoctorProfile(Doctor doctor) {
        Intent intent = new Intent(getActivity(), DoctorProfileActivity.class);
        intent.putExtra("doctor_uid",                doctor.uid);
        intent.putExtra("doctor_name",               doctor.fullName);
        intent.putExtra("doctor_specialty",          doctor.specialization);
        intent.putExtra("doctor_clinic",             doctor.clinicName);
        intent.putExtra("doctor_location",           doctor.location);
        intent.putExtra("doctor_available",          doctor.isAvailable);
        intent.putExtra("doctor_available_days",     doctor.availableDays);
        intent.putExtra("doctor_consultation_hours", doctor.consultationHours);
        intent.putExtra("doctor_consultation_fee",   doctor.consultationFee);
        intent.putExtra("doctor_email",              doctor.email);
        intent.putExtra("doctor_prc_license",        doctor.prcLicense);
        intent.putExtra("doctor_years_experience",   doctor.yearsOfExperience);
        startActivity(intent);
    }

    // ─── Adapter ─────────────────────────────────────────────────────────────────

    public class DoctorsAdapter extends RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder> {

        private List<Doctor> displayList = new ArrayList<>();
        private List<Doctor> fullList    = new ArrayList<>();

        public DoctorsAdapter(List<Doctor> initial) {
            this.fullList    = new ArrayList<>(initial);
            this.displayList = new ArrayList<>(initial);
        }

        public void setFullList(List<Doctor> list) {
            this.fullList    = new ArrayList<>(list);
            this.displayList = new ArrayList<>(list);
            notifyDataSetChanged();
        }

        public void applyFilters(String specialty, String query) {
            displayList.clear();

            for (Doctor doctor : fullList) {

                // ── Hide doctors that are not available ──────────────────────
                if (!doctor.isAvailable) continue;

                boolean matchesChip = specialty.equals("All") ||
                        (doctor.specialization != null &&
                                doctor.specialization.equalsIgnoreCase(specialty));

                boolean matchesSearch = query.isEmpty() ||
                        (doctor.fullName != null &&
                                doctor.fullName.toLowerCase().contains(query)) ||
                        (doctor.specialization != null &&
                                doctor.specialization.toLowerCase().contains(query)) ||
                        (doctor.clinicName != null &&
                                doctor.clinicName.toLowerCase().contains(query)) ||
                        (doctor.location != null &&
                                doctor.location.toLowerCase().contains(query));

                if (matchesChip && matchesSearch) {
                    displayList.add(doctor);
                }
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_doctor_card, parent, false);
            return new DoctorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
            Doctor doctor = displayList.get(position);

            // Name
            holder.nameText.setText(
                    doctor.fullName != null ? doctor.fullName : "Unknown Doctor");

            // Specialty • Clinic
            String specialtyLine = "";
            if (doctor.specialization != null) specialtyLine += doctor.specialization;
            if (doctor.clinicName != null)      specialtyLine += " • " + doctor.clinicName;
            holder.specialtyText.setText(specialtyLine);

            // Location
            holder.distanceText.setText(
                    doctor.location != null ? "📍 " + doctor.location : "");

            // Availability status — always "Available" here since we filter out unavailable
            holder.statusDot.setBackgroundResource(R.drawable.bg_status_available);
            holder.statusText.setText("Available");
            holder.statusText.setTextColor(
                    getResources().getColor(R.color.status_available));

            // Click listeners
            holder.bookButton.setOnClickListener(v -> openDoctorProfile(doctor));
            holder.itemView.setOnClickListener(v -> openDoctorProfile(doctor));
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        // ─── ViewHolder ──────────────────────────────────────────────────────────

        class DoctorViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, specialtyText, experienceText, distanceText, statusText;
            View     statusDot;
            CardView bookButton;

            public DoctorViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText       = itemView.findViewById(R.id.doctor_name);
                specialtyText  = itemView.findViewById(R.id.doctor_specialty);
                distanceText   = itemView.findViewById(R.id.doctor_distance);
                statusText     = itemView.findViewById(R.id.doctor_status);
                statusDot      = itemView.findViewById(R.id.status_dot);
                bookButton     = itemView.findViewById(R.id.book_button);
            }
        }
    }
}