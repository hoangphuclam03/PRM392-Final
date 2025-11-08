package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.ProjectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.repository.SyncRepository;
import com.example.prm392.models.ProjectEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class ListYourProjectsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ProjectAdapter adapter;
    private AppDatabase db;
    private SyncRepository syncRepo;

    private final String currentUserId = "USER001"; // Giả lập user đăng nhập

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_projects);

        // ---------------- Toolbar & Drawer Setup ----------------
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Tạo toggle để quản lý nút menu
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupNavigation();

        // ---------------- RecyclerView Setup ----------------
        recyclerView = findViewById(R.id.recyclerViewProjects);
        fabAdd = findViewById(R.id.fabAddProject);
        db = AppDatabase.getInstance(this);
        syncRepo = new SyncRepository(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, CreateProjectActivity.class)));

        refreshAndLoadProjects();
    }

    // ---------------- Navigation Drawer Action ----------------
    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Bạn đang ở: Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                recreate(); // Trang hiện tại
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                Toast.makeText(this, "Đăng xuất...", Toast.LENGTH_SHORT).show();
                // logoutUser(); nếu có
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ---------------- Firestore Sync ----------------
    private void refreshAndLoadProjects() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            syncRepo.refreshProjectsFromFirestore();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            List<ProjectEntity> projects = db.projectDAO().getProjectsByUser(currentUserId);
            runOnUiThread(() -> displayProjects(projects));
        });
    }

    private void displayProjects(List<ProjectEntity> projects) {
        if (projects == null || projects.isEmpty()) {
            Toast.makeText(this, "Chưa có project nào", Toast.LENGTH_SHORT).show();
            recyclerView.setAdapter(null);
        } else {
            adapter = new ProjectAdapter(
                    projects,
                    currentUserId,
                    project -> Toast.makeText(this, "Đã chọn: " + project.projectName, Toast.LENGTH_SHORT).show(),
                    project -> {
                        Intent intent = new Intent(this, EditProjectActivity.class);
                        intent.putExtra("projectId", project.projectId);
                        startActivity(intent);
                    },
                    this::deleteProject
            );
            recyclerView.setAdapter(adapter);
        }
    }

    private void deleteProject(ProjectEntity project) {
        runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xoá")
                .setMessage("Bạn có chắc muốn xoá project \"" + project.projectName + "\" không?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    Toast.makeText(this, "Đang xoá project...", Toast.LENGTH_SHORT).show();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            syncRepo.deleteProjectAndMembers(project.projectId);
                            db.projectDAO().delete(project);
                            db.projectMemberDAO().deleteByProject(project.projectId);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đã xoá project " + project.projectName, Toast.LENGTH_SHORT).show();
                                refreshAndLoadProjects();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Lỗi khi xoá: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Huỷ", null)
                .show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAndLoadProjects();
    }
}
