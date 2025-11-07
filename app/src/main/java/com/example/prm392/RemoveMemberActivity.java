package com.example.prm392;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.Users;

public class RemoveMemberActivity extends AppCompatActivity {

    private static final String TAG = "TEAM_REMOVE";

    private RecyclerView rv;
    private MemberAdapter adapter;

    /** dataAll/documentIds: toàn bộ thành viên (trừ owner) + uid tương ứng
     *  dataShown: danh sách đang hiển thị sau khi filter theo email */
    private final List<MemberItem> dataAll = new ArrayList<>();
    private final List<MemberItem> dataShown = new ArrayList<>();

    private String teamId;
    private String ownerId; // owner uid dạng chuỗi
    private EditText searchInput; // @+id/search_input nếu layout có

    /** Cặp (uid docId, Users data) để xoá theo đúng chuỗi trong team.members */
    static class MemberItem {
        final String uid;   // chính là documentId trong collection users
        final Users  user;  // dữ liệu hiển thị
        MemberItem(String uid, Users user) { this.uid = uid; this.user = user; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
        setTitle("Xoá thành viên");

        // Back
        android.widget.ImageButton back = findViewById(R.id.back_btn);
        if (back != null) back.setOnClickListener(v -> finish());

        teamId = getIntent().getStringExtra("teamId");
        if (teamId == null) {
            Toast.makeText(this, "Thiếu teamId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        searchInput = findViewById(R.id.search_username_input);

        rv = findViewById(R.id.search_user_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter(dataShown, this::confirmRemove);
        rv.setAdapter(adapter);

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    applyEmailFilter(s.toString());
                }
            });
        }

        loadMembers();
    }

    /** Tải danh sách uid thành viên từ team (trừ owner), sau đó fetch theo FieldPath.documentId() theo lô 10 */
    private void loadMembers() {
        FirebaseUtil.teamRef(teamId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> memberIds = (List<String>) doc.get("members"); // ["38j0dG...", "CiPx3...", "1", ...]
                    ownerId = doc.getString("ownerId");                         // ví dụ "38j0dG..."

                    Log.d(TAG, "members raw = " + memberIds + ", ownerId=" + ownerId);

                    if (memberIds == null || memberIds.isEmpty()) {
                        Toast.makeText(this, "Team chưa có thành viên", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // loại owner
                    Set<String> set = new HashSet<>(memberIds);
                    if (ownerId != null) set.remove(ownerId);

                    if (set.isEmpty()) {
                        Toast.makeText(this, "Chỉ còn owner trong team", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> uids = new ArrayList<>(set);

                    dataAll.clear();
                    dataShown.clear();
                    fetchBatchByDocId(uids, 0, 10);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadMembers ERROR", e);
                    Toast.makeText(this, "Lỗi tải danh sách thành viên", Toast.LENGTH_SHORT).show();
                });
    }

    /** Fetch theo FieldPath.documentId() — tối đa 10 uid/lần */
    private void fetchBatchByDocId(List<String> all, int start, int size) {
        int end = Math.min(start + size, all.size());
        List<String> part = all.subList(start, end);
        Log.d(TAG, "fetchBatchByDocId " + start + " - " + (end - 1) + " | part=" + part);

        FirebaseUtil.usersCollection()
                .whereIn(FieldPath.documentId(), part)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot d : snap) {
                        Users u = d.toObject(Users.class);
                        if (u != null) {
                            dataAll.add(new MemberItem(d.getId(), u)); // gắn uid đúng
                        }
                    }
                    // áp filter hiện tại
                    String key = (searchInput != null && searchInput.getText() != null)
                            ? searchInput.getText().toString()
                            : "";
                    applyEmailFilter(key);

                    if (end < all.size()) {
                        fetchBatchByDocId(all, end, size);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchBatchByDocId ERROR", e);
                    Toast.makeText(this, "Lỗi tải user", Toast.LENGTH_SHORT).show();
                });
    }

    /** Lọc cục bộ theo email trên dataAll */
    private void applyEmailFilter(String keyRaw) {
        String key = keyRaw == null ? "" : keyRaw.trim().toLowerCase();

        dataShown.clear();
        if (key.isEmpty()) {
            dataShown.addAll(dataAll);
        } else {
            for (MemberItem mi : dataAll) {
                String email = mi.user.getEmail() == null ? "" : mi.user.getEmail().trim().toLowerCase();
                if (email.contains(key)) {
                    dataShown.add(mi);
                }
            }
        }
        adapter.notifyDataSetChanged();

        Log.d(TAG, "applyEmailFilter key=" + key + " → shown=" + dataShown.size() + "/" + dataAll.size());
        if (dataShown.isEmpty()) {
            Toast.makeText(this, "Không có thành viên khớp email", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemove(@NonNull MemberItem mi) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá thành viên")
                .setMessage("Xoá " + safeName(mi.user) + " khỏi team?")
                .setPositiveButton("Xoá", (d, w) -> removeMember(mi))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private String safeName(Users u) {
        String name = (u.getUsername() != null && !u.getUsername().trim().isEmpty())
                ? u.getUsername()
                : (((u.getLastName() != null ? u.getLastName() : "") + " " +
                (u.getFirstName() != null ? u.getFirstName() : "")).trim());
        if (name == null || name.isEmpty()) name = "(no name)";
        String email = u.getEmail() == null ? "" : u.getEmail().trim();
        return email.isEmpty() ? name : (name + " • " + email);
    }

    private void removeMember(@NonNull MemberItem mi) {
        String uid = mi.uid; // CHUỖI docId đúng như trong team.members

        if (ownerId != null && ownerId.equals(uid)) {
            Toast.makeText(this, "Không thể xoá owner", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtil.teamRef(teamId)
                .update("members", FieldValue.arrayRemove(uid),
                        "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đã xoá " + safeName(mi.user), Toast.LENGTH_SHORT).show();

                    // Gỡ khỏi 2 danh sách
                    int idxAll = indexOfUid(dataAll, uid);
                    if (idxAll >= 0) dataAll.remove(idxAll);

                    int idxShown = indexOfUid(dataShown, uid);
                    if (idxShown >= 0) {
                        dataShown.remove(idxShown);
                        adapter.notifyItemRemoved(idxShown);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "removeMember ERROR", e);
                    Toast.makeText(this, "Xoá thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private int indexOfUid(List<MemberItem> list, String uid) {
        for (int i = 0; i < list.size(); i++) {
            if (uid.equals(list.get(i).uid)) return i;
        }
        return -1;
    }

    // ========= Adapter hiển thị =========
    static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        interface OnClick { void onClick(MemberItem u); }
        private final List<MemberItem> list;
        private final OnClick listener;

        MemberAdapter(List<MemberItem> l, OnClick cb) {
            this.list = l;
            this.listener = cb;
        }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvName; // dùng @id/user_name_text trong row
            VH(@NonNull android.view.View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.user_name_text);
            }
            void bind(MemberItem mi) {
                String name = (mi.user.getUsername() != null && !mi.user.getUsername().trim().isEmpty())
                        ? mi.user.getUsername()
                        : (((mi.user.getLastName() != null ? mi.user.getLastName() : "") + " " +
                        (mi.user.getFirstName() != null ? mi.user.getFirstName() : "")).trim());
                if (name == null || name.isEmpty()) name = "(no name)";
                String email = mi.user.getEmail() == null ? "" : mi.user.getEmail().trim();
                tvName.setText(email.isEmpty() ? name : (name + " • " + email));
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vt) {
            android.view.View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.search_user_recycler_row, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            MemberItem u = list.get(pos);
            h.bind(u);
            h.itemView.setOnClickListener(v -> listener.onClick(u));
        }

        @Override
        public int getItemCount() { return list.size(); }
    }
}
