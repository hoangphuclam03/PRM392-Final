package com.example.prm392.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline-first list of all projects (teams) the current user belongs to. */
public class TeamListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView; // ✅ hiển thị khi rỗng
    private TeamAdapter adapter;

    private final List<ProjectEntity> projectList = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProjectDAO projectDAO;
    private ProjectMemberDAO memberDAO;
    private String currentUserId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);

        currentUserId = FirebaseUtil.currentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rvTeams);
        emptyView    = findViewById(R.id.emptyTeams); // ✅ nhớ thêm vào layout
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeamAdapter(projectList);
        recyclerView.setAdapter(adapter);

        AppDatabase db = AppDatabase.getInstance(this);
        projectDAO = db.projectDAO();
        memberDAO  = db.projectMemberDAO();

        loadUserTeams(); // Room trước
    }

    private void updateEmptyState() {
        boolean empty = projectList.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyView != null) emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    /** Load từ Room trước; nếu rỗng thì fallback online. */
    private void loadUserTeams() {
        Log.d("TEAM", "uid = " + FirebaseUtil.currentUserId());
        FirebaseFirestore.getInstance().setLoggingEnabled(true);
        executor.execute(() -> {
            List<ProjectMemberEntity> memberships = memberDAO.getMembersByUser(currentUserId);
            List<String> projectIds = new ArrayList<>();
            for (ProjectMemberEntity m : memberships) {
                if (m.projectId != null) projectIds.add(m.projectId);
            }

            List<ProjectEntity> projects = projectIds.isEmpty()
                    ? new ArrayList<>()
                    : projectDAO.getProjectsByIds(projectIds);

            projectList.clear();
            projectList.addAll(projects);

            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                updateEmptyState();
                if (projectList.isEmpty()) {
                    // ✅ Fallback lên Firestore nếu Room chưa có dữ liệu
                    loadUserTeamsOnlineFallback();
                    loadUserTeamsOnlineOwnerFallback();
                }
            });
        });
    }

    /** Lấy từ Firestore: duyệt tất cả teams, lọc những team có members/{currentUserId}. */
    private void loadUserTeamsOnlineFallback() {
        FirebaseUtil.teamsCollection()
                .get()
                .addOnSuccessListener(snap -> {
                    List<ProjectEntity> online = new ArrayList<>();
                    if (snap.isEmpty()) {
                        updateEmptyState();
                        return;
                    }
                    final int total = snap.size();
                    final int[] done = {0};

                    snap.getDocuments().forEach(teamDoc -> {
                        String teamId = teamDoc.getId();
                        String name   = teamDoc.getString("name");
                        String desc   = teamDoc.getString("description");

                        // kiểm tra thành viên: teams/{teamId}/members/{currentUserId}
                        FirebaseUtil.teamRef(teamId)
                                .collection("members")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(mem -> {
                                    if (mem.exists()) {
                                        ProjectEntity pe = new ProjectEntity();
                                        pe.projectId   = teamId;
                                        pe.projectName = (name != null ? name : "(no name)");
                                        pe.description = (desc != null ? desc : "");
                                        online.add(pe);

                                        // (tuỳ chọn) seed về Room:
                                        AppDatabase.databaseWriteExecutor.execute(() -> {
                                            AppDatabase db = AppDatabase.getInstance(this);
                                            db.projectDAO().upsert(pe);
                                            com.example.prm392.models.ProjectMemberEntity me =
                                                    new com.example.prm392.models.ProjectMemberEntity();
                                            me.projectId = teamId;
                                            me.userId = currentUserId;
                                            db.projectMemberDAO().upsert(me);
                                        });
                                    }
                                    if (++done[0] == total) {
                                        projectList.clear();
                                        projectList.addAll(online);
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (++done[0] == total) {
                                        projectList.clear();
                                        projectList.addAll(online);
                                        adapter.notifyDataSetChanged();
                                        updateEmptyState();
                                    }
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải team online: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }
    private void loadUserTeamsOnlineOwnerFallback() {
        final String uid = currentUserId;
        com.example.prm392.utils.FirebaseUtil.teamsCollection()
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;

                    List<com.example.prm392.models.ProjectEntity> online = new ArrayList<>();
                    snap.getDocuments().forEach(teamDoc -> {
                        com.example.prm392.models.ProjectEntity pe = new com.example.prm392.models.ProjectEntity();
                        pe.projectId   = teamDoc.getId();
                        pe.projectName = teamDoc.getString("name");
                        pe.description = teamDoc.getString("description");
                        if (pe.projectName == null) pe.projectName = "(no name)";
                        if (pe.description == null) pe.description = "";

                        online.add(pe);

                        // seed Room để lần sau load offline
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            db.projectDAO().upsert(pe);
                            com.example.prm392.models.ProjectMemberEntity me =
                                    new com.example.prm392.models.ProjectMemberEntity();
                            me.projectId = pe.projectId;
                            me.userId    = uid;
                            db.projectMemberDAO().upsert(me);
                        });
                    });

                    projectList.clear();
                    projectList.addAll(online);
                    adapter.notifyDataSetChanged();
                    // ẩn "Chưa có team"
                    View empty = findViewById(R.id.emptyTeams);
                    if (empty != null) empty.setVisibility(projectList.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(projectList.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("TEAM", "Owner fallback error: " + e.getMessage())
                );
    }

    // ============== RecyclerView Adapter ==============
    static class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {
        private final List<ProjectEntity> list;

        TeamAdapter(List<ProjectEntity> list) {
            this.list = list;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView txtTeamName, txtMembers;

            VH(@NonNull View v) {
                super(v);
                txtTeamName = v.findViewById(R.id.txtTeamName);
                txtMembers  = v.findViewById(R.id.txtMembers);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_team, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ProjectEntity project = list.get(position);
            holder.txtTeamName.setText(project.projectName);
            holder.txtMembers.setText(
                    (project.description != null && !project.description.isEmpty())
                            ? project.description : "No description"
            );

            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), ChatActivity.class);
                i.putExtra("teamId", project.projectId);
                i.putExtra("teamName", project.projectName);
                v.getContext().startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
