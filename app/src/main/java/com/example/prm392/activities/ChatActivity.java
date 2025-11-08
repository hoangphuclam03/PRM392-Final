package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.core.view.GravityCompat;


import com.example.prm392.R;
import com.example.prm392.adapter.ChatRecyclerAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ChatDAO;
import com.example.prm392.data.local.ProjectDAO;
import com.example.prm392.data.local.ProjectMemberDAO;
import com.example.prm392.data.local.UserDAO;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.utils.FirebaseUtil;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String LOCAL_DRAFT = "_LOCAL_DRAFT_";

    private String teamId;
    private boolean isDraft;

    private ChatRecyclerAdapter adapter;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private TextView teamNameText;

    private ChatDAO chatDAO;
    private UserDAO userDAO;
    private ProjectDAO projectDAO;
    private ProjectMemberDAO memberDAO;

    private androidx.lifecycle.LiveData<List<ChatEntity>> messagesLive;

    // ==== NAVIGATION DRAWER ====
    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ==== NAV UI ====
        initNavUI();
        setupNavigation();

        // ==== Back gesture (OnBackPressedDispatcher) ====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Không chặn back nữa -> chuyển về hành vi mặc định
                    setEnabled(false);
                    ChatActivity.super.onBackPressed();
                }
            }
        });

        // ==== CHAT UI ====
        recyclerView   = findViewById(R.id.chat_recycler_view);
        messageInput   = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        teamNameText   = findViewById(R.id.other_username);
        ImageButton teamMenuBtn = findViewById(R.id.btn_team_menu);
        if (teamMenuBtn != null) teamMenuBtn.setOnClickListener(this::showTeamMenu);

        AppDatabase db = AppDatabase.getInstance(this);
        chatDAO    = db.chatDAO();
        userDAO    = db.userDAO();
        projectDAO = db.projectDAO();
        memberDAO  = db.projectMemberDAO();

        // === nhận team từ Intent (có thể chưa có) ===
        String incoming = getIntent().getStringExtra("teamId");
        String incomingName = getIntent().getStringExtra("teamName");
        if (incoming == null || incoming.trim().isEmpty()) {
            teamId = LOCAL_DRAFT;
            isDraft = true;
        } else {
            teamId = incoming;
            isDraft = false;
        }

        teamNameText.setText(isDraft
                ? "Tin nhắn (chưa chọn team)"
                : (incomingName != null ? incomingName : "Team"));

        adapter = new ChatRecyclerAdapter(this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemAnimator ia = recyclerView.getItemAnimator();
        if (ia instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) ia).setSupportsChangeAnimations(false);
        }

        attachMessagesLive(teamId);

        if (!isDraft) {
            FirebaseUtil.teamRef(teamId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    if (name != null) teamNameText.setText(name);
                }
            });
        }

        sendMessageBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (msg.isEmpty()) return;
            sendMessage(msg);
            messageInput.setText("");
        });
    }

    // ==== NAV METHODS ====
    private void initNavUI() {
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar        = findViewById(R.id.toolbar); // MaterialToolbar trong layout

        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Hồ sơ cá nhân", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_chat) {
                // đang ở Chat → không làm gì thêm
            } else if (id == R.id.nav_project) {
                startActivity(new Intent(this, ListYourProjectsActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarEventsActivity.class));
            } else if (id == R.id.nav_logout) {
                // Tuỳ bạn: có thể signOut tại đây nếu muốn giống HomeActivity
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }



    // ==== CHAT METHODS ====
    private void attachMessagesLive(String projectId) {
        if (messagesLive != null) messagesLive.removeObservers(this);
        messagesLive = chatDAO.getByProjectLive(projectId);
        messagesLive.observe(this, chatEntities -> {
            adapter.setChats(chatEntities);
            if (chatEntities != null && !chatEntities.isEmpty()) {
                recyclerView.scrollToPosition(chatEntities.size() - 1);
            }
        });
    }

    private void switchToTeam(String newTeamId) {
        this.teamId = newTeamId;
        this.isDraft = false;
        teamNameText.setText("Team");
        attachMessagesLive(newTeamId);

        FirebaseUtil.teamRef(newTeamId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                if (name != null) teamNameText.setText(name);
            }
        });
    }

    private void sendMessage(String message) {
        String myUid = FirebaseUtil.currentUserId();
        if (myUid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        ChatEntity chat = new ChatEntity();
        chat.chatId = null;
        chat.senderId = myUid;
        chat.receiverId = null;
        chat.projectId = teamId;
        chat.message = message;
        chat.timestamp = now;
        chat.isPendingSync = !isDraft;

        // 1) Insert và LẤY localId
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long rowId = chatDAO.insert(chat);
            chat.localId = (int) rowId;  // giữ lại để update đúng bản ghi

            // 2) Nếu draft thì dừng (local only)
            if (isDraft) return;

            // 3) Đẩy Firestore
            Map<String, Object> payload = new HashMap<>();
            payload.put("senderId", chat.senderId);
            payload.put("receiverId", chat.receiverId);
            payload.put("projectId", chat.projectId);
            payload.put("message", chat.message);
            payload.put("timestamp", chat.timestamp);

            FirebaseUtil.teamMessagesRef(teamId)
                    .add(payload)
                    .addOnSuccessListener(ref -> AppDatabase.databaseWriteExecutor.execute(() -> {
                        // 4) Update lại CHÍNH DÒNG vừa insert (không tạo dòng mới)
                        chat.chatId = ref.getId();
                        chat.isPendingSync = false;
                    }))
                    .addOnFailureListener(e ->
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Gửi Firestore lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            )
                    );
        });
    }

    // ============ MENU ============

    private void showTeamMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(ChatActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_team, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_browse_projects) {
                // Mở danh sách dự án để gửi Join Request
                Intent it = new Intent(this, ListProjectsActivity.class);
                it.putExtra("mode", "join"); // gợi ý: để Activity ẩn FAB, chỉ hiển thị public
                startActivity(it);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    // ============ TẠO TEAM (đã sửa) ============

}
