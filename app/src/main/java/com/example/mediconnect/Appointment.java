package com.example.mediconnect;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Appointment {
    private String appointmentId;
    private String date;
    private String doctorId;
    private String notes;
    private String patientId;
    private String patientName;
    private String status; // "confirmed", "completed", "cancelled"
    private String time;
    private String type; // "Consultation", "Video Call", "In-Person"

    // Doctor info (fetched separately or embedded)
    private String doctorName;
    private String doctorSpecialty;
    private String doctorHospital;

    // Constructor for Firebase
    public Appointment() {}

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialty() { return doctorSpecialty; }
    public void setDoctorSpecialty(String doctorSpecialty) { this.doctorSpecialty = doctorSpecialty; }

    public String getDoctorHospital() { return doctorHospital; }
    public void setDoctorHospital(String doctorHospital) { this.doctorHospital = doctorHospital; }

    // Helper methods
    public boolean isUpcoming() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date appointmentDate = sdf.parse(date);
            Date today = new Date();
            return appointmentDate != null && !appointmentDate.before(today) && !status.equals("cancelled");
        } catch (ParseException e) {
            return false;
        }
    }

    public boolean isToday() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date appointmentDate = sdf.parse(date);
            Date today = new Date();
            return appointmentDate != null && sdf.format(appointmentDate).equals(sdf.format(today));
        } catch (ParseException e) {
            return false;
        }
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date dateObj = inputFormat.parse(date);
            return dateObj != null ? outputFormat.format(dateObj) : date;
        } catch (ParseException e) {
            return date;
        }
    }
}