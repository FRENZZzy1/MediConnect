package com.example.mediconnect;

public class Doctor {


    public String uid;
    public String yearsOfExperience;

    public String fullName;
    public String email;
    public String prcLicense;
    public String specialization;
    public String availableDays;
    public String consultationHours;
    public String consultationFee;
    public String clinicName;
    public String location;
    public boolean isAvailable; // ← true = accepting patients, false = not accepting

    public Doctor() {}

    public Doctor(String fullName, String email, String prcLicense,
                  String specialization, String availableDays,
                  String consultationHours, String consultationFee,
                  String clinicName, String location,
                  boolean isAvailable) {

        this.fullName          = fullName;
        this.email             = email;
        this.prcLicense        = prcLicense;
        this.specialization    = specialization;
        this.availableDays     = availableDays;
        this.consultationHours = consultationHours;
        this.consultationFee   = consultationFee;
        this.clinicName        = clinicName;
        this.location          = location;
        this.isAvailable       = isAvailable;
    }
}