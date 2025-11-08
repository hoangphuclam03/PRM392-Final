package com.example.prm392.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class AddMemberActivity extends AppCompatActivity {

    private RecyclerView rv;
    private EditText searchInput;
    private UserAdapter adapter;

    // List of all users from Room
    private List<UserEntity> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
        setTitle("Thêm thành viên");

        ImageButton back = findViewById(R.id.back_btn);
        if (back != null) back.setOnClickListener(v -> finish());

        rv = findViewById(R.id.search_user_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        searchInput = findViewById(R.id.search_username_input);

        adapter = new UserAdapter();
        rv.setAdapter(adapter);

        // TODO: Load users from Room
        loadUsersFromRoom();

        // Search/filter
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().trim().toLowerCase();
                    filterUsers(query);
                }
            });
        }
    }

    private void loadUsersFromRoom() {
        // Example: fetch all users from your Room database
        // Suppose you have a UserDao: List<UserEntity> getAllUsers()
        new Thread(() -> {
            allUsers = AppDatabase.getInstance(this).userDAO().getAll(); // blocking call in background
            runOnUiThread(() -> adapter.submitList(allUsers));
        }).start();
    }

    private void filterUsers(String query) {
        List<UserEntity> filtered = new ArrayList<>();
        for (UserEntity u : allUsers) {
            String email = u.email == null ? "" : u.email.toLowerCase();
            String name = u.fullName == null ? "" : u.fullName.toLowerCase();
            if (email.contains(query) || name.contains(query)) {
                filtered.add(u);
            }
        }
        adapter.submitList(filtered);
    }

    private void addMember(UserEntity user) {
        if (user == null || user.userId == null) {
            Toast.makeText(this, "User không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: implement adding to team in Room/Firebase as needed
        Toast.makeText(this, "Đã thêm: " + user.fullName, Toast.LENGTH_SHORT).show();
    }

    // ---------------- Adapter ----------------
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

        private final List<UserEntity> users = new ArrayList<>();

        void submitList(List<UserEntity> data) {
            users.clear();
            if (data != null) users.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.search_user_recycler_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            UserEntity u = users.get(position);
            holder.bind(u);
            holder.itemView.setOnClickListener(v -> addMember(u));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tv;

            VH(@NonNull View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.user_name_text);
            }

            void bind(UserEntity u) {
                String line = u.fullName == null || u.fullName.isEmpty() ? "(no name)" : u.fullName;
                if (u.email != null && !u.email.isEmpty()) line += " • " + u.email;
                tv.setText(line);
            }
        }
    }
}
