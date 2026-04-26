package com.example.mediconnect;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final List<AppointmentItem> items;
    private final SimpleDateFormat inputSdf   = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displaySdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

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

        // ── Position number (timeline dot) ────────────────────────────────────
        h.tvPositionNumber.setText(String.valueOf(position + 1));

        // ── Time (left column) ────────────────────────────────────────────────
        h.tvTime.setText(item.time != null ? item.time : "--:--");

        // ── Patient name (doctor side shows patient) ──────────────────────────
        String name = (item.patientName != null && !item.patientName.isEmpty())
                ? item.patientName.trim() : "Unknown Patient";
        h.tvPatientName.setText(name);

        // ── Initials avatar ───────────────────────────────────────────────────
        h.tvInitials.setText(getInitials(name));

        // ── Date subtitle (below patient name) ───────────────────────────────
        if (item.date != null && !item.date.isEmpty()) {
            try {
                Date parsed = inputSdf.parse(item.date);
                h.tvAppointmentDate.setText(parsed != null ? displaySdf.format(parsed) : item.date);
            } catch (Exception e) {
                h.tvAppointmentDate.setText(item.date);
            }
        } else {
            h.tvAppointmentDate.setText("No date");
        }

        // ── Appointment type chip ─────────────────────────────────────────────
        String type = item.type != null && !item.type.isEmpty() ? item.type : "Consultation";
        h.tvType.setText(type);
        setTypeChipColor(h.tvType, type);

        // ── Date detail row ───────────────────────────────────────────────────
        if (item.date != null && !item.date.isEmpty()) {
            try {
                Date parsed = inputSdf.parse(item.date);
                h.tvDate.setText(parsed != null ? displaySdf.format(parsed) : item.date);
            } catch (Exception e) {
                h.tvDate.setText(item.date);
            }
        } else {
            h.tvDate.setText("N/A");
        }

        // ── Time detail row ───────────────────────────────────────────────────
        h.tvTimeDetail.setText(item.time != null ? item.time : "N/A");

        // ── Appointment ID / reference ────────────────────────────────────────
        h.tvAppointmentId.setText(item.appointmentId != null ? item.appointmentId : "N/A");

        // ── Status ────────────────────────────────────────────────────────────
        String status = item.status != null ? item.status : "confirmed";

        View statusDot = h.itemView.findViewById(R.id.viewStatusDot); // add this id to your XML dot View
        if ("ended".equalsIgnoreCase(status)) {
            h.tvStatus.setTextColor(Color.parseColor("#94A3B8")); // grey
        } else {
            h.tvStatus.setTextColor(Color.parseColor("#10B981")); // green
        }




        h.tvStatus.setText(capitalize(status));

        // ── Notes (show row only if notes exist) ──────────────────────────────
        if (item.notes != null && !item.notes.trim().isEmpty()) {
            h.layoutNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText(item.notes.trim());
        } else {
            h.layoutNotes.setVisibility(View.GONE);
        }

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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void setTypeChipColor(TextView chip, String type) {
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
            bg   = Color.parseColor("#ECFEFF");
            text = Color.parseColor("#0E7490");
        }

        chip.setBackgroundTintList(ColorStateList.valueOf(bg));
        chip.setTextColor(text);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvPositionNumber;
        TextView tvInitials, tvPatientName, tvAppointmentDate;
        TextView tvType, tvDate, tvTimeDetail, tvAppointmentId, tvStatus;
        TextView tvNotes;
        View     layoutNotes, viewConnector;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime            = itemView.findViewById(R.id.tvTime);
            tvPositionNumber  = itemView.findViewById(R.id.tvPositionNumber);
            tvInitials        = itemView.findViewById(R.id.tvInitials);
            tvPatientName     = itemView.findViewById(R.id.tvPatientName);      // ← fixed (was tvDoctorName)
            tvAppointmentDate = itemView.findViewById(R.id.tvAppointmentDate);  // ← fixed (was tvDoctorSpecialty)
            tvType            = itemView.findViewById(R.id.tvType);
            tvDate            = itemView.findViewById(R.id.tvDate);
            tvTimeDetail      = itemView.findViewById(R.id.tvTimeDetail);
            tvAppointmentId   = itemView.findViewById(R.id.tvAppointmentId);
            tvStatus          = itemView.findViewById(R.id.tvStatus);
            tvNotes           = itemView.findViewById(R.id.tvNotes);
            layoutNotes       = itemView.findViewById(R.id.layoutNotes);
            viewConnector     = itemView.findViewById(R.id.viewConnector);
        }
    }
}