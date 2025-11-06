package com.example.prm392;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        // Gắn Toolbar vào ActionBar
        setSupportActionBar(toolbar);

        // Tạo toggle để mở/đóng Drawer
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Xin chào, " + user.getEmail());
        }

        btnLogout.setOnClickListener(v -> logoutUser());

        // Sự kiện click menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                tvWelcome.setText("Bạn đang ở: Trang chủ");
            } else if (id == R.id.nav_profile) {
                tvWelcome.setText("Bạn đang ở: Hồ sơ cá nhân");
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(HomeActivity.this, ChatActivity.class));
            } else if (id == R.id.nav_settings) {
                tvWelcome.setText("Bạn đang ở: Cài đặt");
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
