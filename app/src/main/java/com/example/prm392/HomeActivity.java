package com.example.prm392;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

import data.local.DBConnect;
import data.repository.SyncRepository;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 🔹 Initialize Firebase + SQLite
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        DBConnect localDb = new DBConnect(this);

        // 🔹 Optional: insert a test project locally BEFORE syncing
        Projects localTest = new Projects();
        localTest.setProjectId(1001);
        localTest.setProjectName("Offline Project Test");
        localTest.setDescription("Created locally on device");
        localTest.setCreatedBy(1);
        localTest.setCreatedAt("2025-11-05");
        localDb.insertOrUpdateProject(localTest);
        Log.d("HOME", "Inserted local test project for sync verification");

        // 🔹 Run sync tests (SQLite → Firebase → SQLite)
        SyncRepository syncRepo = new SyncRepository(this);
        syncRepo.syncProjectsToFirebase();     // Upload local → Firebase
        syncRepo.syncProjectsFromFirebase();   // Download Firebase → local

        // 🔹 Map UI
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 🔹 Check logged-in user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserInfo(user.getUid());
            updateLastLogin(user.getUid());
        } else {
            Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // 🔹 Logout button
        btnLogout.setOnClickListener(v -> logoutUser());

        // 🔹 Navigation menu logic
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                tvWelcome.setText("Bạn đang ở: Trang chủ");
            } else if (id == R.id.nav_profile) {
                tvWelcome.setText("Bạn đang ở: Hồ sơ cá nhân");
            } else if (id == R.id.nav_settings) {
                tvWelcome.setText("Bạn đang ở: Cài đặt");
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    // 🔹 Load user info from Firestore
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

    // 🔹 Update last login timestamp
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
