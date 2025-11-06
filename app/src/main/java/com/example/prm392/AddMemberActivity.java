package com.example.prm392;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.model.UserModel;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;


public class AddMemberActivity extends AppCompatActivity {
    private String teamId;
    private RecyclerView rv;

    @Override protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);
        setTitle("Thêm thành viên");
        android.widget.ImageButton back = findViewById(R.id.back_btn);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        teamId = getIntent().getStringExtra("teamId");
        if (teamId == null) { Toast.makeText(this, "Thiếu teamId", Toast.LENGTH_SHORT).show(); finish(); return; }

        rv = findViewById(R.id.search_user_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Query q = FirebaseUtil.allUserCollectionReference().orderBy("username");
        FirestoreRecyclerOptions<UserModel> opt = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(q, UserModel.class).build();

        FirestoreRecyclerAdapter<UserModel, VH> adapter =
                new FirestoreRecyclerAdapter<UserModel, VH>(opt) {
                    @Override protected void onBindViewHolder(@NonNull VH h, int pos, @NonNull UserModel m) {
                        h.bind(m);
                        h.itemView.setOnClickListener(v -> addMember(m.getUserId()));
                    }
                    @NonNull @Override public VH onCreateViewHolder(@NonNull android.view.ViewGroup p, int vt) {
                        android.view.View v = getLayoutInflater().inflate(R.layout.search_user_recycler_row, p, false);
                        return new VH(v);
                    }
                };
        rv.setAdapter(adapter);
        adapter.startListening();
    }

    private void addMember(String userId) {
        if (userId == null) return;
        FirebaseUtil.teamRef(teamId)
                .update("members", FieldValue.arrayUnion(userId), "updatedAt", Timestamp.now())
                .addOnSuccessListener(v -> Toast.makeText(this, "Đã thêm", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Thêm thất bại", Toast.LENGTH_SHORT).show());
    }

    static class VH extends RecyclerView.ViewHolder {
        android.widget.TextView tv;
        VH(@NonNull android.view.View itemView) { super(itemView); tv = itemView.findViewById(R.id.user_name_text); }
        void bind(UserModel m) { tv.setText(m.getUsername()); }
    }
}
