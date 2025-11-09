package com.example.prm392.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.SearchResultAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.SearchResultItem;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class GlobalSearchActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchResultAdapter adapter;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable workRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_search);

        searchView = findViewById(R.id.globalSearchView);
        recyclerView = findViewById(R.id.recyclerSearchResults);
        progressBar = findViewById(R.id.progressBarSearch);

        adapter = new SearchResultAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupSearch();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handler.removeCallbacks(workRunnable);
                workRunnable = () -> performSearch(newText.trim());
                handler.postDelayed(workRunnable, 300); // debounce 300ms
                return true;
            }
        });
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            adapter.setItems(new ArrayList<>());
            recyclerView.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            List<ProjectEntity> projects = db.projectDAO().searchProjects(query);
            List<TaskEntity> tasks = db.taskDAO().getAllTasks(); // we'll refine this below
            List<UserEntity> matchedUsers = db.userDAO().searchUsers(query);

            // filter tasks and users manually for now
            List<TaskEntity> matchedTasks = new ArrayList<>();
            for (TaskEntity t : tasks) {
                if (t.title != null && t.title.toLowerCase().contains(query.toLowerCase()))
                    matchedTasks.add(t);
                else if (t.description != null && t.description.toLowerCase().contains(query.toLowerCase()))
                    matchedTasks.add(t);
            }


            List<SearchResultItem> finalList = new ArrayList<>();

            if (!projects.isEmpty()) {
                finalList.add(new SearchResultItem(SearchResultItem.TYPE_HEADER, "Projects", null));
                for (ProjectEntity p : projects)
                    finalList.add(new SearchResultItem(SearchResultItem.TYPE_ITEM, p.projectName, p));
            }
            if (!matchedTasks.isEmpty()) {
                finalList.add(new SearchResultItem(SearchResultItem.TYPE_HEADER, "Tasks", null));
                for (TaskEntity t : matchedTasks)
                    finalList.add(new SearchResultItem(SearchResultItem.TYPE_ITEM, t.title, t));
            }
            if (!matchedUsers.isEmpty()) {
                finalList.add(new SearchResultItem(SearchResultItem.TYPE_HEADER, "Members", null));
                for (UserEntity u : matchedUsers)
                    finalList.add(new SearchResultItem(SearchResultItem.TYPE_ITEM, u.fullName, u));
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (finalList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setItems(finalList);
                }
            });
        });
    }
}
