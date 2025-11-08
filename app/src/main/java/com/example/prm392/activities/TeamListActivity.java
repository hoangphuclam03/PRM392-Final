//package com.example.prm392.activities;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.prm392.R;
//import com.example.prm392.data.local.AppDatabase;
//import com.example.prm392.data.local.ProjectDAO;
//import com.example.prm392.models.ProjectEntity;
//import com.example.prm392.models.ProjectMemberEntity;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * Offline-first list of all projects (teams) the current user belongs to.
// */
//public class TeamListActivity extends AppCompatActivity {
//
//    private RecyclerView recyclerView;
//    private TeamAdapter adapter;
//
//    private final List<ProjectEntity> projectList = new ArrayList<>();
//    private final ExecutorService executor = Executors.newSingleThreadExecutor();
//
//    private ProjectDAO projectDAO;
//    private ProjectMemberDAO memberDAO;
//    private String currentUserId = "guest_user"; // replace with current logged in uid
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_team_list);
//
//        recyclerView = findViewById(R.id.rvTeams);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new TeamAdapter(projectList);
//        recyclerView.setAdapter(adapter);
//
//        AppDatabase db = AppDatabase.getInstance(this);
//        projectDAO = db.projectDAO();
//        memberDAO = db.projectMemberDAO();
//
//        // load all projects the user is a member of
//        loadUserTeams();
//    }
//
//    private void loadUserTeams() {
//        executor.execute(() -> {
//            List<ProjectMemberEntity> memberships = memberDAO.getMembersByUser(currentUserId);
//            List<String> projectIds = new ArrayList<>();
//            for (ProjectMemberEntity m : memberships) {
//                if (m.projectId != null) projectIds.add(m.projectId);
//            }
//
//            List<ProjectEntity> projects = projectIds.isEmpty()
//                    ? new ArrayList<>()
//                    : projectDAO.getProjectsByIds(projectIds);
//
//            projectList.clear();
//            projectList.addAll(projects);
//
//            runOnUiThread(() -> adapter.notifyDataSetChanged());
//        });
//    }
//
//    // ============== RecyclerView Adapter ==============
//    static class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.VH> {
//        private final List<ProjectEntity> list;
//
//        TeamAdapter(List<ProjectEntity> list) {
//            this.list = list;
//        }
//
//        static class VH extends RecyclerView.ViewHolder {
//            TextView txtTeamName, txtMembers;
//
//            VH(@NonNull View v) {
//                super(v);
//                txtTeamName = v.findViewById(R.id.txtTeamName);
//                txtMembers = v.findViewById(R.id.txtMembers);
//            }
//        }
//
//        @NonNull
//        @Override
//        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View v = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_team, parent, false);
//            return new VH(v);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull VH holder, int position) {
//            ProjectEntity project = list.get(position);
//            holder.txtTeamName.setText(project.projectName);
//            holder.txtMembers.setText(project.description != null ? project.description : "No description");
//
//            holder.itemView.setOnClickListener(v -> {
//                Intent i = new Intent(v.getContext(), ChatActivity.class);
//                i.putExtra("openTeamId", project.projectId);
//                i.putExtra("openTeamName", project.projectName);
//                v.getContext().startActivity(i);
//            });
//        }
//
//        @Override
//        public int getItemCount() {
//            return list.size();
//        }
//    }
//}
