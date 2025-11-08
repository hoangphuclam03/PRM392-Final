package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.MemberAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.models.ProjectMemberEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListMembersRolesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private MemberAdapter adapter;
    private ProjectMemberDAO memberDAO;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String projectId;
    private String currentUserRole = "Manager"; // 🔹 Giả lập role cho demo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_members_roles);

        recyclerView = findViewById(R.id.recyclerMembers);
        fabAdd = findViewById(R.id.fabAddMember);

        memberDAO = AppDatabase.getInstance(this).projectMemberDAO();

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "Không tìm thấy dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new MemberAdapter(null);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load members
        loadMembers(projectId);

        // Only managers can add new members
        if ("Manager".equals(currentUserRole)) {
            fabAdd.setVisibility(View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, AddMemberActivity.class)
                        .putExtra("projectId", projectId));
            });
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private void loadMembers(String projectId) {
        executor.execute(() -> {
            List<ProjectMemberEntity> members = memberDAO.getMembersByProject(projectId);
            runOnUiThread(() -> {
                if (members == null || members.isEmpty()) {
                    Toast.makeText(this, "Không có thành viên nào.", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.setMemberList(members);
                }
            });
        });
    }
}
