package com.example.mediconnect;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private int currentTabPosition = 0;

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
            loadFragment(new HomeFragment(), false, getTabPosition(R.id.nav_home));
            bottomNav.setSelectedItemId(R.id.nav_home);
            currentTabPosition = getTabPosition(R.id.nav_home);
        }

        // Bottom nav click handler
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            int nextTabPosition = getTabPosition(itemId);

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
                loadFragment(selectedFragment, true, nextTabPosition);
                currentTabPosition = nextTabPosition;
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, boolean animate, int nextTabPosition) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (animate) {
            if (nextTabPosition > currentTabPosition) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                );
            } else if (nextTabPosition < currentTabPosition) {
                transaction.setCustomAnimations(
                        R.anim.slide_in_left,
                        R.anim.slide_out_right,
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                );
            }
        }

        transaction
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private int getTabPosition(int itemId) {
        if (itemId == R.id.nav_home) {
            return 0;
        } else if (itemId == R.id.nav_find) {
            return 1;
        } else if (itemId == R.id.nav_appointments) {
            return 2;
        } else if (itemId == R.id.nav_profile) {
            return 3;
        }
        return currentTabPosition;
    }

    // Public method for fragments to access bottom nav if needed
    @SuppressWarnings("unused")
    public BottomNavigationView getBottomNav() {
        return bottomNav;
    }
}