package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private TextView userName, userAvatar, greetingText;
    private View settingsIcon;
    private CardView actionVideoConsult, actionAIChat, actionFindNearby, actionMyRecords;
    private CardView appointmentCard;
    private MaterialButton joinButton;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        userName = view.findViewById(R.id.user_name);
        userAvatar = view.findViewById(R.id.user_avatar);
        greetingText = view.findViewById(R.id.greeting_text);
        settingsIcon = view.findViewById(R.id.settings_icon);

        actionVideoConsult = view.findViewById(R.id.action_video_consult);
        actionAIChat = view.findViewById(R.id.action_ai_chat);
        actionFindNearby = view.findViewById(R.id.action_find_nearby);
        actionMyRecords = view.findViewById(R.id.action_my_records);

        appointmentCard = view.findViewById(R.id.appointment_card);
        joinButton = view.findViewById(R.id.join_button);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // Set greeting based on time
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour < 12) greeting = "Good morning,";
            else if (hour < 18) greeting = "Good afternoon,";
            else greeting = "Good evening,";

            greetingText.setText(greeting);

            if (name != null && !name.isEmpty()) {
                userName.setText(name);
                String initial = name.substring(0, 1).toUpperCase();
                userAvatar.setText(initial);
            } else if (email != null) {
                userName.setText(email.split("@")[0]);
                String initial = email.substring(0, 1).toUpperCase();
                userAvatar.setText(initial);
            }
        }
    }

    private void setupClickListeners() {
        settingsIcon.setOnClickListener(v -> showToast("Opening settings..."));

        actionVideoConsult.setOnClickListener(v -> showToast("🎥 Video Consult coming soon!"));

        actionAIChat.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AiMedicalActivity.class);
            startActivity(intent);
        });

        actionFindNearby.setOnClickListener(v -> {
            // Switch to Find tab
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).getBottomNav().setSelectedItemId(R.id.nav_find);
            }
        });

        actionMyRecords.setOnClickListener(v -> showToast("📋 My Records"));

        appointmentCard.setOnClickListener(v -> showToast("📅 Opening appointment..."));

        joinButton.setOnClickListener(v -> showToast("🚀 Starting video call..."));
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}