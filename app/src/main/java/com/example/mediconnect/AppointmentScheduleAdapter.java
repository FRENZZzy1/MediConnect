package com.example.mediconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton;
import com.zegocloud.uikit.service.defines.ZegoUIKitUser;

import java.util.ArrayList;
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
                .inflate(R.layout.item_doctor_consultation_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AppointmentItem item = items.get(position);

        // Patient name shown where "Dr. Name" would be in the patient card
        String displayName = (item.patientName != null && !item.patientName.isEmpty())
                ? item.patientName : "Unknown Patient";
        h.tvDoctorName.setText(displayName);
        h.tvAvatar.setText(String.valueOf(displayName.charAt(0)).toUpperCase());

        // Type / specialization label
        String displayType = (item.type != null && !item.type.isEmpty())
                ? item.type.toUpperCase() : "GP TELECALL";
        h.tvSpecialization.setText(displayType);

        // Date · Time
        String date = item.date != null ? item.date : "";
        String time = item.time != null ? item.time : "";
        h.tvDateTime.setText(date + "  ·  " + time);

        // Notes
        h.tvNotes.setText((item.notes != null && !item.notes.isEmpty())
                ? item.notes : "No notes");

        // Zego — call the patient
        if (item.patientId != null && !item.patientId.isEmpty()) {
            List<ZegoUIKitUser> invitees = new ArrayList<>();
            invitees.add(new ZegoUIKitUser(item.patientId, displayName));
            h.btnVideoCall.setInvitees(invitees);
            h.btnVideoCall.setIsVideoCall(true);
            h.btnVideoCall.setVisibility(View.VISIBLE);
        } else {
            h.btnVideoCall.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvDoctorName, tvSpecialization, tvDateTime, tvNotes;
        ZegoSendCallInvitationButton btnVideoCall;

        ViewHolder(@NonNull View v) {
            super(v);
            tvAvatar         = v.findViewById(R.id.tvAvatar);
            tvDoctorName     = v.findViewById(R.id.tvDoctorName);   // reused ID — shows patient name
            tvSpecialization = v.findViewById(R.id.tvSpecialization);
            tvDateTime       = v.findViewById(R.id.tvDateTime);
            tvNotes          = v.findViewById(R.id.tvNotes);
            btnVideoCall     = v.findViewById(R.id.btnVideoCall);
        }
    }
}