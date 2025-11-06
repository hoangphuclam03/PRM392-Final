package com.example.prm392;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.model.UserModel;
import com.example.prm392.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveMemberActivity extends AppCompatActivity {

    private RecyclerView rv;
    private MemberAdapter adapter;
    private final List<UserModel> data = new ArrayList<>();

    private String teamId;
    private String ownerId;
    private static final String TAG = "TEAM_REMOVE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tái dùng layout search user có sẵn
        setContentView(R.layout.activity_search_user);
        setTitle("Xoá thành viên");
        // Nút quay lại
        android.widget.ImageButton back = findViewById(R.id.back_btn);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }


        teamId = getIntent().getStringExtra("teamId");
        if (teamId == null) {
            Toast.makeText(this, "Thiếu teamId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "RemoveMemberActivity started, teamId=" + teamId);

        rv = findViewById(R.id.search_user_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter(data, this::confirmRemove);
        rv.setAdapter(adapter);

        loadMembers();
    }

    private void loadMembers() {
        FirebaseUtil.teamRef(teamId).get().addOnSuccessListener(doc -> {
            List<String> memberIds = (List<String>) doc.get("members");
            ownerId = doc.getString("ownerId");

            if (memberIds == null || memberIds.isEmpty()) {
                Toast.makeText(this, "Team chưa có thành viên", Toast.LENGTH_SHORT).show();
                return;
            }

            // Không hiển thị owner để xoá
            Set<String> unique = new HashSet<>(memberIds);
            if (ownerId != null) unique.remove(ownerId);

            if (unique.isEmpty()) {
                Toast.makeText(this, "Chỉ có owner trong team", Toast.LENGTH_SHORT).show();
                return;
            }

            data.clear();
            List<String> allIds = new ArrayList<>(unique);
            fetchBatch(allIds, 0, 10);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "loadMembers failed", e);
            Toast.makeText(this, "Lỗi tải danh sách", Toast.LENGTH_SHORT).show();
        });
    }

    // do whereIn giới hạn 10 phần tử, fetch theo lô
    private void fetchBatch(List<String> allIds, int start, int batch) {
        int end = Math.min(start + batch, allIds.size());
        List<String> part = allIds.subList(start, end);
        Log.d(TAG, "fetchBatch " + start + " - " + (end-1));

        FirebaseUtil.allUserCollectionReference()
                .whereIn("userId", part)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot d : snap) {
                        UserModel u = d.toObject(UserModel.class);
                        if (u != null) {
                            if (u.getUserId() == null) u.setUserId(d.getId());
                            data.add(u);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (end < allIds.size()) fetchBatch(allIds, end, batch);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchBatch failed", e);
                    Toast.makeText(this, "Lỗi tải thành viên", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRemove(@NonNull UserModel user) {
        if (user.getUserId() == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xoá thành viên")
                .setMessage("Xoá " + (user.getUsername() != null ? user.getUsername() : user.getUserId()) + " khỏi team?")
                .setPositiveButton("Xoá", (d, w) -> removeMember(user))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void removeMember(@NonNull UserModel user) {
        String uid = user.getUserId();
        if (uid == null) return;
        if (ownerId != null && ownerId.equals(uid)) {
            Toast.makeText(this, "Không thể xoá owner", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtil.teamRef(teamId)
                .update("members", FieldValue.arrayRemove(uid), "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Removed " + uid + " successfully");
                    Toast.makeText(this, "Đã xoá " + (user.getUsername() != null ? user.getUsername() : uid), Toast.LENGTH_SHORT).show();

                    int index = -1;
                    for (int i = 0; i < data.size(); i++) {
                        if (uid.equals(data.get(i).getUserId())) { index = i; break; }
                    }
                    if (index >= 0) {
                        data.remove(index);
                        adapter.notifyItemRemoved(index);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Remove failed", e);
                    Toast.makeText(this, "Xoá thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Adapter đơn giản, tái dùng row search_user_recycler_row
    static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        interface OnClick { void onClick(UserModel m); }
        private final List<UserModel> list;
        private final OnClick cb;
        MemberAdapter(List<UserModel> list, OnClick cb) { this.list = list; this.cb = cb; }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvName, tvPhone;
            VH(@NonNull android.view.View itemView) {
                super(itemView);
                tvName  = itemView.findViewById(R.id.user_name_text);
                tvPhone = itemView.findViewById(R.id.phone_text);
            }
            void bind(UserModel u) {
                tvName.setText(u.getUsername() != null ? u.getUsername() : "(no name)");
                if (tvPhone != null) tvPhone.setText(u.getPhone() != null ? u.getPhone() : "");
            }
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vt) {
            android.view.View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.search_user_recycler_row, p, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            UserModel m = list.get(pos);
            h.bind(m);
            h.itemView.setOnClickListener(v -> cb.onClick(m));
        }

        @Override public int getItemCount() { return list.size(); }
    }
}
