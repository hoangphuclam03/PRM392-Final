package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.UserEntity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.UUID;
import java.util.concurrent.Executors;

public class AddMemberActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnAdd;
    private String projectId;
    private String projectName;
    private String currentUserRole;

    private FirebaseFirestore db;
    private AppDatabase localDb;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_member);

        // 🔹 Ánh xạ View
        edtEmail = findViewById(R.id.edtEmail);
        btnAdd = findViewById(R.id.btnAdd);
        db = FirebaseFirestore.getInstance();
        localDb = AppDatabase.getInstance(this);

        // 🔹 Nhận dữ liệu từ Intent
        projectId = getIntent().getStringExtra("projectId");
        projectName = getIntent().getStringExtra("projectName");
        currentUserRole = getIntent().getStringExtra("role");

        // 🔹 Toolbar có nút Back
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, ProjectMembersActivity.class);
            intent.putExtra("projectId", projectId);
            intent.putExtra("projectName", projectName);
            intent.putExtra("role", currentUserRole);
            startActivity(intent);
            finish();
        });

        // 🔹 Xử lý thêm thành viên
        btnAdd.setOnClickListener(v -> addMemberByEmail());
    }

    private void addMemberByEmail() {
        String email = edtEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Đảm bảo projectId lấy từ local (Room), không phải từ Firestore
        Executors.newSingleThreadExecutor().execute(() -> {
            ProjectEntity project = localDb.projectDAO().getProjectById(projectId);
            if (project == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Không tìm thấy dự án trong local DB", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            String localProjectId = project.projectId; // đây là ID thật trong Room
            Log.d("DEBUG_ADD_MEMBER", "Thêm member vào projectId = " + localProjectId);

            db.collection("Users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            Toast.makeText(this, "Không tìm thấy người dùng với email này", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            UserEntity user = doc.toObject(UserEntity.class);
                            user.userId = doc.getId();

                            ProjectMemberEntity member = new ProjectMemberEntity();
                            member.memberId = UUID.randomUUID().toString();
                            member.projectId = localProjectId; //localProjectID ✅ Quan trọng: dùng ID local
                            member.userId = user.userId;
                            member.fullName = user.fullName != null ? user.fullName : "(No Name)";
                            member.role = "Member";
                            member.pendingSync = false;
                            member.updatedAt = System.currentTimeMillis();

                            // 🔹 Lưu vào Firestore
                            db.collection("project_members")
                                    .document(member.memberId)
                                    .set(member)
                                    .addOnSuccessListener(aVoid -> {
                                        Executors.newSingleThreadExecutor().execute(() -> {
                                            localDb.projectMemberDAO().upsert(member);
                                        });

                                        Toast.makeText(this, "Đã thêm thành viên: " + member.fullName, Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(this, ProjectMembersActivity.class);
                                        intent.putExtra("projectId", localProjectId);
                                        intent.putExtra("projectName", projectName);
                                        intent.putExtra("role", currentUserRole);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lỗi khi thêm: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                    );
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Lỗi kết nối Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }
}
