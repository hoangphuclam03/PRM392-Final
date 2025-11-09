package com.example.prm392.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.repository.SyncRepository;
import com.example.prm392.data.workers.SyncWorker;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SyncRepository syncRepository;

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initUI();
        setupNavigation();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserInfo(user.getUid());
            updateLastLogin(user.getUid());
        } else {
            Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnLogout.setOnClickListener(v -> logoutUser());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase.getInstance(getApplicationContext());
                syncRepository = new SyncRepository(getApplicationContext());
                syncRepository.syncAll();
                schedulePeriodicSync();
                Log.d("HOME", "Initial and periodic sync scheduled successfully.");
            } catch (Exception e) {
                Log.e("HOME", "Background init failed: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        int currentMode = getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if ((darkTheme && currentMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) ||
                (!darkTheme && currentMode == android.content.res.Configuration.UI_MODE_NIGHT_YES)) {
            recreate();
        }

        if (syncRepository != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    syncRepository.syncAll();
                } catch (Exception e) {
                    Log.e("HOME", "Sync on resume failed: " + e.getMessage());
                }
            });
        }
    }

    private void initUI() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setCheckedItem(R.id.nav_home);

        // ✅ Make global search non-checkable so it doesn’t stay highlighted
        navigationView.getMenu().findItem(R.id.nav_global_search).setCheckable(false);
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_global_search) {
                startActivity(new Intent(this, GlobalSearchActivity.class));
            } else if (id == R.id.nav_home) {
                recreate();
            }
            else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
            }
            else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            }
            else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListYourProjectsActivity.class));
            }
            else if (id == R.id.nav_my_tasks) {
                startActivity(new Intent(this, ListTasksActivity.class)); // 🔹 adjust activity name if different
            }
            else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            }
            else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void loadUserInfo(String uid) {
        DocumentReference ref = db.collection("Users").document(uid);
        ref.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String fullName = document.getString("fullName");
                String firstName = document.getString("firstName");
                String lastName = document.getString("lastName");
                String email = document.getString("email");

                if (fullName != null && !fullName.isEmpty()) {
                    tvWelcome.setText("Xin chào, " + fullName + "!");
                } else if (firstName != null && lastName != null) {
                    tvWelcome.setText("Xin chào, " + firstName + " " + lastName + "!");
                } else if (email != null) {
                    tvWelcome.setText("Xin chào, " + email);
                } else {
                    tvWelcome.setText("Xin chào người dùng!");
                }
            } else {
                tvWelcome.setText("Xin chào người dùng mới!");
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void updateLastLogin(String uid) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastLogin", System.currentTimeMillis());

        db.collection("Users").document(uid)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không thể cập nhật Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15,
                TimeUnit.MINUTES
        ).setConstraints(constraints).build();

        WorkManager.getInstance(getApplicationContext()).enqueue(syncWorkRequest);
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
