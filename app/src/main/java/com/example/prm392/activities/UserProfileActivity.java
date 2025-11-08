package com.example.prm392.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.prm392.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.util.Date;

public class UserProfileActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private ImageView ivAvatar;
    private TextView tvFullName, tvEmail, tvCreatedAt, tvLastLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ---------------- Apply theme BEFORE super ----------------
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = prefs.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // ---------------- Firebase ----------------
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ---------------- UI Setup ----------------
        initUI();
        setupDrawer();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserProfile(user.getUid());
        } else {
            Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // ---------------- Init UI ----------------
    private void initUI() {
        drawerLayout = findViewById(R.id.drawerLayoutProfile);
        navigationView = findViewById(R.id.navigationViewProfile);
        toolbar = findViewById(R.id.toolbarProfile);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvLastLogin = findViewById(R.id.tvLastLogin);

        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ✅ đánh dấu menu hiện tại
        navigationView.setCheckedItem(R.id.nav_profile);
    }

    // ---------------- Drawer Navigation ----------------
    private void setupDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else if (id == R.id.nav_profile) {
                drawerLayout.closeDrawers(); // đang ở đây rồi
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                finish();
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListYourProjectsActivity.class));
                finish();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
                finish();
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ---------------- Load User Profile ----------------
    private void loadUserProfile(String uid) {
        DocumentReference ref = db.collection("Users").document(uid);
        ref.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String fullName = document.getString("fullName");
                String firstName = document.getString("firstName");
                String lastName = document.getString("lastName");
                String email = document.getString("email");

                Long createdAt = document.getLong("createdAt");
                Long lastLogin = document.getLong("lastLogin");

                if (fullName != null) {
                    tvFullName.setText(fullName);
                } else if (firstName != null && lastName != null) {
                    tvFullName.setText(firstName + " " + lastName);
                } else {
                    tvFullName.setText("Người dùng");
                }

                tvEmail.setText(email != null ? email : "Không có email");

                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

                if (createdAt != null) {
                    tvCreatedAt.setText("Ngày tạo: " + df.format(new Date(createdAt)));
                } else {
                    tvCreatedAt.setText("Ngày tạo: -");
                }

                if (lastLogin != null) {
                    tvLastLogin.setText("Lần đăng nhập gần nhất: " + df.format(new Date(lastLogin)));
                } else {
                    tvLastLogin.setText("Lần đăng nhập gần nhất: -");
                }

            } else {
                Toast.makeText(this, "Không tìm thấy dữ liệu người dùng.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("PROFILE", "Error loading user: " + e.getMessage());
            Toast.makeText(this, "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
