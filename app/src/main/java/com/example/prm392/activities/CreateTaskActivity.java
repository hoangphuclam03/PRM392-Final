package com.example.prm392.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.MemberSelectAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Projects;
import models.TaskAssignees;
import models.Tasks;
import models.Users;

public class CreateTaskActivity extends AppCompatActivity {

    // UI Components
    private Spinner spinnerProject;
    private EditText etTaskTitle, etTaskDescription;
    private TextView tvSelectedDate, tvSelectedCount;
    private RadioGroup rgStatus;
    private RecyclerView rvMembers;
    private Button btnCreateTask, btnCancel, btnPickDate;

    // Data
    private List<Projects> projectsList = new ArrayList<>();
    private List<Users> membersList = new ArrayList<>();
    private MemberSelectAdapter memberAdapter;
    private String selectedDate = "";
    private int currentUserId; // TODO: Lấy từ SharedPreferences hoặc Auth

    // Firebase
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // Initialize Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // TODO: Lấy currentUserId từ SharedPreferences
        currentUserId = 1; // Dummy data

        initViews();
        setupRecyclerView();
        loadProjects();
        loadMembers();
        setupListeners();
    }

    @SuppressLint("WrongViewCast")
    private void initViews() {
        spinnerProject = findViewById(R.id.spinnerProject);
        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        rgStatus = findViewById(R.id.rgStatus);
        rvMembers = findViewById(R.id.rvMembers);
        btnCreateTask = findViewById(R.id.btnCreateTask);
        btnCancel = findViewById(R.id.btnCancel);
        btnPickDate = findViewById(R.id.btnPickDate);
    }

    private void setupRecyclerView() {
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberSelectAdapter(membersList, count -> {
            tvSelectedCount.setText(count + " người");
        });
        rvMembers.setAdapter(memberAdapter);
    }

    private void loadProjects() {
        // TODO: Thay đổi query để chỉ load projects mà user tham gia
        dbRef.child("projects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                projectsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Projects project = data.getValue(Projects.class);
                    if (project != null) {
                        projectsList.add(project);
                    }
                }
                setupProjectSpinner();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CreateTaskActivity.this,
                        "Lỗi load projects: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupProjectSpinner() {
        List<String> projectNames = new ArrayList<>();
        for (Projects project : projectsList) {
            projectNames.add(project.getProjectName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                projectNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(adapter);
    }

    private void loadMembers() {
        // TODO: Load members từ project được chọn
        // Hiện tại load tất cả users (dummy)
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                membersList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Users user = data.getValue(Users.class);
                    if (user != null) {
                        membersList.add(user);
                    }
                }
                memberAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(CreateTaskActivity.this,
                        "Lỗi load members: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnCreateTask.setOnClickListener(v -> createTask());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    selectedDate = sdf.format(selectedCalendar.getTime());
                    tvSelectedDate.setText(selectedDate);
                    tvSelectedDate.setTextColor(getResources().getColor(android.R.color.black));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Không cho chọn ngày trong quá khứ
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void createTask() {
        // Validate input
        if (!validateInput()) {
            return;
        }

        // Get selected project
        int projectPosition = spinnerProject.getSelectedItemPosition();
        Projects selectedProject = projectsList.get(projectPosition);

        // Get status
        String status = getSelectedStatus();

        // Get next task ID
        getNextTaskId(taskId -> {
            // Create Task object
            Tasks newTask = new Tasks(
                    taskId,
                    selectedProject.getProjectId(),
                    etTaskTitle.getText().toString().trim(),
                    etTaskDescription.getText().toString().trim(),
                    selectedDate,
                    status,
                    currentUserId
            );

            // Save to Firebase
            saveTaskToFirebase(newTask);
        });
    }

    private boolean validateInput() {
        String title = etTaskTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề task", Toast.LENGTH_SHORT).show();
            etTaskTitle.requestFocus();
            return false;
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày hết hạn", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (memberAdapter.getSelectedUserIds().isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 thành viên", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String getSelectedStatus() {
        int selectedId = rgStatus.getCheckedRadioButtonId();
        if (selectedId == R.id.rbTodo) {
            return "TODO";
        } else if (selectedId == R.id.rbInProgress) {
            return "IN_PROGRESS";
        } else if (selectedId == R.id.rbInReview) {
            return "IN_REVIEW";
        }
        return "TODO";
    }

    private void getNextTaskId(OnTaskIdCallback callback) {
        dbRef.child("tasks").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int nextId = 1;
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Tasks task = data.getValue(Tasks.class);
                                if (task != null) {
                                    nextId = task.getTaskId() + 1;
                                }
                            }
                        }
                        callback.onIdGenerated(nextId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(CreateTaskActivity.this,
                                "Lỗi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveTaskToFirebase(Tasks task) {
        // Save task
        dbRef.child("tasks").child(String.valueOf(task.getTaskId()))
                .setValue(task)
                .addOnSuccessListener(aVoid -> {
                    // Save task assignees
                    saveTaskAssignees(task.getTaskId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Lỗi tạo task: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveTaskAssignees(int taskId) {
        List<Integer> selectedUserIds = memberAdapter.getSelectedUserIds();

        getNextAssigneeId(assigneeId -> {
            for (int i = 0; i < selectedUserIds.size(); i++) {
                TaskAssignees assignee = new TaskAssignees(
                        assigneeId + i,
                        taskId,
                        selectedUserIds.get(i)
                );

                dbRef.child("task_assignees").child(String.valueOf(assignee.getId()))
                        .setValue(assignee);
            }

            Toast.makeText(this, "Tạo task thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void getNextAssigneeId(OnTaskIdCallback callback) {
        dbRef.child("task_assignees").orderByKey().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int nextId = 1;
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                TaskAssignees assignee = data.getValue(TaskAssignees.class);
                                if (assignee != null) {
                                    nextId = assignee.getId() + 1;
                                }
                            }
                        }
                        callback.onIdGenerated(nextId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Handle error
                    }
                });
    }

    interface OnTaskIdCallback {
        void onIdGenerated(int id);
    }
}