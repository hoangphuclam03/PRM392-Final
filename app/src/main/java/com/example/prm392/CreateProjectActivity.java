package com.example.prm392;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import data.local.DBConnect;


public class CreateProjectActivity extends AppCompatActivity {

    private EditText edtProjectName, edtDescription;
    private Button btnCreate;
    private DBConnect db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_project);

        // Ánh xạ view
        edtProjectName = findViewById(R.id.edtProjectName);
        edtDescription = findViewById(R.id.edtDescription);
        btnCreate = findViewById(R.id.btnCreate);
        db = new DBConnect(this);

        btnCreate.setOnClickListener(v -> createProject());
    }

    private void createProject() {
        String name = edtProjectName.getText().toString().trim();
        String desc = edtDescription.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên dự án.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("project_name", name);
        values.put("description", desc);
        // created_by giả định là 1 (nếu có login bạn có thể lấy id người dùng)
        values.put("created_by", 1);

        long result = database.insert("projects", null, values);

        if (result != -1) {
            Toast.makeText(this, "Tạo dự án thành công!", Toast.LENGTH_SHORT).show();
            finish(); // trở lại danh sách
        } else {
            Toast.makeText(this, "Lỗi khi lưu dự án.", Toast.LENGTH_SHORT).show();
        }
    }
}
