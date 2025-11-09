package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.PublicProjectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.models.ProjectEntity;
import com.google.android.material.color.MaterialColors;  // <<<< ADD
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListPublicProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PublicProjectAdapter adapter;
    private ProjectDAO projectDAO;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_public_project);
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        recyclerView   = findViewById(R.id.recyclerMembers);
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        setSupportActionBar(toolbar);

        // ====== APPEND: làm Toolbar hiện rõ ràng ======
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Danh sách Project");
        }
        int primary   = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorPrimary);
        int onPrimary = MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnPrimary);
        toolbar.setNavigationIcon(R.drawable.ic_menu_24); // icon 3 gạch của bạn
        toolbar.setNavigationOnClickListener(v -> drawerLayout.open()); // mở drawer
        // ==============================================

        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();

        projectDAO = AppDatabase.getInstance(this).projectDAO();
        loadProjects();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                drawerLayout.closeDrawers(); return true;
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Bạn đang ở: Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                drawerLayout.closeDrawers(); return true;
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
                adapter = new PublicProjectAdapter(projects, new PublicProjectAdapter.OnProjectClickListener() {
                    @Override public void onItemClick(ProjectEntity p) {
                        Intent i = new Intent(ListPublicProjectsActivity.this, ListMembersActivity.class);
                        i.putExtra("projectId", p.projectId); startActivity(i);
                    }
                    @Override public void onRequestJoinClick(ProjectEntity p) { sendJoinRequest(p); }
                });
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void sendJoinRequest(ProjectEntity project) {
        String uid = com.example.prm392.utils.FirebaseUtil.currentUserId();
        if (uid == null) { Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show(); return; }
        Map<String, Object> req = new HashMap<>();
        req.put("userId", uid);
        req.put("projectId", project.projectId);
        req.put("timestamp", System.currentTimeMillis());
        req.put("status", "pending");
        com.example.prm392.utils.FirebaseUtil.db.collection("join_requests")
                .add(req)
                .addOnSuccessListener(r -> Toast.makeText(this, "Đã gửi yêu cầu tham gia!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override protected void onResume() { super.onResume(); loadProjects(); }

    private void logoutUser() {
        Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class)); finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
