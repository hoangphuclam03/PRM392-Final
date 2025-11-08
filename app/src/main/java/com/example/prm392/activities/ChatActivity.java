package com.example.prm392.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.adapter.ChatRecyclerAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ChatDAO;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.utils.FirebaseUtil;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String teamId;
    private ChatRecyclerAdapter adapter;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private TextView teamNameText;

    private ChatDAO chatDAO;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        teamNameText = findViewById(R.id.other_username);

        // ===== Room DAOs =====
        AppDatabase db = AppDatabase.getInstance(this);
        chatDAO = db.chatDAO();
        userDAO = db.userDAO();

        // ===== Get team ID =====
        teamId = getIntent().getStringExtra("teamId");
        if (teamId == null) {
            Toast.makeText(this, "Không có team để chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ===== Setup RecyclerView =====
        adapter = new ChatRecyclerAdapter(this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // ===== Observe messages from Room =====
        LiveData<List<ChatEntity>> messagesLive = chatDAO.getByProjectLive(teamId);
        messagesLive.observe(this, new Observer<List<ChatEntity>>() {
            @Override
            public void onChanged(List<ChatEntity> chatEntities) {
                adapter.setChats(chatEntities);
                recyclerView.scrollToPosition(chatEntities.size() - 1);
            }
        });

        // ===== Load team name =====
        FirebaseUtil.teamRef(teamId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                teamNameText.setText(name != null ? name : "Team");
            }
        });

        // ===== Send message =====
        sendMessageBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (msg.isEmpty()) return;
            sendMessage(msg);
            messageInput.setText("");
        });
    }

    private void sendMessage(String message) {
        String myUid = FirebaseUtil.currentUserId();
        if (myUid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();

        // ===== Create local ChatEntity =====
        ChatEntity chat = new ChatEntity();
        chat.projectId = teamId;
        chat.senderId = myUid;
        chat.message = message;
        chat.timestamp = timestamp;
        chat.isPendingSync = true;

        // Insert to Room
        AppDatabase.databaseWriteExecutor.execute(() -> chatDAO.insertOrUpdate(chat));

        // ===== Push to Firestore =====
        FirebaseUtil.teamMessagesRef(teamId)
                .add(new com.example.prm392.models.ChatEntity())
                .addOnSuccessListener(ref -> {
                    // mark local chat as synced
                    chat.isPendingSync = false;
                    AppDatabase.databaseWriteExecutor.execute(() -> chatDAO.insertOrUpdate(chat));
                });

        // ===== Send notification to team members =====
        FirebaseUtil.sendTeamNotification(teamId, message);
    }
}
