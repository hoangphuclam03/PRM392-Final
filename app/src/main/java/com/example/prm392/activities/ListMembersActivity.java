package com.example.prm392.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.MemberAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.UserEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddMember;
    private MemberAdapter adapter;

    private ProjectMemberDAO memberDAO;
    private UserDAO userDAO;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_members);

        recyclerView = findViewById(R.id.recyclerMembers);
        fabAddMember = findViewById(R.id.fabAddMember);

        AppDatabase db = AppDatabase.getInstance(this);
        memberDAO = db.projectMemberDAO();
        userDAO = db.userDAO();

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "Không tìm thấy dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMembers(projectId);

        fabAddMember.setOnClickListener(v -> showAddMemberDialog());
    }

    // Load all members for project
    private void loadMembers(String projectId) {
        executor.execute(() -> {
            List<ProjectMemberEntity> members = memberDAO.getMembersByProject(projectId);
            runOnUiThread(() -> {
                if (members == null || members.isEmpty()) {
                    Toast.makeText(this, "Chưa có thành viên trong dự án này.", Toast.LENGTH_SHORT).show();
                }
                adapter = new MemberAdapter(members);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    // Add member dialog
    private void showAddMemberDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_member, null);
        EditText edtEmail = dialogView.findViewById(R.id.edtEmail);
        Spinner spinnerRole = dialogView.findViewById(R.id.spinnerRole);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"member", "manager"}
        );
        spinnerRole.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm thành viên")
                .setView(dialogView)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String email = edtEmail.getText().toString().trim();
                    String role = spinnerRole.getSelectedItem().toString();
                    addMember(email, role);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Add member logic (Room)
    private void addMember(String email, String role) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email.", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            UserEntity user = userDAO.findByEmail(email);
            if (user == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Không tìm thấy người dùng với email này.", Toast.LENGTH_SHORT).show());
                return;
            }

            ProjectMemberEntity existing = memberDAO.findMemberInProject(projectId, user.userId);
            if (existing != null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Người dùng này đã có trong dự án.", Toast.LENGTH_SHORT).show());
                return;
            }

            ProjectMemberEntity newMember = new ProjectMemberEntity();
            newMember.memberId = UUID.randomUUID().toString();
            newMember.projectId = projectId;
            newMember.userId = user.userId;
            newMember.fullName = (user.fullName != null && !user.fullName.isEmpty())
                    ? user.fullName
                    : (user.email != null ? user.email : "Người dùng không xác định");
            newMember.role = role;

            memberDAO.insert(newMember);

            runOnUiThread(() -> {
                Toast.makeText(this, "Thêm thành viên thành công!", Toast.LENGTH_SHORT).show();
                loadMembers(projectId);
            });
        });
    }
}