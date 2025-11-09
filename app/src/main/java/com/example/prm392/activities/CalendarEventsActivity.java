package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.CalendarDotDecorator;
import com.example.prm392.R;
import com.example.prm392.adapter.CalendarTaskAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.TaskEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.GravityCompat;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarEventsActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private MaterialCalendarView calendarView;
    private RecyclerView rvTasks;
    private TextView tvSelectedDate;

    private String projectId;
    private String currentUserId;

    private final List<TaskEntity> monthlyTasks = new ArrayList<>();
    private CalendarTaskAdapter tasksAdapter;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private CalendarDotDecorator dotDecorator;
    private int monthLoadToken = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_events);

        initNavUI();
        setupNavigation();


        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.START)) {
                    drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                } else {
                    setEnabled(false);
                    CalendarEventsActivity.super.onBackPressed();
                }
            }
        });

        // ---- Intent/Auth ----
        projectId = getIntent().getStringExtra("projectId");
        currentUserId = getSharedPreferences("auth", MODE_PRIVATE).getString("userId", "");

        // ---- Views ----
        calendarView = findViewById(R.id.calendarView);
        rvTasks      = findViewById(R.id.rvTasks);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        tasksAdapter = new CalendarTaskAdapter();
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
    private void initNavUI() {
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar        = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_global_search) {
                startActivity(new Intent(this, GlobalSearchActivity.class));
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
            } else if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatActivity.class));
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListYourProjectsActivity.class));
            } else if (id == R.id.nav_my_tasks) {
                startActivity(new Intent(this, ListTasksActivity.class)); // adjust name if different
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }


    private void updateSelectedTitle(LocalDate date) {
        tvSelectedDate.setText("Tasks on " + date.format(DATE_FMT));
    }

    private void loadMonthAndDecorate(LocalDate anyDayOfMonth) {
        final int myToken = ++monthLoadToken;
        LocalDate first = anyDayOfMonth.withDayOfMonth(1);
        LocalDate last  = first.plusMonths(1).minusDays(1);

        // clear dots while loading
        dotDecorator.setDates(new HashSet<>());
        calendarView.invalidateDecorators();

        new Thread(() -> {
            List<TaskEntity> tasks = AppDatabase.getInstance(CalendarEventsActivity.this)
                    .taskDAO()
                    .getMyProjectTasksBetweenDates(
                            projectId,
                            currentUserId,
                            first.format(DATE_FMT),
                            last.format(DATE_FMT)
                    );

            if (myToken != monthLoadToken) return;

            monthlyTasks.clear();
            monthlyTasks.addAll(tasks);

            Set<CalendarDay> dots = new HashSet<>();
            for (TaskEntity t : monthlyTasks) {
                String due = t.dueDate;
                if (due == null || due.length() < 10) continue;
                try {
                    LocalDate d = LocalDate.parse(due.substring(0, 10), DATE_FMT);
                    dots.add(CalendarDay.from(d));
                } catch (Exception ignored) {}
            }

            runOnUiThread(() -> {
                if (myToken != monthLoadToken) return;
                dotDecorator.setDates(dots);
                calendarView.invalidateDecorators();

                // refresh list for current selected date
                CalendarDay sel = calendarView.getSelectedDate();
                if (sel != null) showTasksForDate(sel.getDate());
            });
        }).start();
    }

    private void showTasksForDate(LocalDate date) {
        String key = date.format(DATE_FMT);
        List<TaskEntity> filtered = new ArrayList<>();
        for (TaskEntity t : monthlyTasks) {
            String due = t.dueDate;
            String day = (due != null && due.length() >= 10) ? due.substring(0, 10) : "";
            if (key.equals(day)) filtered.add(t);
        }
        tasksAdapter.submit(filtered);
    }
}
