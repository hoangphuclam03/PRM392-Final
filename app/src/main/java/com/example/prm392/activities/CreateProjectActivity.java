package com.example.prm392.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.repository.SyncRepository;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CreateProjectActivity extends AppCompatActivity {
    private SwitchMaterial switchPublic;
    private TextInputEditText etName, etDescription;
    private Button btnCreate;
    private AppDatabase db;
    private SyncRepository syncRepo;

    private final String currentUserId = "USER001";
    private final String currentUserFullName = "Test User";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Tạo Project");

        switchPublic = findViewById(R.id.switchPublic);
        etName = findViewById(R.id.etProjectName);
        etDescription = findViewById(R.id.etProjectDescription);
        btnCreate = findViewById(R.id.btnCreateProject);

        db = AppDatabase.getInstance(this);
        syncRepo = new SyncRepository(this);

        btnCreate.setOnClickListener(v -> createProject());
    }

    private void createProject() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên project", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            ProjectEntity project = new ProjectEntity();
            project.projectId = UUID.randomUUID().toString();
            project.projectName = name;
            project.description = desc;
            project.createdBy = currentUserFullName;
            project.ownerId = currentUserId;
            project.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            project.updatedAt = System.currentTimeMillis();
            project.isPublic = switchPublic.isChecked();
            project.pendingSync = true;

            db.projectDAO().insertOrUpdate(project);

            ProjectMemberEntity member = new ProjectMemberEntity();
            member.memberId = UUID.randomUUID().toString();
            member.projectId = project.projectId;
            member.userId = currentUserId;
            member.fullName = currentUserFullName;
            member.role = "Manager";
            member.pendingSync = true;
            db.projectMemberDAO().insertOrUpdate(member);

            syncRepo.syncProjectsToFirestore();
            syncRepo.syncMembersToFirestore();

            runOnUiThread(() -> {
                String status = project.isPublic ? "Public" : "Private";
                Toast.makeText(this, "Đã tạo Project (" + status + ")", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
