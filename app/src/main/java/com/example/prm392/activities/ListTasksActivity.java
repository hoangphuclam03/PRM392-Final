package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.prm392.R;
import com.example.prm392.adapter.TaskAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Projects;
import models.TaskAssignees;
import models.Tasks;
import models.Users;

public class ListTasksActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private TextView tvTaskCount;
    private Button btnFilterAll, btnFilterTodo, btnFilterInProgress,
            btnFilterInReview, btnFilterDone;
    private FloatingActionButton fabCreateTask;

    private TaskAdapter taskAdapter;
    private List<Tasks> allTasks = new ArrayList<>();
    private Map<Integer, Projects> projectsMap = new HashMap<>();
    private Map<Integer, List<Users>> taskAssigneesMap = new HashMap<>();

    private DatabaseReference dbRef;
    private int currentUserId; // TODO: Get from SharedPreferences

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_tasks);

        // Initialize Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // TODO: Get currentUserId from SharedPreferences/Auth
        currentUserId = 1; // Dummy

        initViews();
        setupRecyclerView();
        setupFilters();
        loadData();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rvTasks);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterTodo = findViewById(R.id.btnFilterTodo);
        btnFilterInProgress = findViewById(R.id.btnFilterInProgress);
        btnFilterInReview = findViewById(R.id.btnFilterInReview);
        btnFilterDone = findViewById(R.id.btnFilterDone);
        fabCreateTask = findViewById(R.id.fabCreateTask);

        // Swipe refresh
        swipeRefresh.setOnRefreshListener(this::loadData);
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
        );

        // FAB click
        fabCreateTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Tasks task) {
                // Open task detail
                Intent intent = new Intent(ListTasksActivity.this, TaskDetailActivity.class);
                intent.putExtra("TASK_ID", task.getTaskId());
                startActivity(intent);
            }

            @Override
            public void onTaskLongClick(Tasks task) {
                // Optional: Show quick actions menu
                Toast.makeText(ListTasksActivity.this,
                        "Long click: " + task.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> applyFilter("ALL", btnFilterAll));
        btnFilterTodo.setOnClickListener(v -> applyFilter("TODO", btnFilterTodo));
        btnFilterInProgress.setOnClickListener(v -> applyFilter("IN_PROGRESS", btnFilterInProgress));
        btnFilterInReview.setOnClickListener(v -> applyFilter("IN_REVIEW", btnFilterInReview));
        btnFilterDone.setOnClickListener(v -> applyFilter("DONE", btnFilterDone));

        // Set initial filter
        highlightButton(btnFilterAll);
    }

    private void applyFilter(String status, Button selectedButton) {
        currentFilter = status;
        taskAdapter.filter(status);

        // Update UI
        resetFilterButtons();
        highlightButton(selectedButton);
        updateTaskCount();
    }

    private void resetFilterButtons() {
        btnFilterAll.setBackgroundColor(getColor(android.R.color.transparent));
        btnFilterTodo.setBackgroundColor(getColor(android.R.color.transparent));
        btnFilterInProgress.setBackgroundColor(getColor(android.R.color.transparent));
        btnFilterInReview.setBackgroundColor(getColor(android.R.color.transparent));
        btnFilterDone.setBackgroundColor(getColor(android.R.color.transparent));
    }

    private void highlightButton(Button button) {
        button.setBackgroundColor(getColor(R.color.blue_light)); // Add to colors.xml: #E3F2FD
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);

        // Load tất cả data song song
        loadProjects(() ->
                loadUserTasks(() ->
                        loadTaskAssignees(() -> {
                            // Sau khi load xong hết, update UI
                            updateUI();
                            swipeRefresh.setRefreshing(false);
                        })
                )
        );
    }

    private void loadProjects(Runnable onComplete) {
        dbRef.child("projects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                projectsMap.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Projects project = data.getValue(Projects.class);
                    if (project != null) {
                        projectsMap.put(project.getProjectId(), project);
                    }
                }
                onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ListTasksActivity.this,
                        "Lỗi: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadUserTasks(Runnable onComplete) {
        // Bước 1: Lấy danh sách taskId mà user được assign
        dbRef.child("task_assignees")
                .orderByChild("userId")
                .equalTo(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Integer> taskIds = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            TaskAssignees assignee = data.getValue(TaskAssignees.class);
                            if (assignee != null) {
                                taskIds.add(assignee.getTaskId());
                            }
                        }

                        // Bước 2: Load tasks từ danh sách taskIds
                        loadTasksByIds(taskIds, onComplete);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(ListTasksActivity.this,
                                "Lỗi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void loadTasksByIds(List<Integer> taskIds, Runnable onComplete) {
        if (taskIds.isEmpty()) {
            allTasks.clear();
            onComplete.run();
            return;
        }

        dbRef.child("tasks").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allTasks.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Tasks task = data.getValue(Tasks.class);
                    if (task != null && taskIds.contains(task.getTaskId())) {
                        allTasks.add(task);
                    }
                }
                onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ListTasksActivity.this,
                        "Lỗi: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadTaskAssignees(Runnable onComplete) {
        dbRef.child("task_assignees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Integer, List<Integer>> taskUserIdsMap = new HashMap<>();

                // Group userIds by taskId
                for (DataSnapshot data : snapshot.getChildren()) {
                    TaskAssignees assignee = data.getValue(TaskAssignees.class);
                    if (assignee != null) {
                        int taskId = assignee.getTaskId();
                        if (!taskUserIdsMap.containsKey(taskId)) {
                            taskUserIdsMap.put(taskId, new ArrayList<>());
                        }
                        taskUserIdsMap.get(taskId).add(assignee.getUserId());
                    }
                }

                // Load user details
                loadUsersForTasks(taskUserIdsMap, onComplete);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ListTasksActivity.this,
                        "Lỗi: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void loadUsersForTasks(Map<Integer, List<Integer>> taskUserIdsMap, Runnable onComplete) {
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Integer, Users> usersMap = new HashMap<>();

                // Load all users
                for (DataSnapshot data : snapshot.getChildren()) {
                    Users user = data.getValue(Users.class);
                    if (user != null) {
                        usersMap.put(user.getId(), user);
                    }
                }

                // Map users to tasks
                taskAssigneesMap.clear();
                for (Map.Entry<Integer, List<Integer>> entry : taskUserIdsMap.entrySet()) {
                    int taskId = entry.getKey();
                    List<Users> assignees = new ArrayList<>();

                    for (int userId : entry.getValue()) {
                        Users user = usersMap.get(userId);
                        if (user != null) {
                            assignees.add(user);
                        }
                    }

                    taskAssigneesMap.put(taskId, assignees);
                }

                onComplete.run();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ListTasksActivity.this,
                        "Lỗi: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateUI() {
        // Update adapter
        taskAdapter.setTasks(allTasks);
        taskAdapter.setProjects(projectsMap);
        taskAdapter.setTaskAssignees(taskAssigneesMap);
        taskAdapter.filter(currentFilter);

        // Show/hide empty state
        if (allTasks.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
        }

        updateTaskCount();
    }

    private void updateTaskCount() {
        int count = taskAdapter.getItemCount();
        tvTaskCount.setText(count + " task");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data khi quay lại activity (sau khi tạo task mới)
        loadData();
    }
}