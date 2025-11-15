package com.example.prm392.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.prm392.R;
import com.example.prm392.adapter.TaskAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.TaskEntity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class ListTasksActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvTasks;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvCount;
    private View emptyLayout;

    private Button chipAll, chipTodo, chipInProgress, chipInReview, chipDone;

    private TaskAdapter adapter;
    private List<TaskEntity> all = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_tasks);

        db = AppDatabase.getInstance(getApplicationContext());
        bindViews();
        setupAdapter();
        loadTasks();

        chipAll.setOnClickListener(v -> applyFilter("ALL"));
        chipTodo.setOnClickListener(v -> applyFilter("TODO"));
        chipInProgress.setOnClickListener(v -> applyFilter("INPROGRESS"));
        chipInReview.setOnClickListener(v -> applyFilter("INREVIEW"));
        chipDone.setOnClickListener(v -> applyFilter("DONE"));

        swipeRefresh.setOnRefreshListener(this::loadTasks);

        FloatingActionButton fab = findViewById(R.id.fabCreateTask);
        fab.setOnClickListener(v -> startActivityForResult(
                new Intent(this, CreateTaskActivity.class), 100));
    }

    @SuppressLint("WrongViewCast")
    private void bindViews() {
        tvCount = findViewById(R.id.tvTaskCount);
        rvTasks = findViewById(R.id.rvTasks);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyLayout = findViewById(R.id.layoutEmpty);

        chipAll = findViewById(R.id.btnFilterAll);
        chipTodo = findViewById(R.id.btnFilterTodo);
        chipInProgress = findViewById(R.id.btnFilterInProgress);
        chipInReview = findViewById(R.id.btnFilterInReview);
        chipDone = findViewById(R.id.btnFilterDone);
    }

    private void setupAdapter() {
        adapter = new TaskAdapter(this, new TaskAdapter.OnTaskClickListener() {
            @Override public void onTaskClick(TaskEntity task) {
                // TODO: mở TaskDetailActivity nếu cần
            }
            @Override public void onTaskLongClick(TaskEntity task) {
                // TODO: menu xóa/sửa
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);
    }

    private void loadTasks() {
        swipeRefresh.setRefreshing(true);
        AsyncTask.execute(() -> {
            List<TaskEntity> list = db.taskDAO().getAllTasks();
            runOnUiThread(() -> {
                all.clear();
                if (list != null) all.addAll(list);
                applyFilter("ALL");
                swipeRefresh.setRefreshing(false);
            });
        });
    }

    private void applyFilter(String status) {
        List<TaskEntity> show = new ArrayList<>();
        if ("ALL".equals(status)) {
            show.addAll(all);
        } else {
            for (TaskEntity t : all) {
                if (status.equalsIgnoreCase(t.status)) show.add(t);
            }
        }

        adapter.submitList(show);           // gọi hàm đã fix trong TaskAdapter
        adapter.notifyDataSetChanged();     // đảm bảo RecyclerView update

        tvCount.setText("Số task: " + show.size());
        emptyLayout.setVisibility(show.isEmpty() ? View.VISIBLE : View.GONE);
    }


    @Override

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadTasks();
        }
    }
}
