package com.example.prm392.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.prm392.R;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchDarkTheme, switchTimeFormat, switchNotifications;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_TIME_FORMAT_24H = "time_format_24h";
    private static final String KEY_ALL_NOTIFICATIONS = "all_notifications";

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ---------------- Apply saved dark theme BEFORE super.onCreate ----------------
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean darkTheme = preferences.getBoolean(KEY_DARK_THEME, false);
        AppCompatDelegate.setDefaultNightMode(
                darkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ---------------- Map Views ----------------
        switchDarkTheme = findViewById(R.id.switchDarkTheme);
        switchTimeFormat = findViewById(R.id.switchTimeFormat);
        switchNotifications = findViewById(R.id.switchNotifications);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open, R.string.close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // ---------------- Navigation Drawer ----------------

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_global_search) {
                startActivity(new Intent(this, GlobalSearchActivity.class));
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListYourProjectsActivity.class));
            } else if (id == R.id.nav_my_tasks) {
                startActivity(new Intent(this, ListTasksActivity.class)); // adjust name if different
            } else if (id == R.id.nav_settings) {
                drawerLayout.closeDrawers(); // already here
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // ---------------- Load Saved Settings ----------------
        switchDarkTheme.setChecked(preferences.getBoolean(KEY_DARK_THEME, false));
        switchTimeFormat.setChecked(preferences.getBoolean(KEY_TIME_FORMAT_24H, true));
        switchNotifications.setChecked(preferences.getBoolean(KEY_ALL_NOTIFICATIONS, true));

        // ---------------- Switch Listeners ----------------

        // Dark Theme
        switchDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_DARK_THEME, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            // Recreate the activity to apply theme instantly
            recreate();
        });

        // 12h/24h Time Format
        switchTimeFormat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_TIME_FORMAT_24H, isChecked).apply();
            // Optional: update any displayed time format
        });

        // All Notifications
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_ALL_NOTIFICATIONS, isChecked).apply();
            // Optional: enable/disable notification channels
        });
    }
}
