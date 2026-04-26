package com.example.mediconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<AppointmentItem> items;

    public ScheduleAdapter(List<AppointmentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AppointmentItem item = items.get(position);

        // ── Time ──────────────────────────────────────────────────────────────
        h.tvTime.setText(item.time != null ? item.time : "--:--");

        // ── Doctor name (instead of patient) ────────────────────────────────────
        String doctorName = (item.doctorName != null && !item.doctorName.isEmpty())
                ? item.doctorName : "" + ((item.patientName != null && !item.patientName.isEmpty())
                ? item.patientName : "Unknown");
        h.tvDoctorName.setText(doctorName);

        // ── Doctor Specialty ───────────────────────────────────────────────────
        if (h.tvDoctorSpecialty != null) {
            String specialty = (item.specialty != null && !item.specialty.isEmpty())
                    ? item.specialty : "Specialist";
            h.tvDoctorSpecialty.setText(specialty);
        }

        // ── Initials avatar ───────────────────────────────────────────────────
        String initials = getInitials(doctorName);
        h.tvInitials.setText(initials);

        // ── Appointment type chip ─────────────────────────────────────────────
        String type = item.type != null ? item.type : "Consultation";
        h.tvType.setText(type);
        setTypeChipColor(h.tvType, type);

        // ── Hospital ───────────────────────────────────────────────────────────
        if (h.tvHospital != null) {
            String hospital = (item.hospital != null && !item.hospital.isEmpty())
                    ? item.hospital : "";
            h.tvHospital.setText(hospital);
            h.tvHospital.setVisibility(hospital.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // ── Notes ─────────────────────────────────────────────────────────────
        if (item.notes != null && !item.notes.isEmpty()) {
            h.tvNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText(item.notes);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }

        // ── Position number ───────────────────────────────────────────────────
        h.tvPositionNumber.setText(String.valueOf(position + 1));

        // ── Connector line: hide for last item ────────────────────────────────
        h.viewConnector.setVisibility(
                position == items.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    private void setTypeChipColor(TextView chip, String type) {
        // Match your app's appointment types to colour codes
        String lower = type.toLowerCase();
        int bg, text;

        if (lower.contains("online") || lower.contains("virtual") || lower.contains("tele")) {
            bg   = Color.parseColor("#EFF6FF");
            text = Color.parseColor("#2563EB");
        } else if (lower.contains("follow") || lower.contains("checkup")) {
            bg   = Color.parseColor("#F0FDF4");
            text = Color.parseColor("#16A34A");
        } else if (lower.contains("emergency") || lower.contains("urgent")) {
            bg   = Color.parseColor("#FEF2F2");
            text = Color.parseColor("#DC2626");
        } else if (lower.contains("lab") || lower.contains("test")) {
            bg   = Color.parseColor("#FFF7ED");
            text = Color.parseColor("#EA580C");
        } else {
            // Default — teal (matches your app theme)
            bg   = Color.parseColor("#ECFEFF");
            text = Color.parseColor("#0E7490");
        }

        chip.setBackgroundTintList(ColorStateList.valueOf(bg));
        chip.setTextColor(text);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvDoctorName, tvDoctorSpecialty, tvInitials, tvType, tvNotes, tvHospital, tvPositionNumber;
        View     viewConnector;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime             = itemView.findViewById(R.id.tvTime);
            tvDoctorName       = itemView.findViewById(R.id.tvDoctorName);
            tvDoctorSpecialty  = itemView.findViewById(R.id.tvDoctorSpecialty);
            tvInitials         = itemView.findViewById(R.id.tvInitials);
            tvType             = itemView.findViewById(R.id.tvType);
            tvNotes            = itemView.findViewById(R.id.tvNotes);
            tvHospital         = itemView.findViewById(R.id.tvHospital);
            tvPositionNumber   = itemView.findViewById(R.id.tvPositionNumber);
            viewConnector      = itemView.findViewById(R.id.viewConnector);
        }
    }
}