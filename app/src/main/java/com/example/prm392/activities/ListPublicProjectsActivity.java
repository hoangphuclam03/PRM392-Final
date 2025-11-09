package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.PublicProjectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.utils.FirebaseUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_public_project);

        // ---------------- ÁNH XẠ VIEW ----------------
        recyclerView = findViewById(R.id.recyclerMembers);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        // ---------------- INIT DATABASE ----------------
        projectDAO = AppDatabase.getInstance(this).projectDAO();

        // ---------------- LOAD PROJECTS ----------------
        loadProjects();

        // ---------------- MENU NAVIGATION ----------------
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
                startActivity(new Intent(this, ListTasksActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ---------------- LOAD PROJECTS ----------------
    private void loadProjects() {
        executor.execute(() -> {

            List<ProjectEntity> projects = projectDAO.getPublicProjects();

            runOnUiThread(() -> {
                if (projects == null || projects.isEmpty()) {
                    Toast.makeText(this, "Chưa có dự án công khai.", Toast.LENGTH_SHORT).show();
                }

                adapter = new PublicProjectAdapter(projects, new PublicProjectAdapter.OnProjectClickListener() {

                    // ✅ CLICK → MỞ CHAT (ONLY)
                    @Override
                    public void onItemClick(ProjectEntity project) {

                        Intent it = new Intent(ListPublicProjectsActivity.this, ChatActivity.class);
                        it.putExtra("teamId", project.projectId);
                        it.putExtra("teamName", project.projectName);
                        startActivity(it);
                    }

                    // ✅ Không dùng nữa → để trống
                    @Override
                    public void onRequestJoinClick(ProjectEntity project) {
                        // BỎ HOÀN TOÀN JOIN
                    }
                });

                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
