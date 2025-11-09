package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.UserEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.UUID;

public class ListYourProjectsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ProjectAdapter adapter;
    private AppDatabase db;
    private SyncRepository syncRepo;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_projects);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupNavigation();

        recyclerView = findViewById(R.id.recyclerViewProjects);
        fabAdd = findViewById(R.id.fabAddProject);
        db = AppDatabase.getInstance(this);
        syncRepo = new SyncRepository(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, CreateProjectActivity.class)));

        loadCurrentUserAndProjects();
    }

    private void setupNavigation() {
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

    private void loadCurrentUserAndProjects() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            UserEntity currentUser = db.userDAO().getLastLoggedInUser();
            if (currentUser == null || currentUser.userId == null) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Không tìm thấy người dùng hiện tại. Vui lòng đăng nhập lại.",
                        Toast.LENGTH_LONG).show());
                return;
            }

            currentUserId = currentUser.userId;
            refreshAndLoadProjects();
        });
    }

    private void refreshAndLoadProjects() {
        if (currentUserId == null) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            syncRepo.refreshProjectsFromFirestore();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            List<ProjectEntity> projects = db.projectDAO().getAll(); // ✅ hiện tất cả project
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
                    project -> { // click vào item
                        Intent intent = new Intent(this, ProjectMembersActivity.class);
                        intent.putExtra("projectId", project.projectId);
                        intent.putExtra("projectName", project.projectName);
                        intent.putExtra("isPublic", project.isPublic);
                        startActivity(intent);
                    },
                    project -> { // edit
                        Intent intent = new Intent(this, EditProjectActivity.class);
                        intent.putExtra("projectId", project.projectId);
                        startActivity(intent);
                    },
                    this::deleteProject,
                    this::joinPublicProject // ✅ truyền đúng số lượng (6 tham số)
            );

            recyclerView.setAdapter(adapter);
        }
    }

    private void deleteProject(ProjectEntity project) {
        runOnUiThread(() -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận xoá")
                .setMessage("Bạn có chắc muốn xoá project \"" + project.projectName + "\" không?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            syncRepo.deleteProjectAndMembers(project.projectId);
                            db.projectDAO().delete(project);
                            db.projectMemberDAO().deleteByProject(project.projectId);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đã xoá project", Toast.LENGTH_SHORT).show();
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

    private void joinPublicProject(ProjectEntity project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProjectMemberEntity existing = db.projectMemberDAO()
                    .getMemberByProjectAndUser(project.projectId, currentUserId);

            if (existing != null) {
                runOnUiThread(() -> Toast.makeText(this, "Bạn đã là thành viên của project này", Toast.LENGTH_SHORT).show());
                return;
            }

            UserEntity user = db.userDAO().getLastLoggedInUser();
            if (user == null) {
                runOnUiThread(() -> Toast.makeText(this, "Không xác định user", Toast.LENGTH_SHORT).show());
                return;
            }

            ProjectMemberEntity newMember = new ProjectMemberEntity();
            newMember.memberId = UUID.randomUUID().toString();
            newMember.projectId = project.projectId;
            newMember.userId = currentUserId;
            newMember.fullName = user.fullName;
            newMember.role = "Member";
            newMember.pendingSync = true;

            db.projectMemberDAO().insertOrUpdate(newMember);
            new SyncRepository(this).syncMembersToFirestore();

            runOnUiThread(() -> Toast.makeText(this,
                    "Đã tham gia project " + project.projectName, Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) refreshAndLoadProjects();
    }
}
