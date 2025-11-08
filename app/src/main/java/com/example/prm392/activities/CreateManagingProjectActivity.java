package com.example.prm392.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.models.ProjectEntity;

import java.util.List;
import java.util.UUID;

public class CreateManagingProjectActivity extends AppCompatActivity {

    private EditText edtName, edtDesc;
    private Button btnCreate;
    private ProjectDAO projectDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_managing_project);

        // ------------------- Toolbar setup -------------------
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ------------------- View binding -------------------
        edtName = findViewById(R.id.edtProjectName);
        edtDesc = findViewById(R.id.edtDescription);
        btnCreate = findViewById(R.id.btnCreate);

        // ------------------- Room DAO -------------------
        projectDAO = AppDatabase.getInstance(getApplicationContext()).projectDAO();

        // ------------------- Button click -------------------
        btnCreate.setOnClickListener(v -> createProject());
    }

    private void createProject() {
        String name = edtName.getText().toString().trim();
        String desc = edtDesc.getText().toString().trim();

        // ---------------- VALIDATION ----------------
        if (name.isEmpty()) {
            edtName.setError("Tên project không được để trống!");
            edtName.requestFocus();
            return;
        }
        if (name.length() < 3) {
            edtName.setError("Tên project phải có ít nhất 3 ký tự!");
            edtName.requestFocus();
            return;
        }
        if (desc.length() < 10) {
            edtDesc.setError("Mô tả phải có ít nhất 10 ký tự!");
            edtDesc.requestFocus();
            return;
        }
        if (desc.length() > 300) {
            edtDesc.setError("Mô tả quá dài (tối đa 300 ký tự)!");
            edtDesc.requestFocus();
            return;
        }

        // ---------------- CHECK DUPLICATE ----------------
        List<ProjectEntity> existing = projectDAO.findByName(name);
        if (existing != null && !existing.isEmpty()) {
            edtName.setError("Tên project đã tồn tại!");
            edtName.requestFocus();
            return;
        }

        // ---------------- CREATE PROJECT ----------------
        ProjectEntity project = new ProjectEntity();
        project.projectId = java.util.UUID.randomUUID().toString();
        project.projectName = name;
        project.description = desc;
        project.createdBy = "currentUserId"; // if available
        project.createdAt = String.valueOf(System.currentTimeMillis());
        project.isPublic = false;


        // Room insert (synchronous for simplicity; could use Executor in production)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            projectDAO.insertOrUpdate(project);
        });

        Toast.makeText(this, "Tạo project thành công!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
