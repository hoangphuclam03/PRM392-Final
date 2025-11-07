package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.KanbanAdapter;
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

public class KanbanBoardActivity extends AppCompatActivity {

    private TextView tvProjectName, tvTaskCount;
    private TextView tvTodoCount, tvInProgressCount, tvInReviewCount, tvDoneCount;
    private ImageButton btnBack, btnRefresh;
    private FloatingActionButton fabAddTask;

    private RecyclerView rvTodo, rvInProgress, rvInReview, rvDone;
    private KanbanAdapter todoAdapter, inProgressAdapter, inReviewAdapter, doneAdapter;

    private DatabaseReference dbRef;
    private int projectId;
    private Projects currentProject;

    private List<Tasks> allTasks = new ArrayList<>();
    private Map<Integer, List<Users>> taskAssigneesMap = new HashMap<>();

    // Drag and Drop
    private Tasks draggedTask;
    private int draggedTaskPosition;
    private String draggedFromColumn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kanban_board);

        // Get project ID from intent
        projectId = getIntent().getIntExtra("PROJECT_ID", -1);
        if (projectId == -1) {
            Toast.makeText(this, "Invalid project ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupRecyclerViews();
        setupDragAndDrop();
        setupListeners();
        loadData();
    }

    private void initViews() {
        tvProjectName = findViewById(R.id.tvProjectName);
        tvTaskCount = findViewById(R.id.tvTaskCount);
        tvTodoCount = findViewById(R.id.tvTodoCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvInReviewCount = findViewById(R.id.tvInReviewCount);
        tvDoneCount = findViewById(R.id.tvDoneCount);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        fabAddTask = findViewById(R.id.fabAddTask);

        rvTodo = findViewById(R.id.rvTodo);
        rvInProgress = findViewById(R.id.rvInProgress);
        rvInReview = findViewById(R.id.rvInReview);
        rvDone = findViewById(R.id.rvDone);
    }

    private void setupRecyclerViews() {
        // TODO Column
        rvTodo.setLayoutManager(new LinearLayoutManager(this));
        todoAdapter = new KanbanAdapter(this,
                task -> showTaskDetail(task),
                (task, position) -> {
                    draggedTask = task;
                    draggedTaskPosition = position;
                    draggedFromColumn = "TODO";
                }
        );
        rvTodo.setAdapter(todoAdapter);

        // IN_PROGRESS Column
        rvInProgress.setLayoutManager(new LinearLayoutManager(this));
        inProgressAdapter = new KanbanAdapter(this,
                task -> showTaskDetail(task),
                (task, position) -> {
                    draggedTask = task;
                    draggedTaskPosition = position;
                    draggedFromColumn = "IN_PROGRESS";
                }
        );
        rvInProgress.setAdapter(inProgressAdapter);

        // IN_REVIEW Column
        rvInReview.setLayoutManager(new LinearLayoutManager(this));
        inReviewAdapter = new KanbanAdapter(this,
                task -> showTaskDetail(task),
                (task, position) -> {
                    draggedTask = task;
                    draggedTaskPosition = position;
                    draggedFromColumn = "IN_REVIEW";
                }
        );
        rvInReview.setAdapter(inReviewAdapter);

        // DONE Column
        rvDone.setLayoutManager(new LinearLayoutManager(this));
        doneAdapter = new KanbanAdapter(this,
                task -> showTaskDetail(task),
                (task, position) -> {
                    draggedTask = task;
                    draggedTaskPosition = position;
                    draggedFromColumn = "DONE";
                }
        );
        rvDone.setAdapter(doneAdapter);
    }

    private void setupDragAndDrop() {
        setupColumnDragListener(rvTodo, "TODO", todoAdapter);
        setupColumnDragListener(rvInProgress, "IN_PROGRESS", inProgressAdapter);
        setupColumnDragListener(rvInReview, "IN_REVIEW", inReviewAdapter);
        setupColumnDragListener(rvDone, "DONE", doneAdapter);
    }

    private void setupColumnDragListener(RecyclerView recyclerView, String targetStatus, KanbanAdapter adapter) {
        recyclerView.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    // Visual feedback - highlight column
                    v.setAlpha(0.7f);
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setAlpha(1.0f);
                    return true;

                case DragEvent.ACTION_DROP:
                    v.setAlpha(1.0f);

                    if (draggedTask == null) {
                        return false;
                    }

                    // If dropped in the same column, do nothing
                    if (draggedFromColumn.equals(targetStatus)) {
                        resetDraggedItem();
                        return true;
                    }

                    // Move task to new status
                    moveTaskToColumn(draggedTask, targetStatus, adapter);
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    v.setAlpha(1.0f);
                    resetDraggedItem();
                    return true;

                default:
                    return false;
            }
        });
    }

    private void moveTaskToColumn(Tasks task, String newStatus, KanbanAdapter targetAdapter) {
        // Remove from old column
        KanbanAdapter sourceAdapter = getAdapterByStatus(draggedFromColumn);
        if (sourceAdapter != null) {
            sourceAdapter.removeTask(draggedTaskPosition);
        }

        // Update task status
        task.setStatus(newStatus);

        // Add to new column
        targetAdapter.addTask(task);

        // Update in Firebase
        updateTaskStatusInFirebase(task);

        // Refresh UI
        updateTaskCounts();

        Toast.makeText(this, "Task moved to " + newStatus, Toast.LENGTH_SHORT).show();
    }

    private KanbanAdapter getAdapterByStatus(String status) {
        switch (status) {
            case "TODO":
                return todoAdapter;
            case "IN_PROGRESS":
                return inProgressAdapter;
            case "IN_REVIEW":
                return inReviewAdapter;
            case "DONE":
                return doneAdapter;
            default:
                return null;
        }
    }

    private void resetDraggedItem() {
        draggedTask = null;
        draggedTaskPosition = -1;
        draggedFromColumn = null;

        // Reset alpha for all items
        rvTodo.post(() -> resetRecyclerViewAlpha(rvTodo));
        rvInProgress.post(() -> resetRecyclerViewAlpha(rvInProgress));
        rvInReview.post(() -> resetRecyclerViewAlpha(rvInReview));
        rvDone.post(() -> resetRecyclerViewAlpha(rvDone));
    }

    private void resetRecyclerViewAlpha(RecyclerView recyclerView) {
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            if (child != null) {
                child.setAlpha(1.0f);
            }
        }
    }

    private void updateTaskStatusInFirebase(Tasks task) {
        dbRef.child("tasks").child(String.valueOf(task.getTaskId()))
                .child("status")
                .setValue(task.getStatus())
                .addOnSuccessListener(aVoid -> {
                    // Success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to update task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadData());
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateTaskActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
        });
    }

    private void loadData() {
        loadProject();
        loadTasksForProject();
    }

    private void loadProject() {
        dbRef.child("projects").child(String.valueOf(projectId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentProject = snapshot.getValue(Projects.class);
                        if (currentProject != null) {
                            tvProjectName.setText(currentProject.getProjectName());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(KanbanBoardActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTasksForProject() {
        dbRef.child("tasks")
                .orderByChild("projectId")
                .equalTo(projectId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        allTasks.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Tasks task = data.getValue(Tasks.class);
                            if (task != null) {
                                allTasks.add(task);
                            }
                        }
                        loadTaskAssignees();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(KanbanBoardActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTaskAssignees() {
        dbRef.child("task_assignees").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Integer, List<Integer>> taskUserIdsMap = new HashMap<>();

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

                loadUsersForTasks(taskUserIdsMap);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(KanbanBoardActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsersForTasks(Map<Integer, List<Integer>> taskUserIdsMap) {
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Integer, Users> usersMap = new HashMap<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Users user = data.getValue(Users.class);
                    if (user != null) {
                        usersMap.put(user.getId(), user);
                    }
                }

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

                updateKanbanBoard();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(KanbanBoardActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateKanbanBoard() {
        List<Tasks> todoTasks = new ArrayList<>();
        List<Tasks> inProgressTasks = new ArrayList<>();
        List<Tasks> inReviewTasks = new ArrayList<>();
        List<Tasks> doneTasks = new ArrayList<>();

        for (Tasks task : allTasks) {
            switch (task.getStatus()) {
                case "TODO":
                    todoTasks.add(task);
                    break;
                case "IN_PROGRESS":
                    inProgressTasks.add(task);
                    break;
                case "IN_REVIEW":
                    inReviewTasks.add(task);
                    break;
                case "DONE":
                    doneTasks.add(task);
                    break;
            }
        }

        todoAdapter.setTasks(todoTasks);
        todoAdapter.setTaskAssignees(taskAssigneesMap);

        inProgressAdapter.setTasks(inProgressTasks);
        inProgressAdapter.setTaskAssignees(taskAssigneesMap);

        inReviewAdapter.setTasks(inReviewTasks);
        inReviewAdapter.setTaskAssignees(taskAssigneesMap);

        doneAdapter.setTasks(doneTasks);
        doneAdapter.setTaskAssignees(taskAssigneesMap);

        updateTaskCounts();
    }

    private void updateTaskCounts() {
        int todoCount = todoAdapter.getItemCount();
        int inProgressCount = inProgressAdapter.getItemCount();
        int inReviewCount = inReviewAdapter.getItemCount();
        int doneCount = doneAdapter.getItemCount();
        int totalCount = todoCount + inProgressCount + inReviewCount + doneCount;

        tvTodoCount.setText(String.valueOf(todoCount));
        tvInProgressCount.setText(String.valueOf(inProgressCount));
        tvInReviewCount.setText(String.valueOf(inReviewCount));
        tvDoneCount.setText(String.valueOf(doneCount));
        tvTaskCount.setText(totalCount + " tasks");
    }

    private void showTaskDetail(Tasks task) {
        Intent intent = new Intent(this, TaskDetailActivity.class);
        intent.putExtra("TASK_ID", task.getTaskId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }
}