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
import models.ProjectMembers;

public class ListMembersRolesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private DBConnect db;
    private List<ProjectMembers> memberList;
    private MemberAdapter adapter;
    private int projectId;
    private String currentUserRole = "Manager"; // 🔹 Giả lập role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_members_roles);

        recyclerView = findViewById(R.id.recyclerMembers);
        fabAdd = findViewById(R.id.fabAddMember);
        db = new DBConnect(this);

        projectId = getIntent().getIntExtra("projectId", -1);

        loadMembers();

        if ("Manager".equals(currentUserRole)) {
            fabAdd.setVisibility(android.view.View.VISIBLE);
            fabAdd.setOnClickListener(v -> {
                startActivity(new Intent(this, AddMemberActivity.class)
                        .putExtra("projectId", projectId));
            });
        }
    }

    private void loadMembers() {
        memberList = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();

        Cursor cursor = database.rawQuery(
                "SELECT * FROM project_members WHERE project_id = ?",
                new String[]{String.valueOf(projectId)}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                memberList.add(new ProjectMembers(id, projectId, userId, role));
            } while (cursor.moveToNext());
        } else {
            Toast.makeText(this, "No members found.", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        adapter = new MemberAdapter(memberList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
}
