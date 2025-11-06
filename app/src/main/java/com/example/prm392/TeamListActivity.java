package com.example.prm392;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.model.TeamModel;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class TeamListActivity extends AppCompatActivity {

    private FirestoreRecyclerAdapter<TeamModel, VH> adapter;

    static class VH extends RecyclerView.ViewHolder {
        TextView name, members;
        VH(@NonNull View v) { super(v);
            name = v.findViewById(R.id.txtTeamName);
            members = v.findViewById(R.id.txtMembers);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_list);

        String uid = FirebaseUtil.currentUserId();
        RecyclerView rv = findViewById(R.id.rvTeams);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Query q = FirebaseUtil.teamsCollection()
                .whereArrayContains("members", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<TeamModel> opts =
                new FirestoreRecyclerOptions.Builder<TeamModel>()
                        .setQuery(q, TeamModel.class).build();

        adapter = new FirestoreRecyclerAdapter<TeamModel, VH>(opts) {
            @Override public void onBindViewHolder(@NonNull VH h, int p, @NonNull TeamModel m) {
                h.name.setText(m.getName());
                int count = (m.getMembers() != null) ? m.getMembers().size() : 0;
                h.members.setText(count + " thành viên");
                h.itemView.setOnClickListener(v -> {
                    // Trả về ChatActivity
                    Intent i = new Intent(TeamListActivity.this, ChatActivity.class);
                    i.putExtra("openTeamId", getSnapshots().getSnapshot(p).getId());
                    i.putExtra("openTeamName", m.getName());
                    startActivity(i);
                    finish();
                });
            }
            @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vType) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_team, p, false);
                return new VH(v);
            }
        };
        rv.setAdapter(adapter);
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop()  { super.onStop();  if (adapter != null) adapter.stopListening();  }
}
