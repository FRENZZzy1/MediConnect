package com.example.mediconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.AppointmentViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentClickListener listener;

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

    public AppointmentsAdapter(List<Appointment> appointments, OnAppointmentClickListener listener) {
        this.appointments = appointments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_card, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);

        // Doctor name
        holder.doctorNameText.setText(appointment.getDoctorName() != null ?
                appointment.getDoctorName() : "Doctor");

        // Specialty & Hospital
        String specialtyHospital = "";
        if (appointment.getDoctorSpecialty() != null) {
            specialtyHospital = appointment.getDoctorSpecialty();
        }
        if (appointment.getDoctorHospital() != null) {
            specialtyHospital += " • " + appointment.getDoctorHospital();
        }
        holder.specialtyText.setText(specialtyHospital);

        // Date & Time
        holder.dateTimeText.setText(appointment.getFormattedDate() + " • " + appointment.getTime());

        // Type badge
        holder.typeText.setText(appointment.getType());

        // Status styling
        String status = appointment.getStatus();
        if (status != null) {
            switch (status.toLowerCase()) {
                case "confirmed":
                    holder.statusText.setText("Confirmed");
                    holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.status_available));
                    holder.statusIndicator.setBackgroundResource(R.drawable.bg_status_available);
                    break;
                case "completed":
                    holder.statusText.setText("Completed");
                    holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.text_tertiary));
                    holder.statusIndicator.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case "cancelled":
                    holder.statusText.setText("Cancelled");
                    holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.status_busy));
                    holder.statusIndicator.setBackgroundResource(R.drawable.bg_status_busy);
                    break;
                default:
                    holder.statusText.setText("Pending");
                    holder.statusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.accent_orange));
                    holder.statusIndicator.setBackgroundResource(R.drawable.bg_status_away);
            }
        }

        // Today badge
        if (appointment.isToday()) {
            holder.todayBadge.setVisibility(View.VISIBLE);
        } else {
            holder.todayBadge.setVisibility(View.GONE);
        }

        // Click listener
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAppointmentClick(appointment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView doctorNameText, specialtyText, dateTimeText, typeText, statusText, todayBadge;
        View statusIndicator;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.appointment_card);
            doctorNameText = itemView.findViewById(R.id.doctor_name);
            specialtyText = itemView.findViewById(R.id.doctor_specialty);
            dateTimeText = itemView.findViewById(R.id.date_time);
            typeText = itemView.findViewById(R.id.type_badge);
            statusText = itemView.findViewById(R.id.status_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            todayBadge = itemView.findViewById(R.id.today_badge);
        }
    }
}