package com.example.mediconnect;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_host);

        // Handle edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Bottom nav click handler
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            }
            else if (itemId == R.id.nav_find) {
                selectedFragment = new FindFragment();
            }
            else if (itemId == R.id.nav_appointments) {
                selectedFragment = new AppointmentsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Public method for fragments to access bottom nav if needed
    public BottomNavigationView getBottomNav() {
        return bottomNav;
    }
}