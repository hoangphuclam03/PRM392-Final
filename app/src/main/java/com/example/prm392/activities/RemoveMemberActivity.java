package com.example.prm392.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoveMemberActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText searchInput;
    private MemberAdapter adapter;

    private final List<ProjectMemberEntity> membersShown = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProjectDAO projectDAO;
    private ProjectMemberDAO memberDAO;
    private String projectId;
    private String ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_member);


        AppDatabase db = AppDatabase.getInstance(this);
        projectDAO = db.projectDAO();
        memberDAO = db.projectMemberDAO();

        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            Toast.makeText(this, "Thiếu projectId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter(membersShown, this::confirmRemove);
        recyclerView.setAdapter(adapter);

        loadMembers();
    }

    private void loadMembers() {
        executor.execute(() -> {
            ProjectEntity project = projectDAO.getProjectById(projectId);
            if (project == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Không tìm thấy dự án", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            ownerId = project.ownerId;
            List<ProjectMemberEntity> all = memberDAO.getMembersByProject(projectId);
            membersShown.clear();

            for (ProjectMemberEntity m : all) {
                if (ownerId == null || !ownerId.equals(m.userId)) {
                    membersShown.add(m);
                }
            }

            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }

    private void confirmRemove(ProjectMemberEntity member) {
        new AlertDialog.Builder(this)
                .setTitle("Xoá thành viên")
                .setMessage("Xoá " + member.userId + " khỏi dự án?")
                .setPositiveButton("Xoá", (d, w) -> removeMember(member))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void removeMember(ProjectMemberEntity member) {
        if (ownerId != null && ownerId.equals(member.userId)) {
            Toast.makeText(this, "Không thể xoá chủ dự án", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            memberDAO.deleteByProject(projectId);
            member.pendingSync = true; // for future sync
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xoá thành viên!", Toast.LENGTH_SHORT).show();
                membersShown.remove(member);
                adapter.notifyDataSetChanged();
            });
        });
    }

    // ===== RecyclerView Adapter =====
    static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {
        interface OnClick {
            void onClick(ProjectMemberEntity u);
        }

        private final List<ProjectMemberEntity> list;
        private final OnClick listener;

        MemberAdapter(List<ProjectMemberEntity> l, OnClick cb) {
            this.list = l;
            this.listener = cb;
        }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvName;

            VH(@NonNull android.view.View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.user_name_text);
            }

            void bind(ProjectMemberEntity u) {
                tvName.setText(u.userId + " • " + u.role);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vt) {
            android.view.View v = android.view.LayoutInflater.from(p.getContext())
                    .inflate(R.layout.search_user_recycler_row, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ProjectMemberEntity u = list.get(pos);
            h.bind(u);
            h.itemView.setOnClickListener(v -> listener.onClick(u));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
