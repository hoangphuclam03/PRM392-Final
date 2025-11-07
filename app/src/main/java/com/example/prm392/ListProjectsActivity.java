package com.example.prm392;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

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

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_projects);

        // ---------------- ÁNH XẠ VIEW ----------------
        recyclerView = findViewById(R.id.recyclerProjects);
        fabAdd = findViewById(R.id.fabAddProject);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ---------------- SETUP DRAWER TOGGLE ----------------
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // Đổi màu biểu tượng menu nếu cần
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

        // ---------------- LOAD DỮ LIỆU PROJECT ----------------
        db = new DBConnect(this);
        loadProjects();

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateManagingProjectActivity.class);
            startActivity(intent);

        });

        // ---------------- XỬ LÝ MENU BÊN TRÁI ----------------
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent intent = new Intent(ListProjectsActivity.this, HomeActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;

        } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Bạn đang ở: Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListProjectsActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                logoutUser();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ---------------- HÀM LOAD PROJECTS ----------------
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

    // ---------------- REFRESH KHI QUAY LẠI ----------------
    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    // ---------------- LOGOUT USER ----------------
    private void logoutUser() {
        Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ---------------- MỞ MENU KHI BẤM NÚT ☰ ----------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
