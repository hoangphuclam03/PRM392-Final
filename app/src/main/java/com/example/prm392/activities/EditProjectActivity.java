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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class EditProjectActivity extends AppCompatActivity {

    private SwitchMaterial switchPublic;
    private TextInputEditText etName, etDesc;
    private Button btnSave;
    private AppDatabase db;
    private SyncRepository syncRepo;
    private String projectId;
    private ProjectEntity project;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_project);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Chỉnh sửa Project");

        // Ánh xạ view
        etName = findViewById(R.id.etProjectName);
        etDesc = findViewById(R.id.etProjectDescription);
        btnSave = findViewById(R.id.btnSave);
        switchPublic = findViewById(R.id.switchPublicEdit);

        db = AppDatabase.getInstance(this);
        syncRepo = new SyncRepository(this);

        projectId = getIntent().getStringExtra("projectId");

        AppDatabase.databaseWriteExecutor.execute(() -> {
            project = db.projectDAO().getProjectById(projectId);
            if (project != null) {
                runOnUiThread(() -> {
                    etName.setText(project.projectName);
                    etDesc.setText(project.description);
                    switchPublic.setChecked(project.isPublic);
                });
            }
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
            boolean isNowPublic = switchPublic.isChecked();

            if (name.isEmpty()) {
                Toast.makeText(this, "Tên project không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                ProjectEntity project = db.projectDAO().getProjectById(projectId);
                if (project != null) {
                    project.projectName = name;
                    project.description = desc;
                    project.isPublic = isNowPublic;
                    project.pendingSync = true;
                    project.updatedAt = System.currentTimeMillis();

                    db.projectDAO().insertOrUpdate(project);
                    syncRepo.syncProjectsToFirestore();

                    runOnUiThread(() -> {
                        String status = isNowPublic ? "Công khai" : "Riêng tư";
                        Toast.makeText(this, "Đã cập nhật (" + status + ")", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
