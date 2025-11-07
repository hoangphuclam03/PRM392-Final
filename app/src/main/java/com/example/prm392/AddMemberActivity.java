package com.example.prm392;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import models.Users;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AddMemberActivity extends AppCompatActivity {
    private static final String TAG = "TEAM_ADD";

    private String teamId;
    private RecyclerView rv;
    private FirestoreRecyclerAdapter<Users, VH> adapter;
    private EditText searchInput; // optional: nếu layout có @id/search_input

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
        setTitle("Thêm thành viên");

        // nút back
        ImageButton back = findViewById(R.id.back_btn);
        if (back != null) back.setOnClickListener(v -> finish());

        teamId = getIntent().getStringExtra("teamId");
        if (teamId == null) {
            Toast.makeText(this, "Thiếu teamId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rv = findViewById(R.id.search_user_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        searchInput = findViewById(R.id.search_username_input); // nếu layout có ô tìm kiếm

        // Adapter mặc định: liệt kê theo email/email_lower
        FirestoreRecyclerOptions<Users> opt = new FirestoreRecyclerOptions.Builder<Users>()
                .setQuery(buildEmailQuery(null), Users.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Users, VH>(opt) {
            @Override protected void onBindViewHolder(@NonNull VH h, int pos, @NonNull Users u) {
                h.bind(u);
                h.itemView.setOnClickListener(v -> {
                    Log.d(TAG, "Selected user: id=" + u.getUid() + ", email=" + u.getEmail());
                    addMember(String.valueOf(u.getUid()));
                });
            }

            @NonNull
            @Override
            public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                View v = getLayoutInflater().inflate(R.layout.search_user_recycler_row, p, false);
                return new VH(v);
            }

            @Override public void onDataChanged() {
                super.onDataChanged();
                Log.d(TAG, "adapter itemCount = " + getItemCount());
                if (getItemCount() == 0) {
                    Log.d(TAG, "query: email filter yielded 0 docs");
                    Toast.makeText(AddMemberActivity.this, "Không có người dùng khớp email", Toast.LENGTH_SHORT).show();
                }
            }
        };
        rv.setAdapter(adapter);

        // DEBUG: In 5 user đầu để kiểm tra field email/email_lower
        FirebaseUtil.usersCollection()
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "users top5 count = " + snap.size());
                    for (QueryDocumentSnapshot d : snap) {
                        Object e = d.get("email");
                        Object el = d.get("email_lower");
                        Log.d(TAG, "docId=" + d.getId() + " email=" + e + " | email_lower=" + el);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "count users FAILED", e));

        // Lọc theo email khi gõ (nếu có ô tìm kiếm)
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    String key = s.toString();
                    FirestoreRecyclerOptions<Users> newOpt = new FirestoreRecyclerOptions.Builder<Users>()
                            .setQuery(buildEmailQuery(key), Users.class)
                            .build();
                    adapter.updateOptions(newOpt);
                }
            });
        }
    }

    /**
     * Tạo query theo email:
     * - Nếu có field email_lower: dùng email_lower (khuyến nghị lưu lowercase ngay khi tạo user)
     * - Nếu không có: fallback orderBy("email") + prefix hoặc whereEqualTo("email") exact
     *
     * Logic:
     *  - key rỗng -> list theo email_lower/email
     *  - key có '@' -> ưu tiên exact (email_lower == keyLower) (nếu không có email_lower thì whereEqualTo("email", key))
     *  - key chưa đủ -> prefix startAt/endAt với email_lower/email
     */
    private Query buildEmailQuery(String keyRaw) {
        String key = (keyRaw == null) ? "" : keyRaw.trim();
        String keyLower = key.toLowerCase();      // email của bạn đều lowercase → ổn

        // Chưa gõ gì → liệt kê theo email
        if (key.isEmpty()) {
            return FirebaseUtil.usersCollection()
                    .orderBy("email")            // 🔁 dùng email
                    .limit(50);
        }

        // Có '@' → ưu tiên exact match
        if (key.contains("@")) {
            return FirebaseUtil.usersCollection()
                    .whereEqualTo("email", keyLower) // exact trên email (thường là lowercase)
                    .limit(50);
        }

        // Prefix search theo email (a, ab, abc…)
        return FirebaseUtil.usersCollection()
                .orderBy("email")
                .startAt(keyLower)
                .endAt(keyLower + "\uf8ff")
                .limit(50);
    }

    @Override protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    private void addMember(String userKey) {
        if (userKey == null || userKey.isEmpty()) {
            Toast.makeText(this, "User không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUtil.teamRef(teamId)
                .update("members", FieldValue.arrayUnion(userKey),
                        "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã thêm", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addMember FAILED", e);
                    Toast.makeText(this, "Thêm thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    static class VH extends RecyclerView.ViewHolder {
        android.widget.TextView tv;
        VH(@NonNull android.view.View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.user_name_text);
        }
        void bind(Users u) {
            // Dòng hiển thị: Tên (Email)
            String name = (u.getUsername() != null && !u.getUsername().trim().isEmpty())
                    ? u.getUsername()
                    : (((u.getLastName() != null ? u.getLastName() : "") + " " +
                    (u.getFirstName() != null ? u.getFirstName() : "")).trim());

            String email = u.getEmail() == null ? "" : u.getEmail();
            String line = (name == null || name.isEmpty()) ? "(no name)" : name;
            if (!email.isEmpty()) line = line + " • " + email;

            tv.setText(line);
            Log.d(TAG, "Bind: id=" + u.getUid() + ", email=" + u.getEmail() + ", name=" + name);
        }
    }
}
