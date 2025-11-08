package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.ProjectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListYourProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private ProjectAdapter adapter;

    private ProjectDAO projectDAO;
    private ProjectMemberDAO memberDAO;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // 🔹 Simulate current logged-in user (replace later with SharedPreferences or Firebase UID)
    private String currentUserId = "user_1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_your_projects);

        recyclerView = findViewById(R.id.recyclerYourProjects);
        fabAdd = findViewById(R.id.fabAddProject);

        Toolbar toolbar = findViewById(R.id.toolbarYourProjects);
        setSupportActionBar(toolbar);

        AppDatabase db = AppDatabase.getInstance(this);
        projectDAO = db.projectDAO();
        memberDAO = db.projectMemberDAO();

        loadYourProjects();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateManagingProjectActivity.class);
            startActivity(intent);
        });
    }

    private void loadYourProjects() {
        executor.execute(() -> {
            // 🔹 Step 1: Find all project IDs where current user is a member
            List<ProjectMemberEntity> memberRecords = memberDAO.getMembersByUser(currentUserId);
            if (memberRecords == null || memberRecords.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Bạn chưa tham gia dự án nào!", Toast.LENGTH_SHORT).show());
                return;
            }

            // Collect unique project IDs
            Set<String> projectIds = new HashSet<>();
            for (ProjectMemberEntity pm : memberRecords) {
                projectIds.add(pm.projectId);
            }

            // 🔹 Step 2: Fetch matching projects
            List<ProjectEntity> userProjects = projectDAO.getProjectsByIds(new ArrayList<>(projectIds));

            runOnUiThread(() -> {
                if (userProjects == null || userProjects.isEmpty()) {
                    Toast.makeText(this, "Không tìm thấy dự án nào.", Toast.LENGTH_SHORT).show();
                    return;
                }

                adapter = new ProjectAdapter(userProjects, new ProjectAdapter.OnProjectClickListener() {
                    @Override
                    public void onItemClick(ProjectEntity project) {
                        // xử lý click item
                    }

                    @Override
                    public void onRequestJoinClick(ProjectEntity project) {
                        // nếu màn hình này KHÔNG có nút Join, cứ để trống hoặc no-op
                        // ví dụ: không làm gì
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
        loadYourProjects();
    }
}
