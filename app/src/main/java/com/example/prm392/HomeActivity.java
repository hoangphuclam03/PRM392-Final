package com.example.prm392;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import data.local.DBConnect;

import com.example.prm392.data.repository.SyncRepository;
import com.example.prm392.data.workers.SyncWorker;
import models.Projects;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // SharedPreferences keys
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ---------------- Apply saved dark theme BEFORE super.onCreate ----------------
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ---------------- Initialize Firebase + SQLite ----------------
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        DBConnect localDb = new DBConnect(this);

        // Optional: insert a test project locally BEFORE syncing
        //Projects localTest = new Projects();
        //localTest.setProjectId(1001);
        //localTest.setProjectName("Offline Project Test");
        //localTest.setDescription("Created locally on device");
        //localTest.setCreatedBy(1);
        //localTest.setCreatedAt("2025-11-05");
        //localDb.insertOrUpdateProject(localTest);
        //Log.d("HOME", "Inserted local test project for sync verification");

        // ---------------- Run initial sync (SQLite → Firebase → SQLite) ----------------
        SyncRepository syncRepo = new SyncRepository(this);
        syncRepo.syncProjectsToFirestore();
        syncRepo.syncProjectsFromFirestore();

        // ---------------- Schedule periodic background sync with WorkManager ----------------
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueue(syncWorkRequest);
        Log.d("HOME", "Periodic WorkManager sync scheduled every 15 minutes");

        // ---------------- Map UI ----------------
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ---------------- Check logged-in user ----------------
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserInfo(user.getUid());
            updateLastLogin(user.getUid());
        } else {
            Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // ---------------- Logout button ----------------
        btnLogout.setOnClickListener(v -> logoutUser());

        // ---------------- Navigation menu logic ----------------
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Recreate activity to apply theme in case changed
                recreate();
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_profile) {
                tvWelcome.setText("Bạn đang ở: Hồ sơ cá nhân");
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(HomeActivity.this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(HomeActivity.this, ListProjectsActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_logout) {
                logoutUser();
            } else if (id == R.id.nav_calendar) {  // <-- ID của mục “Lịch”
            startActivity(new Intent(HomeActivity.this, CalendarEventsActivity.class));
            drawerLayout.closeDrawers();
            return true; // nhớ return true để sự kiện không bị rơi
        }

        drawerLayout.closeDrawers();
            return true;
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
            recreate(); // refresh activity to apply theme
        }
    }

    // ---------------- Load user info from Firestore ----------------
    private void loadUserInfo(String uid) {
        DocumentReference ref = db.collection("Users").document(uid);
        ref.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String firstName = document.getString("firstName");
                String lastName = document.getString("lastName");
                String email = document.getString("email");

                if (firstName != null && lastName != null) {
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

    // ---------------- Update last login timestamp ----------------
    private void updateLastLogin(String uid) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastLogin", System.currentTimeMillis());

        db.collection("Users").document(uid)
                .set(update, SetOptions.merge())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không thể cập nhật Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
