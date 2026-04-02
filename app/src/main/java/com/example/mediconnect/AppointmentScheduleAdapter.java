package com.example.mediconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppointmentScheduleAdapter
        extends RecyclerView.Adapter<AppointmentScheduleAdapter.ViewHolder> {

    private final List<AppointmentItem> items;

    public AppointmentScheduleAdapter(List<AppointmentItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentItem item = items.get(position);

        // ── Time: split "09:00 AM" into "9:00" and "AM" ──────────────────────
        if (item.time != null && !item.time.isEmpty()) {
            String[] parts = item.time.split(" ");
            if (parts.length == 2) {
                holder.tvTime.setText(parts[0]);
                holder.tvAmPm.setText(parts[1]);
            } else {
                holder.tvTime.setText(item.time);
                holder.tvAmPm.setText("");
            }
        } else {
            holder.tvTime.setText("--:--");
            holder.tvAmPm.setText("");
        }

        // ── Patient Name (direct from item) ───────────────────────────────────
        String patientName = item.patientName != null && !item.patientName.isEmpty()
                ? item.patientName
                : "Unknown Patient";
        holder.tvPatientName.setText(patientName);

        // ── Type + emoji ──────────────────────────────────────────────────────
        String type = item.type != null ? item.type : "Appointment";
        holder.tvType.setText(" " + type);

        String typeLower = type.toLowerCase();
        if (typeLower.contains("video") || typeLower.contains("tele")) {
            holder.tvTypeEmoji.setText("📹");
        } else if (typeLower.contains("person") || typeLower.contains("clinic")) {
            holder.tvTypeEmoji.setText("🏥");
        } else {
            holder.tvTypeEmoji.setText("📋");
        }

        // ── Status badge ──────────────────────────────────────────────────────
        String status = item.status != null ? item.status.toLowerCase() : "pending";
        holder.tvStatusBadge.setText(capitalize(status));

        switch (status) {
            case "confirmed":
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_confirmed);
                break;
            case "cancelled":
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_cancelled);
                break;
            default: // pending
                holder.tvStatusBadge.setBackgroundResource(R.drawable.badge_pending);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvAmPm, tvPatientName, tvTypeEmoji, tvType, tvStatusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAmPm = itemView.findViewById(R.id.tvAmPm);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvTypeEmoji = itemView.findViewById(R.id.tvTypeEmoji);
            tvType = itemView.findViewById(R.id.tvType);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
        }
    }
}