package com.example.prm392.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.CalendarDotDecorator;
import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.TaskEntity;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarEventsActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private TextView tvSelectedDate;

    private List<TaskEntity> monthlyTasks = new ArrayList<>();
    private TasksAdapter tasksAdapter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private CalendarDotDecorator dotDecorator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_events);

        calendarView = findViewById(R.id.calendarView);
        rvTasks = findViewById(R.id.rvTasks);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        tasksAdapter = new TasksAdapter();
        rvTasks.setAdapter(tasksAdapter);

        int dotColor = ContextCompat.getColor(this, android.R.color.holo_red_dark);
        dotDecorator = new CalendarDotDecorator(dotColor, 5f);
        calendarView.addDecorator(dotDecorator);

        LocalDate today = LocalDate.now();
        calendarView.setSelectedDate(today);
        updateSelectedTitle(today);

        // Load tasks for current month
        loadMonthAndDecorate(today);

        calendarView.setOnMonthChangedListener((widget, date) -> {
            LocalDate firstDayOfMonth = date.getDate().withDayOfMonth(1);
            loadMonthAndDecorate(firstDayOfMonth);
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            LocalDate picked = date.getDate();
            updateSelectedTitle(picked);
            showTasksForDate(picked);
        });

        showTasksForDate(today);
    }

    /**
     * Update header text
     */
    private void updateSelectedTitle(LocalDate date) {
        tvSelectedDate.setText("Tasks on " + date.format(DATE_FMT));
    }

    /**
     * Load all tasks in the month and decorate dots
     */
    private void loadMonthAndDecorate(LocalDate anyDayOfMonth) {
        LocalDate first = anyDayOfMonth.withDayOfMonth(1);
        LocalDate last = first.plusMonths(1).minusDays(1);

        // Load from Room (blocking, in background thread)
        new Thread(() -> {
            List<TaskEntity> tasks = AppDatabase.getInstance(this)
                    .taskDAO()
                    .getTasksBetweenDates(first.format(DATE_FMT), last.format(DATE_FMT));

            monthlyTasks.clear();
            monthlyTasks.addAll(tasks);

            // Build set of dots
            Set<CalendarDay> dots = new HashSet<>();
            for (TaskEntity t : monthlyTasks) {
                if (t.dueDate != null) {
                    try {
                        LocalDate d = LocalDate.parse(t.dueDate, DATE_FMT);
                        dots.add(CalendarDay.from(d));
                    } catch (Exception ignored) {
                    }
                }
            }

            runOnUiThread(() -> {
                dotDecorator.setDates(dots);
                calendarView.invalidateDecorators();
            });
        }).start();
    }

    /**
     * Show tasks for a specific date
     */
    private void showTasksForDate(LocalDate date) {
        String key = date.format(DATE_FMT);
        List<TaskEntity> filtered = new ArrayList<>();
        for (TaskEntity t : monthlyTasks) {
            if (key.equals(t.dueDate)) {
                filtered.add(t);
            }
        }
        tasksAdapter.submit(filtered);
    }

    // ==========================================================
    // RecyclerView Adapter
    // ==========================================================
    private static class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.VH> {

        private final List<TaskEntity> data = new ArrayList<>();

        void submit(List<TaskEntity> newData) {
            data.clear();
            if (newData != null) data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_event_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TaskEntity t = data.get(position);
            holder.bind(t);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTaskId, tvEventDate;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTaskId = itemView.findViewById(R.id.tvTaskId);
                tvEventDate = itemView.findViewById(R.id.tvEventDate);
            }

            void bind(TaskEntity t) {
                tvTaskId.setText("Task ID: " + t.taskId);
                tvEventDate.setText("Date: " + t.dueDate);
            }
        }
    }
}
