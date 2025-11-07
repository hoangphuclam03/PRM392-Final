package com.example.prm392.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.AssigneeDisplayAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import models.Projects;
import models.TaskAssignees;
import models.Tasks;
import models.Users;

public class TaskDetailActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack, btnEditMode, btnDelete, btnEditDueDate;
    private TextView tvDetailStatus, tvDetailProjectName, tvDetailDueDate;
    private EditText etDetailTitle, etDetailDescription;
    private RadioGroup rgDetailStatus;
    private RecyclerView rvDetailAssignees;
    private CardView cardStatusChange;
    private LinearLayout layoutActionButtons;
    private Button btnCancelEdit, btnSaveEdit;

    // Data
    private Tasks currentTask;
    private Projects currentProject;
    private List<Users> assigneesList = new ArrayList<>();
    private AssigneeDisplayAdapter assigneeAdapter;
    private DatabaseReference dbRef;

    // Edit mode
    private boolean isEditMode = false;
    private String originalTitle;
    private String originalDescription;
    private String originalDueDate;
    private String originalStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        dbRef = FirebaseDatabase.getInstance().getReference();

        // Get task ID from intent
        int taskId = getIntent().getIntExtra("TASK_ID", -1);
        if (taskId == -1) {
            Toast.makeText(this, "Invalid task ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadTaskData(taskId);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEditMode = findViewById(R.id.btnEditMode);
        btnDelete = findViewById(R.id.btnDelete);
        btnEditDueDate = findViewById(R.id.btnEditDueDate);

        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        tvDetailProjectName = findViewById(R.id.tvDetailProjectName);
        tvDetailDueDate = findViewById(R.id.tvDetailDueDate);

        etDetailTitle = findViewById(R.id.etDetailTitle);
        etDetailDescription = findViewById(R.id.etDetailDescription);

        rgDetailStatus = findViewById(R.id.rgDetailStatus);
        rvDetailAssignees = findViewById(R.id.rvDetailAssignees);

        cardStatusChange = findViewById(R.id.cardStatusChange);
        layoutActionButtons = findViewById(R.id.layoutActionButtons);

        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnSaveEdit = findViewById(R.id.btnSaveEdit);
    }

    private void setupRecyclerView() {
        rvDetailAssignees.setLayoutManager(new LinearLayoutManager(this));
        assigneeAdapter = new AssigneeDisplayAdapter(this);
        rvDetailAssignees.setAdapter(assigneeAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnEditMode.setOnClickListener(v -> toggleEditMode());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnEditDueDate.setOnClickListener(v -> showDatePicker());
        btnCancelEdit.setOnClickListener(v -> cancelEdit());
        btnSaveEdit.setOnClickListener(v -> saveChanges());
    }

    private void loadTaskData(int taskId) {
        dbRef.child("tasks").child(String.valueOf(taskId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentTask = snapshot.getValue(Tasks.class);
                        if (currentTask != null) {
                            displayTaskData();
                            loadProjectData(currentTask.getProjectId());
                            loadAssignees(currentTask.getTaskId());
                        } else {
                            Toast.makeText(TaskDetailActivity.this,
                                    "Task not found",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayTaskData() {
        etDetailTitle.setText(currentTask.getTitle());
        etDetailDescription.setText(currentTask.getDescription());
        tvDetailDueDate.setText("📅 " + currentTask.getDueDate());

        // Set status badge
        setStatusBadge(currentTask.getStatus());

        // Set radio button
        setStatusRadioButton(currentTask.getStatus());

        // Save original values
        originalTitle = currentTask.getTitle();
        originalDescription = currentTask.getDescription();
        originalDueDate = currentTask.getDueDate();
        originalStatus = currentTask.getStatus();
    }

    private void setStatusBadge(String status) {
        switch (status) {
            case "TODO":
                tvDetailStatus.setText("📋 TO DO");
                tvDetailStatus.setBackgroundResource(R.drawable.badge_todo);
                break;
            case "IN_PROGRESS":
                tvDetailStatus.setText("⚡ IN PROGRESS");
                tvDetailStatus.setBackgroundResource(R.drawable.badge_in_progress);
                break;
            case "IN_REVIEW":
                tvDetailStatus.setText("👀 IN REVIEW");
                tvDetailStatus.setBackgroundResource(R.drawable.badge_in_review);
                break;
            case "DONE":
                tvDetailStatus.setText("✅ DONE");
                tvDetailStatus.setBackgroundResource(R.drawable.badge_done);
                break;
        }
    }

    private void setStatusRadioButton(String status) {
        switch (status) {
            case "TODO":
                rgDetailStatus.check(R.id.rbDetailTodo);
                break;
            case "IN_PROGRESS":
                rgDetailStatus.check(R.id.rbDetailInProgress);
                break;
            case "IN_REVIEW":
                rgDetailStatus.check(R.id.rbDetailInReview);
                break;
            case "DONE":
                rgDetailStatus.check(R.id.rbDetailDone);
                break;
        }
    }

    private void loadProjectData(int projectId) {
        dbRef.child("projects").child(String.valueOf(projectId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        currentProject = snapshot.getValue(Projects.class);
                        if (currentProject != null) {
                            tvDetailProjectName.setText(currentProject.getProjectName());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void loadAssignees(int taskId) {
        // Get user IDs assigned to this task
        dbRef.child("task_assignees")
                .orderByChild("taskId")
                .equalTo(taskId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<Integer> userIds = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            TaskAssignees assignee = data.getValue(TaskAssignees.class);
                            if (assignee != null) {
                                userIds.add(assignee.getUserId());
                            }
                        }
                        loadUserDetails(userIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Error loading assignees: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserDetails(List<Integer> userIds) {
        if (userIds.isEmpty()) {
            assigneesList.clear();
            assigneeAdapter.setAssignees(assigneesList);
            return;
        }

        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                assigneesList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Users user = data.getValue(Users.class);
                    if (user != null && userIds.contains(user.getId())) {
                        assigneesList.add(user);
                    }
                }
                assigneeAdapter.setAssignees(assigneesList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            // Enable edit mode
            etDetailTitle.setEnabled(true);
            etDetailTitle.setBackgroundResource(android.R.drawable.edit_text);

            etDetailDescription.setEnabled(true);
            etDetailDescription.setBackgroundResource(android.R.drawable.edit_text);

            btnEditDueDate.setVisibility(View.VISIBLE);
            cardStatusChange.setVisibility(View.VISIBLE);
            layoutActionButtons.setVisibility(View.VISIBLE);

            btnEditMode.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            btnDelete.setVisibility(View.GONE);

        } else {
            // Disable edit mode (cancel)
            cancelEdit();
        }
    }

    private void cancelEdit() {
        isEditMode = false;

        // Restore original values
        etDetailTitle.setText(originalTitle);
        etDetailDescription.setText(originalDescription);
        tvDetailDueDate.setText("📅 " + originalDueDate);
        setStatusRadioButton(originalStatus);

        // Disable editing
        etDetailTitle.setEnabled(false);
        etDetailTitle.setBackgroundResource(android.R.color.transparent);

        etDetailDescription.setEnabled(false);
        etDetailDescription.setBackgroundResource(android.R.color.transparent);

        btnEditDueDate.setVisibility(View.GONE);
        cardStatusChange.setVisibility(View.GONE);
        layoutActionButtons.setVisibility(View.GONE);

        btnEditMode.setImageResource(android.R.drawable.ic_menu_edit);
        btnDelete.setVisibility(View.VISIBLE);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    String newDate = sdf.format(selectedCalendar.getTime());
                    tvDetailDueDate.setText("📅 " + newDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveChanges() {
        // Validate
        String newTitle = etDetailTitle.getText().toString().trim();
        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get new values
        String newDescription = etDetailDescription.getText().toString().trim();
        String newDueDate = tvDetailDueDate.getText().toString().replace("📅 ", "");
        String newStatus = getSelectedStatus();

        // Update task object
        currentTask.setTitle(newTitle);
        currentTask.setDescription(newDescription);
        currentTask.setDueDate(newDueDate);
        currentTask.setStatus(newStatus);

        // Save to Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", newTitle);
        updates.put("description", newDescription);
        updates.put("dueDate", newDueDate);
        updates.put("status", newStatus);

        dbRef.child("tasks").child(String.valueOf(currentTask.getTaskId()))
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                    // Update original values
                    originalTitle = newTitle;
                    originalDescription = newDescription;
                    originalDueDate = newDueDate;
                    originalStatus = newStatus;

                    // Update UI
                    setStatusBadge(newStatus);
                    cancelEdit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedStatus() {
        int selectedId = rgDetailStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.rbDetailTodo) {
            return "TODO";
        } else if (selectedId == R.id.rbDetailInProgress) {
            return "IN_PROGRESS";
        } else if (selectedId == R.id.rbDetailInReview) {
            return "IN_REVIEW";
        } else if (selectedId == R.id.rbDetailDone) {
            return "DONE";
        }
        return "TODO";
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Task")
                .setMessage("Bạn có chắc chắn muốn xóa task này?\n\n" +
                        "Hành động này không thể hoàn tác!")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Xóa", (dialog, which) -> deleteTask())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTask() {
        // Show loading
        Toast.makeText(this, "Đang xóa...", Toast.LENGTH_SHORT).show();

        // Delete task assignees first
        dbRef.child("task_assignees")
                .orderByChild("taskId")
                .equalTo(currentTask.getTaskId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            data.getRef().removeValue();
                        }

                        // Then delete the task
                        deleteTaskFromFirebase();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(TaskDetailActivity.this,
                                "Lỗi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteTaskFromFirebase() {
        dbRef.child("tasks").child(String.valueOf(currentTask.getTaskId()))
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Xóa task thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Lỗi xóa task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            // Show confirmation if in edit mode
            new AlertDialog.Builder(this)
                    .setTitle("Hủy chỉnh sửa?")
                    .setMessage("Các thay đổi chưa lưu sẽ bị mất!")
                    .setPositiveButton("Hủy chỉnh sửa", (dialog, which) -> {
                        cancelEdit();
                        super.onBackPressed();
                    })
                    .setNegativeButton("Tiếp tục chỉnh sửa", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}