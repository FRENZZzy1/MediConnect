package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ENHANCED DASHBOARD ACTIVITY
 *
 * This is the main dashboard screen with improved functionality:
 * - User profile information
 * - Health status card
 * - Quick action buttons (Video Consult, AI Chat, Find Nearby, My Records)
 * - Upcoming appointments
 * - Recent Activity section (NEW)
 * - Daily Health Tips (NEW)
 * - Medication Reminders (NEW)
 * - Bottom navigation bar
 *
 * IMPROVEMENTS:
 * - Fixed oval drawable error
 * - Added more scrollable content
 * - Better click handling
 * - Enhanced user feedback
 */
public class Dashboard extends AppCompatActivity {

    // ═══════════════════════════════════════════════════════════
    // DECLARE UI COMPONENTS
    // ═══════════════════════════════════════════════════════════

    // Header elements
    private TextView userName;
    private TextView userAvatar;
    private View settingsIcon;

    // Quick action cards
    private CardView actionVideoConsult;
    private CardView actionAIChat;
    private CardView actionFindNearby;
    private CardView actionMyRecords;

    // Appointment section
    private CardView appointmentCard;
    private MaterialButton joinButton;

    // Bottom navigation
    private BottomNavigationView bottomNav;

    // Firebase Authentication instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display (removes status bar background)
        EdgeToEdge.enable(this);

        // Set the layout for this activity
        setContentView(R.layout.activity_dashboard);

        // Handle system bars (status bar, navigation bar) padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize all UI components
        initializeViews();

        // Load user data and display it
        loadUserData();

        // Set up click listeners for all interactive elements
        setupClickListeners();
    }

    /**
     * INITIALIZE VIEWS
     *
     * Gets references to all UI elements from the XML layout.
     */
    private void initializeViews() {
        // Header elements
        userName = findViewById(R.id.user_name);
        userAvatar = findViewById(R.id.user_avatar);
        settingsIcon = findViewById(R.id.settings_icon);

        // Quick action cards
        actionVideoConsult = findViewById(R.id.action_video_consult);
        actionAIChat = findViewById(R.id.action_ai_chat);
        actionFindNearby = findViewById(R.id.action_find_nearby);
        actionMyRecords = findViewById(R.id.action_my_records);

        // Appointment section
        appointmentCard = findViewById(R.id.appointment_card);
        joinButton = findViewById(R.id.join_button);

        // Bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    /**
     * LOAD USER DATA
     *
     * Retrieves the current user's information from Firebase
     * and displays it in the UI.
     */
    private void loadUserData() {
        // Get the current logged-in user from Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is logged in
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // If name exists, display it
            if (name != null && !name.isEmpty()) {
                userName.setText(name);

                // Get first letter of name for avatar
                String initial = name.substring(0, 1).toUpperCase();
                userAvatar.setText(initial);
            } else if (email != null) {
                // If no name, use email
                userName.setText(email.split("@")[0]); // Get part before @

                // Get first letter of email for avatar
                String initial = email.substring(0, 1).toUpperCase();
                userAvatar.setText(initial);
            }
        } else {
            // User is not logged in, redirect to login
            Intent intent = new Intent(Dashboard.this, Login.class);
            startActivity(intent);
            finish(); // Close this activity so user can't go back
        }
    }

    /**
     * SETUP CLICK LISTENERS
     *
     * Sets up what happens when the user clicks on buttons
     * and cards in the dashboard.
     */
    private void setupClickListeners() {

        // ───────────────────────────────────────────────────
        // SETTINGS ICON
        // ───────────────────────────────────────────────────
        settingsIcon.setOnClickListener(v -> {
            showToast("Opening settings...");
            // TODO: Create SettingsActivity
            // Intent intent = new Intent(Dashboard.this, SettingsActivity.class);
            // startActivity(intent);
        });

        // ───────────────────────────────────────────────────
        // VIDEO CONSULT ACTION
        // ───────────────────────────────────────────────────
        actionVideoConsult.setOnClickListener(v -> {
            showToast("🎥 Video Consult feature coming soon!");
            // TODO: Create VideoConsultActivity
        });

        // ───────────────────────────────────────────────────
        // AI MEDICAL CHAT ACTION
        // ───────────────────────────────────────────────────
        actionAIChat.setOnClickListener(v -> {
            Intent intent = new Intent(Dashboard.this, AiMedicalActivity.class);
            startActivity(intent);
        });

        // ───────────────────────────────────────────────────
        // FIND NEARBY CLINICS ACTION
        // ───────────────────────────────────────────────────
        actionFindNearby.setOnClickListener(v -> {
            showToast("📍 Find Nearby feature coming soon!");
        });

        // ───────────────────────────────────────────────────
        // MY RECORDS ACTION
        // ───────────────────────────────────────────────────
        actionMyRecords.setOnClickListener(v -> {
            showToast("📋 My Records feature coming soon!");
            // TODO: Create MyRecordsActivity
            // Intent intent = new Intent(Dashboard.this, MyRecordsActivity.class);
            // startActivity(intent);
        });

        // ───────────────────────────────────────────────────
        // APPOINTMENT CARD ACTION
        // ───────────────────────────────────────────────────
        appointmentCard.setOnClickListener(v -> {
            showToast("📅 Opening appointment details...");
            // TODO: Create AppointmentDetailsActivity
            // Intent intent = new Intent(Dashboard.this, AppointmentDetailsActivity.class);
            // startActivity(intent);
        });

        // ───────────────────────────────────────────────────
        // JOIN APPOINTMENT BUTTON
        // ───────────────────────────────────────────────────
        joinButton.setOnClickListener(v -> {
            showToast("🚀 Starting video call...");
            // TODO: Start video call for the appointment
            // Intent intent = new Intent(Dashboard.this, VideoCallActivity.class);
            // startActivity(intent);
        });

        // ───────────────────────────────────────────────────
        // BOTTOM NAVIGATION
        // ───────────────────────────────────────────────────
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on home, do nothing
                return true;

            } else if (itemId == R.id.nav_find) {
                showToast("🔍 Find Doctors");
                return true;

            } else if (itemId == R.id.nav_appointments) {
                showToast("📅 My Appointments");
                return true;


            } else if (itemId == R.id.nav_profile) {
                showToast("👤 My Profile");
                return true;
            }

            return false;
        });

        // Set Home as selected by default
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /**
     * SHOW TOAST MESSAGE
     *
     * Helper method to display a quick message to the user.
     *
     * @param message The message to display
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * HANDLE BACK BUTTON
     *
     * Override to prevent accidental exit from dashboard.
     * Press back twice to exit.
     */
    private long backPressedTime = 0;

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            finish();
        } else {
            showToast("Press back again to exit");
            backPressedTime = System.currentTimeMillis();
        }
    }

    public void AI_CHAT(View view){
        Intent intent = new Intent(this, AiMedicalActivity.class);
        startActivity(intent);
    }

    public void Nearby(View view){
        Intent intent = new Intent(this, FindNearbyActivity.class);
        startActivity(intent);
    }





}