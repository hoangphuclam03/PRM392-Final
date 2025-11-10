package com.example.prm392.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.prm392.R;
import com.example.prm392.adapter.ChatRecyclerAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ChatDAO;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.utils.FirebaseUtil;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final String LOCAL_DRAFT = "_LOCAL_DRAFT_";

    // Project-only
    private String projectId;
    private boolean isDraft;

    private ChatRecyclerAdapter adapter;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private TextView projectTitle;

    private ChatDAO chatDAO;
    private LiveData<List<ChatEntity>> messagesLive;

    private androidx.drawerlayout.widget.DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;

    private ListenerRegistration msgListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ==== NAV UI ====
        initNavUI();
        setupNavigation();

        // Back gesture
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    ChatActivity.super.onBackPressed();
                }
            }
        });

        // ==== CHAT UI ====
        recyclerView   = findViewById(R.id.chat_recycler_view);
        messageInput   = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        projectTitle   = findViewById(R.id.other_username);
        ImageButton menuBtn = findViewById(R.id.btn_team_menu);
        if (menuBtn != null) menuBtn.setOnClickListener(this::showProjectMenu);

        chatDAO = AppDatabase.getInstance(this).chatDAO();

        // Nhận projectId (ưu tiên "projectId"; fallback "teamId" để tương thích cũ)
        String incomingId   = getIntent().getStringExtra("projectId");
        if (incomingId == null) incomingId = getIntent().getStringExtra("teamId");
        String incomingName = getIntent().getStringExtra("projectName");
        if (incomingName == null) incomingName = getIntent().getStringExtra("teamName");

        if (incomingId == null || incomingId.trim().isEmpty()) {
            projectId = LOCAL_DRAFT;
            isDraft   = true;
        } else {
            projectId = incomingId;
            isDraft   = false;
        }

        projectTitle.setText(isDraft ? "Tin nhắn (Chưa chọn Project)"
                : (incomingName != null ? incomingName : "Project"));

        adapter = new ChatRecyclerAdapter(this, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemAnimator ia = recyclerView.getItemAnimator();
        if (ia instanceof SimpleItemAnimator) ((SimpleItemAnimator) ia).setSupportsChangeAnimations(false);

        attachMessagesLive(projectId);

        // Lấy tên project & ĐẢM BẢO tồn tại doc projects/{id} để chứa subcollection messages
        if (!isDraft) {
            FirebaseUtil.projectRef(projectId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("projectName");
                    if (name != null) projectTitle.setText(name);
                }
            });
            ensureProjectExists(projectId, projectTitle.getText().toString());
        }

        sendMessageBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                messageInput.setText("");
            }
        });
    }

    private void initNavUI() {
        drawerLayout   = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar        = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_global_search)      startActivity(new Intent(this, GlobalSearchActivity.class));
            else if (id == R.id.nav_home)          startActivity(new Intent(this, HomeActivity.class));
            else if (id == R.id.nav_profile)       startActivity(new Intent(this, UserProfileActivity.class));
            else if (id == R.id.nav_chat)          startActivity(new Intent(this, ChatActivity.class));
            else if (id == R.id.nav_project)       startActivity(new Intent(this, ListYourProjectsActivity.class));
            else if (id == R.id.nav_my_tasks)      startActivity(new Intent(this, ListTasksActivity.class));
            else if (id == R.id.nav_settings)      startActivity(new Intent(this, SettingsActivity.class));
            else if (id == R.id.nav_calendar)      startActivity(new Intent(this, CalendarEventsActivity.class));
            else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    // ==== CHAT ====
    private void attachMessagesLive(String pid) {
        if (messagesLive != null) messagesLive.removeObservers(this);
        messagesLive = chatDAO.getByProjectLive(pid);
        messagesLive.observe(this, list -> {
            adapter.setChats(list);
            if (list != null && !list.isEmpty()) {
                recyclerView.scrollToPosition(list.size() - 1);
            }
        });
    }

    private void sendMessage(String text) {
        String myUid = FirebaseUtil.currentUserId();
        if (myUid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isDraft) {
            Toast.makeText(this, "Chưa chọn project.", Toast.LENGTH_SHORT).show();
            return;
        }

        long now   = System.currentTimeMillis();
        String msgId = java.util.UUID.randomUUID().toString();

        ChatEntity chat = new ChatEntity();
        chat.messageId     = msgId;
        chat.chatId        = null;
        chat.senderId      = myUid;
        chat.receiverId    = null;
        chat.projectId     = projectId;
        chat.message       = text;
        chat.timestamp     = now;
        chat.isPendingSync = true;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Lưu local để hiển thị ngay
            chatDAO.insert(chat);

            // Đẩy Firestore: projects/{projectId}/messages
            Map<String, Object> payload = new HashMap<>();
            payload.put("messageId", msgId);
            payload.put("senderId", chat.senderId);
            payload.put("receiverId", chat.receiverId);
            payload.put("projectId", chat.projectId);
            payload.put("message", chat.message);
            payload.put("timestamp", chat.timestamp);

            FirebaseUtil.projectMessagesRef(projectId)
                    .add(payload)
                    .addOnSuccessListener(ref -> {
                        Log.d("CHAT", "Uploaded: projects/" + projectId + "/messages/" + ref.getId());
                        AppDatabase.databaseWriteExecutor.execute(
                                () -> chatDAO.markUploaded(msgId, ref.getId())
                        );
                    })
                    .addOnFailureListener(e -> runOnUiThread(() ->
                            Toast.makeText(this, "Gửi Firestore lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    ));
        });
    }

    private void showProjectMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenuInflater().inflate(R.menu.menu_team, menu.getMenu()); // dùng lại menu nếu chưa đổi tên
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_browse_projects) {
                Intent it = new Intent(this, ListPublicProjectsActivity.class);
                startActivity(it);
                return true;
            }
            return false;
        });
        menu.show();
    }

    // Bảo đảm có doc projects/{projectId} để chứa subcollection messages
    private void ensureProjectExists(String pid, String name) {
        FirebaseUtil.projectRef(pid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) return; // đã có thì thôi
                    Map<String, Object> p = new HashMap<>();
                    p.put("projectId", pid);
                    p.put("projectName", (name != null && !name.isEmpty()) ? name : "Untitled");
                    p.put("description", "");
                    p.put("isPublic", true);
                    p.put("createdBy", FirebaseUtil.currentUserId());
                    long now = System.currentTimeMillis();
                    p.put("createdAt", now);
                    p.put("updatedAt", now);
                    FirebaseUtil.projectRef(pid).set(p)
                            .addOnSuccessListener(v -> Log.d("CHAT", "Created projects/" + pid))
                            .addOnFailureListener(e -> Log.e("CHAT", "Create project fail", e));
                })
                .addOnFailureListener(e -> Log.e("CHAT", "Check project fail", e));
    }

    // Realtime listener Firestore
    private void startFirestoreListener(String pid) {
        if (msgListener != null) msgListener.remove();
        msgListener = FirebaseUtil.projectMessagesRef(pid)
                .orderBy("timestamp")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { Log.e("CHAT", "listener", e); return; }
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String mid = doc.getString("messageId");
                            if (mid == null) continue;
                            ChatEntity c = new ChatEntity();
                            c.messageId     = mid;
                            c.chatId        = doc.getId();
                            c.senderId      = doc.getString("senderId");
                            c.receiverId    = doc.getString("receiverId");
                            c.projectId     = doc.getString("projectId");
                            c.message       = doc.getString("message");
                            Long ts         = doc.getLong("timestamp");
                            c.timestamp     = ts != null ? ts : 0L;
                            c.isPendingSync = false;
                            chatDAO.insertOrUpdate(c); // upsert theo messageId
                        }
                    });
                });
    }

    @Override protected void onStart() {
        super.onStart();
        if (!isDraft) startFirestoreListener(projectId);
    }

    @Override protected void onStop() {
        super.onStop();
        if (msgListener != null) { msgListener.remove(); msgListener = null; }
    }
}
