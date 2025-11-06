package com.example.prm392;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import data.local.DBConnect;
import models.Projects;


public class ListProjectsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private DBConnect db;
    private List<Projects> projectList;
    private ProjectAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_projects);

        recyclerView = findViewById(R.id.recyclerProjects);
        fabAdd = findViewById(R.id.fabAddProject);
        db = new DBConnect(this);

        loadProjects();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateProjectActivity.class);
            startActivity(intent);
        });
    }

    private void loadProjects() {
        projectList = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM projects", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("project_id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("project_name"));
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                projectList.add(new Projects(id, name, desc));
            } while (cursor.moveToNext());
        } else {
            Toast.makeText(this, "Chưa có dự án nào.", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        adapter = new ProjectAdapter(projectList, project -> {
            Intent intent = new Intent(this, ListMembersActivity.class);
            intent.putExtra("projectId", project.getProjectId());
            startActivity(intent);
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects(); // refresh lại khi quay lại từ CreateProject
    }
}
