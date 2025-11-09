package com.example.prm392.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.MemberAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.UserEntity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ProjectMembersActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private Button btnAddMember;
    private TextView tvProjectTitle, tvProjectVisibility;

    private MemberAdapter memberAdapter;
    private final List<ProjectMemberEntity> members = new ArrayList<>();

    private String projectId;
    private String projectName;
    private boolean isPublic;
    private String currentUserId = "";
    private boolean isManager = false;

    private static final int REQUEST_ADD_MEMBER = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_members);

        rvMembers = findViewById(R.id.rvMembers);
        btnAddMember = findViewById(R.id.btnAddMember);
        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        tvProjectVisibility = findViewById(R.id.tvProjectVisibility);

        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");
        isPublic = getIntent().getBooleanExtra("isPublic", false);

        // ✅ Lấy user hiện tại từ Room
        Executors.newSingleThreadExecutor().execute(() -> {
            UserEntity currentUser = AppDatabase.getInstance(this)
                    .userDAO()
                    .getLastLoggedInUser();
            if (currentUser != null) {
                currentUserId = currentUser.userId;
            }
        });

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberAdapter(members, isDarkMode());
        rvMembers.setAdapter(memberAdapter);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvProjectTitle.setText(projectName != null ? projectName : "Project");
        tvProjectVisibility.setText(isPublic ? "(Public)" : "(Private)");

        // 🔹 Callback xoá
        memberAdapter.setOnMemberDeleteListener(member -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("project_members")
                    .document(member.memberId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Executors.newSingleThreadExecutor().execute(() ->
                                AppDatabase.getInstance(this)
                                        .projectMemberDAO()
                                        .deleteMemberById(member.memberId));

                        members.remove(member);
                        memberAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Đã xoá " + member.fullName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi khi xoá: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        fetchLatestProjectInfo(); // 🔹 fix luôn hiển thị projectName
        loadMembers();
    }

    private void loadMembers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("project_members")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProjectMemberEntity> remoteMembers = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        ProjectMemberEntity m = doc.toObject(ProjectMemberEntity.class);
                        remoteMembers.add(m);
                    }

                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase localDb = AppDatabase.getInstance(this);
                        for (ProjectMemberEntity m : remoteMembers) {
                            localDb.projectMemberDAO().upsert(m);
                        }

                        // ✅ Kiểm tra xem current user có phải Manager
                        boolean foundManager = false;
                        for (ProjectMemberEntity m : remoteMembers) {
                            if (m.userId.equals(currentUserId) && "Manager".equalsIgnoreCase(m.role)) {
                                foundManager = true;
                                break;
                            }
                        }
                        isManager = foundManager;

                        runOnUiThread(() -> {
                            members.clear();
                            members.addAll(remoteMembers);
                            memberAdapter.setMemberList(members);
                            memberAdapter.setManager(isManager);
                            btnAddMember.setVisibility(isManager ? View.VISIBLE : View.GONE);

                            if (isManager) {
                                btnAddMember.setOnClickListener(v -> {
                                    Intent intent = new Intent(this, AddMemberActivity.class);
                                    intent.putExtra("projectId", projectId);
                                    intent.putExtra("projectName", projectName);
                                    startActivityForResult(intent, REQUEST_ADD_MEMBER);
                                });
                            }
                        });
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không thể tải danh sách thành viên", Toast.LENGTH_SHORT).show());
    }

    private void fetchLatestProjectInfo() {
        FirebaseFirestore.getInstance()
                .collection("projects")
                .document(projectId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("projectName");
                        Boolean pub = doc.getBoolean("isPublic");
                        if (name != null) tvProjectTitle.setText(name);
                        if (pub != null) {
                            isPublic = pub;
                            tvProjectVisibility.setText(pub ? "(Public)" : "(Private)");
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_MEMBER) loadMembers();
    }

    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
