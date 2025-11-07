package com.example.prm392;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import data.local.DBConnect;
import models.Projects;

public class ListYourProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private DBConnect db;
    private List<Projects> yourProjectList;
    private ProjectAdapter adapter;
    private int currentUserId = 1; // 🔹 giả lập user đang đăng nhập (sau này sẽ lấy từ SharedPref)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_your_projects);

        recyclerView = findViewById(R.id.recyclerYourProjects);
        fabAdd = findViewById(R.id.fabAddProject);

        Toolbar toolbar = findViewById(R.id.toolbarYourProjects);
        setSupportActionBar(toolbar);

        db = new DBConnect(this);
        loadYourProjects();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ListYourProjectsActivity.this, CreateManagingProjectActivity.class);
            startActivity(intent);
        });

    }

    private void loadYourProjects() {
        yourProjectList = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();

        // 🔹 Truy vấn: chỉ lấy project có userId khớp (user đang đăng nhập)
        String query = "SELECT p.project_id, p.project_name, p.description " +
                "FROM projects p INNER JOIN project_members pm " +
                "ON p.project_id = pm.project_id " +
                "WHERE pm.user_id = ?";
        Cursor cursor = database.rawQuery(query, new String[]{String.valueOf(currentUserId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("project_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("project_name"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                yourProjectList.add(new Projects(id, name, desc));
            } while (cursor.moveToNext());
        } else {
            Toast.makeText(this, "Bạn chưa tham gia dự án nào!", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        adapter = new ProjectAdapter(yourProjectList, project -> {
            Intent intent = new Intent(this, ProjectStatusActivity.class); // Kanban
            intent.putExtra("projectId", project.getProjectId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadYourProjects();
    }
}
