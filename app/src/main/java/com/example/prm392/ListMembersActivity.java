package com.example.prm392;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import models.ProjectMembers;
import utils.DBConnect;

public class ListMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddMember;
    private DBConnect db;
    private List<ProjectMembers> memberList;
    private MemberAdapter adapter;
    private int projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_members);

        recyclerView = findViewById(R.id.recyclerMembers);
        fabAddMember = findViewById(R.id.fabAddMember);
        db = new DBConnect(this);

        projectId = getIntent().getIntExtra("projectId", -1);
        if (projectId == -1) {
            Toast.makeText(this, "Không tìm thấy dự án.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMembers(projectId);

        fabAddMember.setOnClickListener(v -> showAddMemberDialog());
    }

    // =============== HÀM HIỂN THỊ DANH SÁCH ===============
    private void loadMembers(int projectId) {
        memberList = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();

        Cursor cursor = database.rawQuery(
                "SELECT pm.id, pm.project_id, pm.user_id, u.firstName || ' ' || u.lastName AS name, pm.role " +
                        "FROM project_members pm " +
                        "JOIN users u ON pm.user_id = u.id " +
                        "WHERE pm.project_id = ?",
                new String[]{String.valueOf(projectId)}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));

                memberList.add(new ProjectMembers(id, projectId, userId, name, role));
            } while (cursor.moveToNext());
        } else {
            Toast.makeText(this, "Chưa có thành viên trong dự án này.", Toast.LENGTH_SHORT).show();
        }

        cursor.close();

        adapter = new MemberAdapter(memberList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // =============== HÀM HIỂN THỊ HỘP THOẠI THÊM THÀNH VIÊN ===============
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

    // =============== HÀM XỬ LÝ THÊM THÀNH VIÊN ===============
    private void addMember(String email, String role) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase database = db.getWritableDatabase();

        // Kiểm tra user tồn tại
        Cursor userCursor = database.rawQuery(
                "SELECT id FROM users WHERE email = ?",
                new String[]{email}
        );

        if (userCursor.moveToFirst()) {
            int userId = userCursor.getInt(userCursor.getColumnIndexOrThrow("id"));

            // Kiểm tra đã có trong project chưa
            Cursor checkCursor = database.rawQuery(
                    "SELECT * FROM project_members WHERE project_id = ? AND user_id = ?",
                    new String[]{String.valueOf(projectId), String.valueOf(userId)}
            );

            if (checkCursor.getCount() > 0) {
                Toast.makeText(this, "Người dùng này đã có trong dự án.", Toast.LENGTH_SHORT).show();
            } else {
                ContentValues values = new ContentValues();
                values.put("project_id", projectId);
                values.put("user_id", userId);
                values.put("role", role);
                long result = database.insert("project_members", null, values);

                if (result != -1) {
                    Toast.makeText(this, "Thêm thành viên thành công!", Toast.LENGTH_SHORT).show();
                    loadMembers(projectId);
                } else {
                    Toast.makeText(this, "Lỗi khi thêm thành viên.", Toast.LENGTH_SHORT).show();
                }
            }
            checkCursor.close();
        } else {
            Toast.makeText(this, "Không tìm thấy người dùng với email này.", Toast.LENGTH_SHORT).show();
        }

        userCursor.close();
    }
}
