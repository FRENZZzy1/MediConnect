package com.example.mediconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PatientListActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        bottomNav = findViewById(R.id.bottomNav);

        // ViewPager with 2 tabs
        viewPager.setAdapter(new PatientPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Upcoming / Current" : "Past Patients");
        }).attach();

        setupBottomNav();
    }

    // ─── ViewPager Adapter ────────────────────────────────────────────────────

    static class PatientPagerAdapter extends FragmentStateAdapter {
        PatientPagerAdapter(FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0
                    ? PatientListFragment.newInstance(false)
                    : PatientListFragment.newInstance(true);
        }

        @Override
        public int getItemCount() { return 2; }
    }

    // ─── Fragment (shared for both tabs) ──────────────────────────────────────

    public static class PatientListFragment extends Fragment {

        private static final String ARG_PAST = "past";

        public static PatientListFragment newInstance(boolean showPast) {
            PatientListFragment f = new PatientListFragment();
            Bundle args = new Bundle();
            args.putBoolean(ARG_PAST, showPast);
            f.setArguments(args);
            return f;
        }

        private RecyclerView recyclerView;
        private View layoutEmpty;
        private final List<PatientItem> patientList = new ArrayList<>();
        private PatientListAdapter adapter;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            // Inline layout: RecyclerView + empty state
            View root = inflater.inflate(R.layout.fragment_patient_list, container, false);
            recyclerView = root.findViewById(R.id.rvPatients);
            layoutEmpty  = root.findViewById(R.id.layoutEmptyPatients);

            adapter = new PatientListAdapter(patientList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);

            boolean showPast = getArguments() != null && getArguments().getBoolean(ARG_PAST, false);
            loadPatients(showPast);

            return root;
        }

        private void loadPatients(boolean past) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            // Build query: confirmed appointments for this doctor
            // Past  → date < today
            // Current/Upcoming → date >= today
            Query query = firestore.collection("appointments")
                    .whereEqualTo("doctorId", user.getUid())
                    .whereEqualTo("status", "confirmed");

            if (past) {
                query = query.whereLessThan("date", today)
                        .orderBy("date", Query.Direction.DESCENDING);
            } else {
                query = query.whereGreaterThanOrEqualTo("date", today)
                        .orderBy("date", Query.Direction.ASCENDING);
            }

            query.get()
                    .addOnSuccessListener(querySnapshot -> {
                        patientList.clear();
                        adapter.notifyDataSetChanged();

                        if (querySnapshot.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            return;
                        }

                        layoutEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            PatientItem item = new PatientItem();
                            item.appointmentId = doc.getString("appointmentId");
                            item.patientId     = doc.getString("patientId");
                            item.patientName   = doc.getString("patientName");
                            item.date          = doc.getString("date");
                            item.time          = doc.getString("time");
                            item.type          = doc.getString("type");
                            item.status        = doc.getString("status");

                            // Fetch dob + phone from Realtime DB
                            fetchUserDetails(item);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null)
                            Toast.makeText(getContext(),
                                    "Failed to load patients: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                    });
        }

        private void fetchUserDetails(PatientItem item) {
            if (item.patientId == null) {
                addToList(item);
                return;
            }

            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(item.patientId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        item.dob   = snapshot.child("dob").getValue(String.class);
                        item.phone = snapshot.child("phone").getValue(String.class);

                        // Build full name from Realtime DB if not already in Firestore
                        if (item.patientName == null || item.patientName.isEmpty()) {
                            String first = snapshot.child("firstName").getValue(String.class);
                            String last  = snapshot.child("lastName").getValue(String.class);
                            item.patientName = ((first != null ? first : "") + " "
                                    + (last != null ? last : "")).trim();
                        }
                        addToList(item);
                    })
                    .addOnFailureListener(e -> addToList(item));
        }

        private void addToList(PatientItem item) {
            patientList.add(item);
            adapter.notifyItemInserted(patientList.size() - 1);
        }
    }

    // ─── Bottom Navigation ────────────────────────────────────────────────────

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_patients);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_patients) return true;
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, Doctor_Dashboard.class));
                finish();
                return true;
            }
            if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, ScheduleActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_profile) return true;
            return false;
        });
    }
}