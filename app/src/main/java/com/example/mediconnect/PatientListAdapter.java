package com.example.mediconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PatientListAdapter extends RecyclerView.Adapter<PatientListAdapter.VH> {

    private final List<PatientItem> list;

    public PatientListAdapter(List<PatientItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PatientItem p = list.get(position);

        h.tvName.setText(p.patientName != null ? p.patientName : "Unknown");
        h.tvDob.setText("Birthday: " + (p.dob != null && !p.dob.isEmpty() ? p.dob : "N/A"));
        h.tvPhone.setText("Contact: " + (p.phone != null && !p.phone.isEmpty() ? p.phone : "N/A"));
        h.tvAppDate.setText(p.date + (p.time != null ? "  •  " + p.time : ""));
        h.tvType.setText(p.type != null ? p.type : "");
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDob, tvPhone, tvAppDate, tvType;

        VH(@NonNull View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvPatientName);
            tvDob     = v.findViewById(R.id.tvPatientDob);
            tvPhone   = v.findViewById(R.id.tvPatientPhone);
            tvAppDate = v.findViewById(R.id.tvAppointmentDate);
            tvType    = v.findViewById(R.id.tvAppointmentType);
        }
    }
}