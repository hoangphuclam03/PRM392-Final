package com.example.prm392.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.prm392.R;
import com.example.prm392.adapter.TaskAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.TaskEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class KanbanBoardActivity extends AppCompatActivity {

    private AppDatabase db;
    private String projectId;

    private RecyclerView rvTodo, rvInProgress, rvInReview, rvDone;
    private TaskAdapter adTodo, adInProgress, adInReview, adDone;
    private TextView tvTodoCount, tvInProgressCount, tvInReviewCount, tvDoneCount;

    private final List<TaskEntity> todo = new ArrayList<>();
    private final List<TaskEntity> inprogress = new ArrayList<>();
    private final List<TaskEntity> inreview = new ArrayList<>();
    private final List<TaskEntity> done = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kanban_board);

        db = AppDatabase.getInstance(getApplicationContext());
        projectId = getIntent().getStringExtra("PROJECT_ID");

        bindViews();
        setupColumn(rvTodo, (adTodo = mkAdapter()), "TODO");
        setupColumn(rvInProgress, (adInProgress = mkAdapter()), "INPROGRESS");
        setupColumn(rvInReview, (adInReview = mkAdapter()), "INREVIEW");
        setupColumn(rvDone, (adDone = mkAdapter()), "DONE");

        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadTasks());
        ((FloatingActionButton)findViewById(R.id.fabAddTask)).setOnClickListener(v -> {
            // mở CreateTaskActivity cho đúng project
            // startActivity(new Intent(this, CreateTaskActivity.class).putExtra("PROJECT_ID", projectId));
        });

        loadTasks();
    }

    private void bindViews() {
        rvTodo = findViewById(R.id.rvTodo);
        rvInProgress = findViewById(R.id.rvInProgress);
        rvInReview = findViewById(R.id.rvInReview);
        rvDone = findViewById(R.id.rvDone);
        tvTodoCount = findViewById(R.id.tvTodoCount);
        tvInProgressCount = findViewById(R.id.tvInProgressCount);
        tvInReviewCount = findViewById(R.id.tvInReviewCount);
        tvDoneCount = findViewById(R.id.tvDoneCount);
    }

    private TaskAdapter mkAdapter() {
        return new TaskAdapter(this, new TaskAdapter.OnTaskClickListener() {
            @Override public void onTaskClick(TaskEntity task) { }
            @Override public void onTaskLongClick(TaskEntity task) { }
        });
    }

    private void setupColumn(RecyclerView rv, TaskAdapter adapter, String status) {
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // drag & drop thay đổi status
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // không reorder trong 1 cột
            }
            @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(rv);
    }

    private void loadTasks() {
        AsyncTask.execute(() -> {
            List<TaskEntity> all = db.taskDAO().getAllTasks();
            List<TaskEntity> byProject = new ArrayList<>();
            if (projectId == null) byProject = all;
            else for (TaskEntity t : all) if (projectId.equals(t.projectId)) byProject.add(t);

            todo.clear(); inprogress.clear(); inreview.clear(); done.clear();
            for (TaskEntity t : byProject) {
                switch (t.status == null ? "" : t.status) {
                    case "INPROGRESS": inprogress.add(t); break;
                    case "INREVIEW": inreview.add(t); break;
                    case "DONE": done.add(t); break;
                    default: todo.add(t);
                }
            }
            runOnUiThread(() -> {
                adTodo.submitList(new ArrayList<>(todo));
                adInProgress.submitList(new ArrayList<>(inprogress));
                adInReview.submitList(new ArrayList<>(inreview));
                adDone.submitList(new ArrayList<>(done));

                tvTodoCount.setText(String.valueOf(todo.size()));
                tvInProgressCount.setText(String.valueOf(inprogress.size()));
                tvInReviewCount.setText(String.valueOf(inreview.size()));
                tvDoneCount.setText(String.valueOf(done.size()));
            });
        });
    }
}
