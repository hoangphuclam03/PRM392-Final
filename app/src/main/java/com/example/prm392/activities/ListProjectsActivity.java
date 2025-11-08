package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.ProjectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.models.ProjectEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ProjectAdapter adapter;
    private ProjectDAO projectDAO;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_projects);

        // ---------------- ÁNH XẠ VIEW ----------------
        recyclerView = findViewById(R.id.recyclerProjects);
        fabAdd = findViewById(R.id.fabAddProject);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ẨN FAB NẾU Ở CHẾ ĐỘ JOIN (phải làm sau khi findViewById)
        String mode = getIntent().getStringExtra("mode");
        if ("join".equals(mode)) {
            fabAdd.setVisibility(View.GONE);
        }

        // ---------------- SETUP DRAWER TOGGLE ----------------
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

        // ---------------- INIT ROOM DATABASE ----------------
        projectDAO = AppDatabase.getInstance(this).projectDAO();

        // ---------------- LOAD DỮ LIỆU PROJECT ----------------
        loadProjects();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateManagingProjectActivity.class);
            startActivity(intent);
        });

        // ---------------- XỬ LÝ MENU BÊN TRÁI ----------------
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                drawerLayout.closeDrawers();
                return true;

            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Bạn đang ở: Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));

            } else if (id == R.id.nav_project) {
                // Already here
                drawerLayout.closeDrawers();
                return true;

            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));

            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));

            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ---------------- LOAD PROJECTS (duy nhất) ----------------
    private void loadProjects() {
        final boolean joinMode = "join".equals(getIntent().getStringExtra("mode"));

        executor.execute(() -> {
            List<ProjectEntity> projects = joinMode
                    ? projectDAO.getPublicProjects()
                    : projectDAO.getAllProjects();

            runOnUiThread(() -> {
                if (projects == null || projects.isEmpty()) {
                    Toast.makeText(this, joinMode ? "Chưa có dự án công khai." : "Chưa có dự án nào.", Toast.LENGTH_SHORT).show();
                }

                adapter = new ProjectAdapter(projects, new ProjectAdapter.OnProjectClickListener() {
                    @Override
                    public void onItemClick(ProjectEntity project) {
                        // Mở chi tiết, hoặc danh sách thành viên dự án
                        Intent intent = new Intent(ListProjectsActivity.this, ListMembersActivity.class);
                        intent.putExtra("projectId", project.projectId);
                        startActivity(intent);
                    }

                    @Override
                    public void onRequestJoinClick(ProjectEntity project) {
                        // Gửi join request
                        sendJoinRequest(project);
                    }
                });

                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void sendJoinRequest(ProjectEntity project) {
        String uid = com.example.prm392.utils.FirebaseUtil.currentUserId();
        if (uid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> req = new HashMap<>();
        req.put("userId", uid);
        req.put("projectId", project.projectId);
        req.put("timestamp", System.currentTimeMillis());
        req.put("status", "pending");

        com.example.prm392.utils.FirebaseUtil
                .db
                .collection("join_requests")
                .add(req)
                .addOnSuccessListener(r -> Toast.makeText(this, "Đã gửi yêu cầu tham gia!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ---------------- REFRESH KHI QUAY LẠI ----------------
    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    // ---------------- LOGOUT USER ----------------
    private void logoutUser() {
        Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ---------------- MỞ MENU KHI BẤM NÚT ☰ ----------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
