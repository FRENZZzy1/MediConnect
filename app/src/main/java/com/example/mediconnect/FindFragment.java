package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private EditText  searchEditText;
    private ChipGroup specialtyChipGroup;
    private RecyclerView doctorsRecyclerView;
    private View     loadingSpinner, emptyState;
    private TextView tvResultsCount;

    private DoctorsAdapter doctorsAdapter;
    private final List<Doctor> doctorList = new ArrayList<>();
    private String activeChip = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_find, container, false);

        searchEditText      = view.findViewById(R.id.search_edit_text);
        specialtyChipGroup  = view.findViewById(R.id.specialty_chip_group);
        doctorsRecyclerView = view.findViewById(R.id.doctors_recycler_view);
        loadingSpinner      = view.findViewById(R.id.find_loading);
        emptyState          = view.findViewById(R.id.find_empty_state);
        tvResultsCount      = view.findViewById(R.id.tv_results_count);

        setupRecyclerView();
        setupSearch();
        fetchDoctors();
        return view;
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        doctorsAdapter = new DoctorsAdapter(doctorList);
        doctorsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        doctorsRecyclerView.setAdapter(doctorsAdapter);
    }

    // ── Firebase ──────────────────────────────────────────────────────────────

    private void fetchDoctors() {
        showLoading();
        FirebaseDatabase.getInstance()
            .getReference("Doctors")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    doctorList.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Doctor d = child.getValue(Doctor.class);
                        if (d != null) {
                            d.uid = child.getKey();
                            doctorList.add(d);
                        }
                    }
                    buildSpecialtyChips(doctorList);
                    doctorsAdapter.setFullList(doctorList);
                    applyFilters();
                    hideLoading();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (!isAdded()) return;
                    hideLoading();
                    Toast.makeText(getContext(),
                            "Failed to load doctors", Toast.LENGTH_SHORT).show();
                }
            });
    }

    // ── Specialty Chips ───────────────────────────────────────────────────────

    private void buildSpecialtyChips(List<Doctor> doctors) {
        if (!isAdded() || getContext() == null) return;
        specialtyChipGroup.removeAllViews();

        List<String> specialties = new ArrayList<>();
        specialties.add("All");
        for (Doctor d : doctors) {
            if (d.isAvailable && d.specialization != null
                    && !specialties.contains(d.specialization))
                specialties.add(d.specialization);
        }

        for (String s : specialties) {
            Chip chip = new Chip(requireContext());
            chip.setText(s);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(s.equals(activeChip));
            chip.setOnClickListener(v -> { activeChip = s; applyFilters(); });
            specialtyChipGroup.addView(chip);
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        String query = searchEditText.getText().toString().toLowerCase().trim();
        int count = doctorsAdapter.applyFilters(activeChip, query);
        updateResultsCount(count);
        if (count == 0 && doctorList.size() > 0) showEmpty();
        else if (count > 0) showList();
    }

    private void updateResultsCount(int count) {
        if (tvResultsCount != null) {
            tvResultsCount.setText(count + " doctor" + (count == 1 ? "" : "s") + " available");
        }
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private void showLoading() {
        loadingSpinner.setVisibility(View.VISIBLE);
        doctorsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingSpinner.setVisibility(View.GONE);
    }

    private void showList() {
        doctorsRecyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showEmpty() {
        doctorsRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    // ── Open doctor profile ───────────────────────────────────────────────────

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
        startActivity(intent);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Inner Adapter
    // ════════════════════════════════════════════════════════════════════════

    public class DoctorsAdapter extends RecyclerView.Adapter<DoctorsAdapter.DoctorViewHolder> {

        private final List<Doctor> fullList    = new ArrayList<>();
        private final List<Doctor> displayList = new ArrayList<>();

        public DoctorsAdapter(List<Doctor> initial) {
            fullList.addAll(initial);
            displayList.addAll(initial);
        }

        public void setFullList(List<Doctor> list) {
            fullList.clear(); fullList.addAll(list);
            displayList.clear(); displayList.addAll(list);
            notifyDataSetChanged();
        }

        /** Returns count of items shown after filtering */
        public int applyFilters(String specialty, String query) {
            displayList.clear();
            for (Doctor d : fullList) {
                if (!d.isAvailable) continue;

                boolean matchChip   = specialty.equals("All") ||
                        (d.specialization != null && d.specialization.equalsIgnoreCase(specialty));
                boolean matchSearch = query.isEmpty() ||
                        (d.fullName      != null && d.fullName.toLowerCase().contains(query)) ||
                        (d.specialization!= null && d.specialization.toLowerCase().contains(query))||
                        (d.clinicName    != null && d.clinicName.toLowerCase().contains(query)) ||
                        (d.location      != null && d.location.toLowerCase().contains(query));

                if (matchChip && matchSearch) displayList.add(d);
            }
            notifyDataSetChanged();
            return displayList.size();
        }

        @NonNull
        @Override
        public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_doctor_card, parent, false);
            return new DoctorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DoctorViewHolder h, int position) {
            Doctor d = displayList.get(position);

            // Name
            String name = d.fullName != null ? d.fullName : "Unknown Doctor";
            h.nameText.setText(name.startsWith("Dr.") ? name : "Dr. " + name);

            // Initials for avatar
            h.initialsText.setText(getInitials(d.fullName));

            // Specialty • Clinic
            String line = "";
            if (d.specialization != null) line += d.specialization;
            if (d.clinicName     != null) line += (line.isEmpty() ? "" : " • ") + d.clinicName;
            h.specialtyText.setText(line.isEmpty() ? "General Practitioner" : line);

            // Location
            h.locationText.setText(d.location != null ? d.location : "—");

            // Available days
            if (h.daysText != null) {
                h.daysText.setText(d.availableDays != null ? "📅 " + d.availableDays : "");
            }

            // Fee
            h.feeText.setText(d.consultationFee != null && !d.consultationFee.isEmpty()
                    ? "₱" + d.consultationFee : "");

            // Status — always available here (we filter out unavailable above)
            h.statusDot.setBackgroundResource(R.drawable.bg_status_available);
            h.statusText.setText("Available");
            if (getContext() != null)
                h.statusText.setTextColor(
                        getContext().getResources().getColor(R.color.status_available));

            h.bookButton.setOnClickListener(v -> openDoctorProfile(d));
            h.itemView.setOnClickListener(v -> openDoctorProfile(d));
        }

        @Override public int getItemCount() { return displayList.size(); }

        private String getInitials(String name) {
            if (name == null || name.isEmpty()) return "?";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) return String.valueOf(Character.toUpperCase(parts[0].charAt(0)));
            return String.valueOf(Character.toUpperCase(parts[0].charAt(0)))
                 + String.valueOf(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }

        class DoctorViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, specialtyText, locationText, daysText, statusText, initialsText, feeText;
            View     statusDot;
            com.google.android.material.button.MaterialButton bookButton;

            DoctorViewHolder(@NonNull View v) {
                super(v);
                nameText      = v.findViewById(R.id.doctor_name);
                initialsText  = v.findViewById(R.id.doctor_initials);
                specialtyText = v.findViewById(R.id.doctor_specialty);
                locationText  = v.findViewById(R.id.doctor_distance);
                daysText      = v.findViewById(R.id.doctor_days);
                statusText    = v.findViewById(R.id.doctor_status);
                statusDot     = v.findViewById(R.id.status_dot);
                feeText       = v.findViewById(R.id.doctor_fee);
                bookButton    = v.findViewById(R.id.book_button);
            }
        }
    }
}
