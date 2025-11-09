package com.example.prm392.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.prm392.R;
import com.example.prm392.adapter.MemberSelectAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.TaskEntity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateTaskActivity extends AppCompatActivity {

    private Spinner spinnerProject;
    private EditText etTitle, etDesc;
    private TextView tvDate, tvSelectedCount;
    private RadioGroup rgStatus;
    private RecyclerView rvMembers;
    private Button btnCreate, btnCancel;
    private ImageButton btnPickDate;


    private AppDatabase db;
    private List<ProjectEntity> projects = new ArrayList<>();
    private List<ProjectMemberEntity> projectMembers = new ArrayList<>();
    private MemberSelectAdapter memberAdapter;

    private String selectedProjectId = null;
    private String selectedDate = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        db = AppDatabase.getInstance(getApplicationContext());
        bindViews();
        loadProjects();

        btnPickDate.setOnClickListener(v -> pickDate());
        btnCancel.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> createTask());
    }

    @SuppressLint("WrongViewCast")
    private void bindViews() {
        spinnerProject   = findViewById(R.id.spinnerProject);
        etTitle          = findViewById(R.id.etTaskTitle);
        etDesc           = findViewById(R.id.etTaskDescription);
        tvDate           = findViewById(R.id.tvSelectedDate);
        btnPickDate      = findViewById(R.id.btnPickDate);
        tvSelectedCount  = findViewById(R.id.tvSelectedCount);
        rvMembers        = findViewById(R.id.rvMembers);
        rgStatus         = findViewById(R.id.rgStatus);
        btnCreate        = findViewById(R.id.btnCreateTask);
        btnCancel        = findViewById(R.id.btnCancel);

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        memberAdapter = new MemberSelectAdapter(new ArrayList<>(), ids -> {
            tvSelectedCount.setText("Đã chọn: " + ids);
        });
        rvMembers.setAdapter(memberAdapter);
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedDate = String.format("%02d/%02d/%04d", d, (m + 1), y);
            tvDate.setText(selectedDate);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadProjects() {
        AsyncTask.execute(() -> {
            // Lấy user hiện tại từ Firebase
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // ✅ Chỉ load các project mà người này là Owner
            List<ProjectEntity> list = db.projectDAO().getProjectsByOwner(currentUserId);

            runOnUiThread(() -> {
                projects.clear();
                projects.addAll(list);

                if (projects.isEmpty()) {
                    Toast.makeText(this, "Bạn chưa là Owner của dự án nào để tạo Task.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item,
                        toProjectNames(list)
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerProject.setAdapter(adapter);

                spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedProjectId = projects.get(position).projectId;
                        loadMembersForProject(selectedProjectId);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });

                // Nếu có PROJECT_ID truyền sẵn (ví dụ khi click từ ListProject)
                String forcedId = getIntent().getStringExtra("PROJECT_ID");
                if (forcedId != null) {
                    int idx = indexOfProject(forcedId);
                    if (idx >= 0) spinnerProject.setSelection(idx);
                }
            });
        });
    }


    private List<String> toProjectNames(List<ProjectEntity> list) {
        List<String> names = new ArrayList<>();
        for (ProjectEntity p : list) names.add(p.projectName);
        return names;
    }

    private int indexOfProject(String projectId) {
        for (int i = 0; i < projects.size(); i++) {
            if (projectId.equals(projects.get(i).projectId)) return i;
        }
        return -1;
    }

    private void loadMembersForProject(String projectId) {
        AsyncTask.execute(() -> {
            List<ProjectMemberEntity> members = db.projectMemberDAO().getMembersByProject(projectId);
            runOnUiThread(() -> {
                projectMembers.clear();
                projectMembers.addAll(members);
                memberAdapter.submitList(members);
                tvSelectedCount.setText("Đã chọn: 0");
            });
        });
    }

    private void createTask() {
        String title = etTitle.getText().toString().trim();
        String desc  = etDesc.getText().toString().trim();
        String status;
        int checked = rgStatus.getCheckedRadioButtonId();
        if (checked == R.id.rbInProgress) status = "INPROGRESS";
        else if (checked == R.id.rbInReview) status = "INREVIEW";
        else {
            status = "TODO";
        }

        if (title.isEmpty()) { etTitle.setError("Nhập tiêu đề"); return; }
        if (selectedProjectId == null) { toast("Chưa chọn project"); return; }
        if (selectedDate.isEmpty()) { toast("Chưa chọn deadline"); return; }

        List<String> selectedUserIds = memberAdapter.getSelectedUserIds();
        if (selectedUserIds.isEmpty()) { toast("Chọn ít nhất 1 thành viên"); return; }

        // hiện tại TaskEntity có 1 field assignedTo => tạo 1 task/1 assignee
        AsyncTask.execute(() -> {
            long now = System.currentTimeMillis();
            for (String uid : selectedUserIds) {
                TaskEntity t = new TaskEntity();
                t.taskId = "task_" + now + "_" + uid; // tạm thời, nếu cần hãy chuyển sang Firestore ID
                t.projectId = selectedProjectId;
                t.assignedTo = uid;
                t.title = title;
                t.description = desc;
                t.status = status;
                t.dueDate = selectedDate;
                t.isPendingSync = false;
                t.pendingSync = false;
                t.lastSyncedAt = now;
                db.taskDAO().insertOrUpdate(t);
            }
            runOnUiThread(() -> {
                toast("Tạo task thành công");
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
