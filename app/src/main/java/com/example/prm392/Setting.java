package com.example.prm392;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch; // import for the switch
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;


public class Setting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🌗 DARK THEME TOGGLE
        MaterialSwitch themeSwitch = findViewById(R.id.Theme);

        // Load previous state
        boolean isDark = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);

        themeSwitch.setChecked(isDark);
        AppCompatDelegate.setDefaultNightMode(isDark ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            getSharedPreferences("settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("dark_mode", isChecked)
                    .apply();

            // Apply theme instantly
            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES :
                    AppCompatDelegate.MODE_NIGHT_NO);
        });
    }
}
