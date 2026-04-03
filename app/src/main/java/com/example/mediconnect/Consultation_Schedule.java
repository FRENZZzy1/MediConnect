package com.example.mediconnect;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallService;
import com.zegocloud.uikit.prebuilt.call.invite.ZegoUIKitPrebuiltCallInvitationConfig;
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton;
import com.zegocloud.uikit.service.defines.ZegoUIKitUser;

import java.util.ArrayList;
import java.util.List;

public class Consultation_Schedule extends AppCompatActivity {

    private static final long   ZEGO_APP_ID   = 503023280L;
    private static final String ZEGO_APP_SIGN = "bdfa45fc67d54e89dbfd857cf20ca18091269a18d28367e25f98bc84d214f5ef";

    private LinearLayout      consultationContainer;
    private TextView          tvEmpty;
    private FirebaseFirestore db;
    private String            patientId;
    private String            patientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_consultation_schedule);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        consultationContainer = findViewById(R.id.consultationContainer);
        tvEmpty               = findViewById(R.id.tvEmpty);
        db                    = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("User not logged in.");
            return;
        }
        patientId = currentUser.getUid();

        // Fetch patient name first so Zego shows it on the doctor's incoming-call popup
        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(patientId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String first = snapshot.child("firstName").getValue(String.class);
                    String last  = snapshot.child("lastName").getValue(String.class);
                    patientName  = (nn(first) + " " + nn(last)).trim();
                    if (patientName.isEmpty()) patientName = patientId;

                    initZegoAndLoadCards();
                })
                .addOnFailureListener(e -> {
                    patientName = patientId; // fallback to UID
                    initZegoAndLoadCards();
                });
    }

    private void initZegoAndLoadCards() {
        // Init Zego with patient's Firebase UID as the Zego userID.
        // The doctor's Firebase UID will be the invitee — both sides
        // use their own UID so no ID mapping is ever needed.
        ZegoUIKitPrebuiltCallService.init(
                getApplication(),
                ZEGO_APP_ID,
                ZEGO_APP_SIGN,
                patientId,   // Zego userID  = Firebase Auth UID
                patientName, // shown on the doctor's incoming-call screen
                new ZegoUIKitPrebuiltCallInvitationConfig()
        );

        loadConsultations();
    }

    private void loadConsultations() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Loading consultations...");

        db.collection("appointments")
                .whereEqualTo("patientId", patientId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    consultationContainer.removeAllViews();

                    if (snapshots.isEmpty()) {
                        tvEmpty.setText("🗓️ No consultations found");
                        return;
                    }
                    tvEmpty.setVisibility(View.GONE);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String doctorId = doc.getString("doctorId");
                        String date     = doc.getString("date");
                        String time     = doc.getString("time");
                        String notes    = doc.getString("notes");
                        String type     = doc.getString("type");

                        if (doctorId == null) continue;

                        // Fetch doctor name + specialization from RTDB
                        FirebaseDatabase.getInstance()
                                .getReference("Doctors")
                                .child(doctorId)
                                .get()
                                .addOnSuccessListener(ds -> addConsultationCard(
                                        ds.child("fullName").getValue(String.class),
                                        ds.child("specialization").getValue(String.class),
                                        date, time, notes, type, doctorId
                                ))
                                .addOnFailureListener(e -> addConsultationCard(
                                        null, null, date, time, notes, type, doctorId
                                ));
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("❌ Failed to load consultations");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addConsultationCard(String doctorName, String specialization,
                                     String date, String time,
                                     String notes, String type,
                                     String doctorId) {

        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_consultation_card, consultationContainer, false);

        String displayName = (doctorName != null) ? doctorName : "Unknown Doctor";
        String displaySpec = (specialization != null) ? specialization
                : (type != null)           ? type
                : "GP TELECALL";

        ((TextView) card.findViewById(R.id.tvDoctorName)).setText(displayName);
        ((TextView) card.findViewById(R.id.tvAvatar))
                .setText(String.valueOf(displayName.charAt(0)).toUpperCase());
        ((TextView) card.findViewById(R.id.tvSpecialization))
                .setText(displaySpec.toUpperCase());
        ((TextView) card.findViewById(R.id.tvDateTime))
                .setText(nn(date) + "  ·  " + nn(time));
        ((TextView) card.findViewById(R.id.tvNotes))
                .setText((notes != null && !notes.isEmpty()) ? notes : "No notes");

        // Wire the call button — doctorId IS the doctor's Zego userID.
        // Zego handles outgoing ring, the doctor's incoming popup,
        // accept/decline, and room join — nothing else needed on this side.
        ZegoSendCallInvitationButton btnVideoCall = card.findViewById(R.id.btnVideoCall);
        if (btnVideoCall != null) {
            List<ZegoUIKitUser> invitees = new ArrayList<>();
            invitees.add(new ZegoUIKitUser(doctorId, displayName));
            btnVideoCall.setInvitees(invitees);
            btnVideoCall.setIsVideoCall(true);
        }

        consultationContainer.addView(card);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZegoUIKitPrebuiltCallService.unInit();
    }

    private static String nn(String s) { return s != null ? s : ""; }
}