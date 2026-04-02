package com.example.mediconnect;

/**
 * Lightweight model for a single appointment document from Firestore.
 * Fields mirror your Firestore schema:
 *   appointmentId, date, doctorId, notes, patientId, status, time, type
 */
public class AppointmentItem {
    public String appointmentId;
    public String date;
    public String doctorId;
    public String notes;
    public String patientId;
    public String patientName;
    public String status;   // "confirmed" | "pending" | "cancelled"
    public String time;     // e.g. "09:00 AM"
    public String type;     // e.g. "Consultation", "Follow-up"

    public AppointmentItem() {}
}