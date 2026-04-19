package com.example.mediconnect;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PendingRequestsAdapter
        extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    public interface OnActionListener {
        void onAction(AppointmentItem item, int position, String action); // "accept" or "decline"
    }

    private final List<AppointmentItem> list;
    private final OnActionListener listener;

    public PendingRequestsAdapter(List<AppointmentItem> list, OnActionListener listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AppointmentItem item = list.get(position);
        h.tvPatientName.setText(item.patientName != null ? item.patientName : "Unknown");
        h.tvDateTime.setText(item.date + "  ·  " + item.time);
        h.tvType.setText(item.type != null ? item.type : "Consultation");

        h.btnAccept.setOnClickListener(v ->
                listener.onAction(item, h.getAdapterPosition(), "accept"));
        h.btnDecline.setOnClickListener(v ->
                listener.onAction(item, h.getAdapterPosition(), "decline"));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvDateTime, tvType;
        CardView btnAccept;
        TextView btnDecline;

        ViewHolder(View v) {
            super(v);
            tvPatientName = v.findViewById(R.id.tvPatientName);
            tvDateTime    = v.findViewById(R.id.tvDateTime);
            tvType        = v.findViewById(R.id.tvType);
            btnAccept     = v.findViewById(R.id.btnAccept);
            btnDecline    = v.findViewById(R.id.btnDecline);
        }
    }
}