package com.example.prm392;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;

import data.local.DBConnect;

public class CreateManagingProjectActivity extends AppCompatActivity {

    private EditText edtName, edtDesc;
    private Button btnCreate;
    private DBConnect db;
    private int currentUserId = 1; // 🔹 Giả lập user đang đăng nhập

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
        db = new DBConnect(this);

        // ------------------- Nút tạo project -------------------
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

        // ✅ Kiểm tra project trùng tên
        SQLiteDatabase database = db.getReadableDatabase();
        Cursor cursor = database.rawQuery(
                "SELECT project_id FROM projects WHERE LOWER(project_name) = ?",
                new String[]{name.toLowerCase()}
        );
        if (cursor.moveToFirst()) {
            cursor.close();
            edtName.setError("Tên project đã tồn tại!");
            edtName.requestFocus();
            return;
        }
        cursor.close();

        // ---------------- LƯU PROJECT ----------------
        SQLiteDatabase writableDb = db.getWritableDatabase();

        ContentValues projectValues = new ContentValues();
        projectValues.put("project_name", name);
        projectValues.put("description", desc);

        long projectId = writableDb.insert("projects", null, projectValues);

        if (projectId != -1) {
            ContentValues memberValues = new ContentValues();
            memberValues.put("project_id", projectId);
            memberValues.put("user_id", currentUserId);
            memberValues.put("role", "Manager");
            writableDb.insert("project_members", null, memberValues);

            Toast.makeText(this, "Tạo project thành công!", Toast.LENGTH_SHORT).show();
            finish(); // quay lại màn trước mà không đổi theme
        } else {
            Toast.makeText(this, "Không thể tạo project. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
        }
    }
}
