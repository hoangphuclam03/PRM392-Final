package com.example.prm392;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.adapter.ChatRecyclerAdapter;
import models.ChatMessageModel;
import models.ChatroomModel;
import models.Users;

import com.example.prm392.utils.AndroidUtil;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    // ===== Private chat state =====
    private Users otherUser;
    private String chatroomId;
    private ChatroomModel chatroomModel;

    // ===== Team chat state =====
    private boolean isTeamChat = false;
    private String teamId = null;
    private boolean restoredTeam = false;

    // ===== UI =====
    private ChatRecyclerAdapter adapter;
    private EditText messageInput;
    private ImageButton sendMessageBtn, backBtn;
    private TextView otherUsername;
    private RecyclerView recyclerView;
    private ImageView imageView;

    private static final String PREFS = "chat_prefs";
    private static final String KEY_LAST_TEAM_ID = "last_team_id";
    private static final String KEY_LAST_TEAM_NAME = "last_team_name";

    // Toolbar buttons
    private ImageButton btnAddMember;
    private ImageButton btnTeamMenu;

    // ===== Name helpers (đảm bảo luôn có tên) =====
    private String fullName(Users u) {
        if (u == null) return "Unknown";
        if (u.getUsername() != null && !u.getUsername().trim().isEmpty()) {
            return u.getUsername().trim();
        }
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String name = (ln + " " + fn).trim();
        return name.isEmpty() ? "Unknown" : name;
    }

    private String usersIdStr(Users u) {
        return (u == null) ? null : u.getUid(); // UID là String
    }

    // ====== Header bind (hiển thị tên & avatar) ======
    private void bindOtherUserHeader() {
        if (otherUser == null) {
            otherUsername.setText("Unknown");
            return;
        }

        otherUsername.setText(fullName(otherUser));

        String otherUid = usersIdStr(otherUser);
        if (otherUid == null) return;

        FirebaseUtil.getOtherProfilePicStorageRef(otherUid)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> AndroidUtil.setProfilePic(this, uri, imageView))
                .addOnFailureListener(e -> { /* ignore */ });
    }

    private void saveLastTeam(String id, String name) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_TEAM_ID, id)
                .putString(KEY_LAST_TEAM_NAME, name)
                .apply();
    }

    private void clearLastTeam() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_TEAM_ID)
                .remove(KEY_LAST_TEAM_NAME)
                .apply();
    }

    /** Gọi sớm trong onCreate(): nếu có team trước đó thì mở luôn */
    private void tryRestoreLastTeam() {
        String lastId = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LAST_TEAM_ID, null);
        if (lastId == null) return;

        String lastName = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(KEY_LAST_TEAM_NAME, "Team");

        isTeamChat = true;
        teamId = lastId;
        otherUser = null;
        otherUsername.setText(lastName);
        messageInput.setEnabled(true);
        sendMessageBtn.setEnabled(true);

        if (btnAddMember != null) btnAddMember.setVisibility(View.VISIBLE);
        if (btnTeamMenu != null) btnTeamMenu.setVisibility(View.VISIBLE);

        setupTeamRecyclerView();
        if (adapter != null) adapter.startListening();

        restoredTeam = true;
    }

    private void enterEmptyMode() {
        otherUsername.setText("Chưa chọn người");
        sendMessageBtn.setEnabled(false);
        messageInput.setEnabled(false);
        if (recyclerView != null) recyclerView.setAdapter(null);
        adapter = null;
        if (btnAddMember != null) btnAddMember.setVisibility(View.GONE);
    }

    private void initChatAfterHaveOther() {
        String myUid = FirebaseUtil.currentUserId();
        if (myUid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_LONG).show();
            return;
        }
        String otherUid = usersIdStr(otherUser);
        if (otherUser == null || otherUid == null) {
            enterEmptyMode();
            return;
        }

        isTeamChat = false;
        teamId = null;

        chatroomId = FirebaseUtil.getChatroomId(myUid, otherUid);
        getOrCreateChatroomModel();
        setupPrivateRecyclerView();

        sendMessageBtn.setEnabled(true);
        messageInput.setEnabled(true);
        if (btnAddMember != null) btnAddMember.setVisibility(View.GONE);
    }

    // ===== RecyclerView setups =====
    private void setupPrivateRecyclerView() {
        Query query = FirebaseUtil.chatroomMessagesRef(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(query, ChatMessageModel.class)
                        .build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext(), false);


        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void setupTeamRecyclerView() {
        if (teamId == null) return;

        Query q = FirebaseUtil.teamMessagesRef(teamId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options =
                new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                        .setQuery(q, ChatMessageModel.class)
                        .build();

/** BẬT showSenderName = true cho team */
        adapter = new ChatRecyclerAdapter(options, getApplicationContext(), true);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    // ===== Sending messages =====
    private void sendMessageToUser(String message) {
        String myUid = FirebaseUtil.currentUserId();
        if (myUid == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TEAM MODE
        if (isTeamChat) {
            if (teamId == null) {
                Toast.makeText(this, "Chưa có team.", Toast.LENGTH_SHORT).show();
                return;
            }
            ChatMessageModel chatMessageModel = new ChatMessageModel(message, myUid, Timestamp.now());
            FirebaseUtil.teamMessagesRef(teamId)
                    .add(chatMessageModel)
                    .addOnSuccessListener(ref -> {
                        FirebaseUtil.teamRef(teamId).update(
                                "lastMessage", message,
                                "lastMessageTimestamp", Timestamp.now(),
                                "updatedAt", Timestamp.now()
                        );
                        messageInput.setText("");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Gửi thất bại", Toast.LENGTH_SHORT).show()
                    );
            return;
        }

        // PRIVATE MODE
        String otherUid = usersIdStr(otherUser);
        if (chatroomId == null || otherUser == null || otherUid == null) {
            Toast.makeText(this, "Hãy chọn người để chat (nút +).", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatroomModel == null) {
            String myUid2 = FirebaseUtil.currentUserId();
            chatroomModel = new ChatroomModel(
                    chatroomId,
                    Arrays.asList(myUid2, otherUid),
                    Timestamp.now(),
                    ""
            );
        }
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(myUid);
        chatroomModel.setLastMessage(message);
        FirebaseUtil.chatroomRef(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, myUid, Timestamp.now());
        FirebaseUtil.chatroomMessagesRef(chatroomId)
                .add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        messageInput.setText("");
                        sendNotification(message);
                    }
                });
    }

    // ===== Private chatroom bootstrap =====
    private void getOrCreateChatroomModel() {
        FirebaseUtil.chatroomRef(chatroomId).get()
                .addOnSuccessListener(snap -> {
                    chatroomModel = snap.toObject(ChatroomModel.class);
                    if (chatroomModel == null) {
                        String myUid = FirebaseUtil.currentUserId();
                        String otherUid = usersIdStr(otherUser);
                        if (myUid == null || otherUid == null) return;
                        chatroomModel = new ChatroomModel(
                                chatroomId,
                                Arrays.asList(myUid, otherUid),
                                Timestamp.now(),
                                ""
                        );
                        FirebaseUtil.chatroomRef(chatroomId).set(chatroomModel);
                    }
                });
    }

    // ===== FCM push to other user (private chat) =====
    private void sendNotification(String message) {
        FirebaseUtil.currentUserDetails().get().addOnSuccessListener(doc -> {
            models.Users currentUser = doc.toObject(models.Users.class);
            if (currentUser == null) return;

            String token = null;
            try {
                java.lang.reflect.Method m = otherUser.getClass().getMethod("getFcmToken");
                Object val = m.invoke(otherUser);
                if (val != null) token = String.valueOf(val);
            } catch (Exception ignore) { /* Users chưa có fcmToken → skip */ }

            if (token == null || token.isEmpty()) {
                Log.w("FCM", "Other user has no FCM token. Skip notify.");
                return;
            }

            try {
                JSONObject root = new JSONObject();

                JSONObject notificationObj = new JSONObject();
                notificationObj.put("title", currentUser.getUsername()); // hoặc fullName(otherUser)
                notificationObj.put("body", message);

                JSONObject dataObj = new JSONObject();
                dataObj.put("userId", currentUser.getUid());

                root.put("notification", notificationObj);
                root.put("data", dataObj);
                root.put("to", token);

                callApi(root);
            } catch (Exception ignore) {}
        });
    }

    private void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "key=YOUR_SERVER_KEY_HERE") // TODO: đổi server key thật
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) { Log.e("FCM", "notify failed", e); }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) { Log.d("FCM", "notify status=" + response.code()); }
        });
    }

    // ===== Lifecycle =====
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput  = findViewById(R.id.chat_message_input);
        sendMessageBtn= findViewById(R.id.message_send_btn);
        backBtn       = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView  = findViewById(R.id.chat_recycler_view);
        imageView     = findViewById(R.id.profile_pic_image_view);
        btnTeamMenu   = findViewById(R.id.btn_team_menu);

        // ===== NHẬN TEAM TỪ TeamListActivity (ưu tiên) =====
        String openTeamId   = getIntent().getStringExtra("openTeamId");
        String openTeamName = getIntent().getStringExtra("openTeamName");

        // TH1: Có teamId -> mở trực tiếp
        if (openTeamId != null && !openTeamId.trim().isEmpty()) {
            isTeamChat = true;
            teamId = openTeamId;
            otherUser = null;

            otherUsername.setText(openTeamName != null ? openTeamName : "Team");
            messageInput.setEnabled(true);
            sendMessageBtn.setEnabled(true);
            if (btnAddMember != null) btnAddMember.setVisibility(View.VISIBLE);
            if (btnTeamMenu  != null) btnTeamMenu.setVisibility(View.VISIBLE);

            setupTeamRecyclerView();
            if (adapter != null) adapter.startListening();

            saveLastTeam(teamId, (openTeamName != null ? openTeamName : "Team"));
            restoredTeam = true;
        }
        // TH2: Không có teamId nhưng có tên -> tìm theo name rồi mở
        else if (openTeamName != null && !openTeamName.trim().isEmpty()) {
            openTeamByName(openTeamName.trim());
        }

        // ẩn + mặc định (hiện khi team mode)
        if (btnAddMember != null) btnAddMember.setVisibility(isTeamChat ? View.VISIBLE : View.GONE);
        if (btnTeamMenu != null)  btnTeamMenu.setVisibility(View.VISIBLE);

        if (!restoredTeam) enterEmptyMode();
        tryRestoreLastTeam(); // KHÔI PHỤC TEAM nếu có

        // Private chat (khi không mở team)
        if (!restoredTeam) {
            String otherUidFromIntent = getIntent().getStringExtra("userUid");
            if (otherUidFromIntent == null || otherUidFromIntent.trim().isEmpty()) {
                otherUidFromIntent = getIntent().getStringExtra("userId");
            }

            if (otherUidFromIntent != null && !otherUidFromIntent.trim().isEmpty()) {
                final String otherUid = otherUidFromIntent.trim();

                // docId == uid (khuyến nghị)
                FirebaseUtil.usersCollection()
                        .document(otherUid)
                        .get()
                        .addOnSuccessListener(doc -> handleUserDocLoaded(doc, otherUid))
                        .addOnFailureListener(e -> enterEmptyMode());
            } else {
                enterEmptyMode();
            }
        }

        if (backBtn != null) backBtn.setOnClickListener(v -> onBackPressed());

        if (sendMessageBtn != null) {
            sendMessageBtn.setOnClickListener(v -> {
                String message = messageInput != null ? messageInput.getText().toString().trim() : "";
                if (message.isEmpty()) return;
                sendMessageToUser(message);
            });
        }

        if (btnAddMember != null) {
            btnAddMember.setOnClickListener(v -> {
                if (!isTeamChat || teamId == null) {
                    Toast.makeText(this, "Hãy tạo team trước", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(ChatActivity.this, AddMemberActivity.class);
                i.putExtra("teamId", teamId);
                startActivity(i);
            });
        }

        if (btnTeamMenu != null) {
            btnTeamMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(ChatActivity.this, v, Gravity.END);
                popup.getMenuInflater().inflate(R.menu.menu_team, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_open_team) { // mở danh sách Team
                        startActivity(new Intent(ChatActivity.this, TeamListActivity.class));
                        return true;
                    } else if (id == R.id.action_add_team) {
                        createTeam(); return true;
                    } else if (id == R.id.action_rename_team) {
                        renameTeam(); return true;
                    } else if (id == R.id.action_delete_team) {
                        deleteTeam(); return true;
                    } else if (id == R.id.action_add_member) {
                        if (!isTeamChat || teamId == null) {
                            Toast.makeText(this, "Hãy mở hoặc tạo team trước", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent i = new Intent(ChatActivity.this, AddMemberActivity.class);
                            i.putExtra("teamId", teamId);
                            startActivity(i);
                        }
                        return true;
                    } else if (id == R.id.action_delete_member) {
                        if (!isTeamChat || teamId == null) {
                            Toast.makeText(this, "Hãy mở team trước", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        Intent i = new Intent(ChatActivity.this, RemoveMemberActivity.class);
                        i.putExtra("teamId", teamId);
                        startActivity(i);
                        return true;
                    } else if (id == R.id.action_open_team_by_name) {
                        final EditText input = new EditText(ChatActivity.this);
                        input.setHint("Nhập tên team");
                        new AlertDialog.Builder(ChatActivity.this)
                                .setTitle("Mở team theo tên")
                                .setView(input)
                                .setPositiveButton("Mở", (d, w) -> {
                                    String name = input.getText().toString().trim();
                                    if (!name.isEmpty()) openTeamByName(name);
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    /** Xử lý sau khi load document user theo docId = uid; nếu không có, fallback tìm theo field "uid" */
    private void handleUserDocLoaded(DocumentSnapshot doc, String expectedUid) {
        if (doc.exists()) {
            Users u = doc.toObject(Users.class);
            if (u != null) {
                if (u.getUid() == null || u.getUid().isEmpty()) {
                    u.setUid(doc.getId());
                }
                // Tự điền username nếu trống để header luôn có tên
                if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
                    String fn = u.getFirstName() != null ? u.getFirstName() : "";
                    String ln = u.getLastName()  != null ? u.getLastName()  : "";
                    String autoName = (ln + " " + fn).trim();
                    if (!autoName.isEmpty()) u.setUsername(autoName);
                }
                otherUser = u;
                bindOtherUserHeader();
                initChatAfterHaveOther();
                return;
            }
        }

        // Fallback: uid KHÔNG phải docId → tìm theo field "uid"
        FirebaseUtil.usersCollection()
                .whereEqualTo("uid", expectedUid)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Users u = snap.getDocuments().get(0).toObject(Users.class);
                        if (u != null) {
                            if (u.getUid() == null || u.getUid().isEmpty()) {
                                u.setUid(expectedUid);
                            }
                            if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
                                String fn = u.getFirstName() != null ? u.getFirstName() : "";
                                String ln = u.getLastName()  != null ? u.getLastName()  : "";
                                String autoName = (ln + " " + fn).trim();
                                if (!autoName.isEmpty()) u.setUsername(autoName);
                            }
                            otherUser = u;
                            bindOtherUserHeader();
                            initChatAfterHaveOther();
                            return;
                        }
                    }
                    enterEmptyMode();
                })
                .addOnFailureListener(e -> enterEmptyMode());
    }

    // ====== TEAM open-by-name helpers ======
    private void openTeamByDoc(DocumentSnapshot d) {
        String foundTeamId = d.getId();
        String foundName = String.valueOf(d.get("name"));

        isTeamChat = true;
        teamId = foundTeamId;
        otherUser = null;

        otherUsername.setText(foundName != null ? foundName : "Team");
        messageInput.setEnabled(true);
        sendMessageBtn.setEnabled(true);

        if (btnAddMember != null) btnAddMember.setVisibility(View.VISIBLE);
        if (btnTeamMenu  != null) btnTeamMenu.setVisibility(View.VISIBLE);

        setupTeamRecyclerView();
        if (adapter != null) adapter.startListening();

        saveLastTeam(teamId, (foundName != null ? foundName : "Team"));
        restoredTeam = true;
    }

    private void openTeamByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(this, "Tên team trống", Toast.LENGTH_SHORT).show();
            return;
        }
        String q = name.trim();

        FirebaseUtil.teamsCollection()
                .whereEqualTo("name_lower", q.toLowerCase(Locale.ROOT))
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        openTeamByDoc(snap.getDocuments().get(0));
                    } else {
                        openTeamByNameExactFallback(q);
                    }
                })
                .addOnFailureListener(e -> openTeamByNameExactFallback(q));
    }

    private void openTeamByNameExactFallback(String q) {
        FirebaseUtil.teamsCollection()
                .whereEqualTo("name", q)
                .limit(1)
                .get()
                .addOnSuccessListener(snap2 -> {
                    if (!snap2.isEmpty()) {
                        openTeamByDoc(snap2.getDocuments().get(0));
                    } else {
                        Toast.makeText(this, "Không tìm thấy team: " + q, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e2 ->
                        Toast.makeText(this, "Lỗi tìm team: " + e2.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ===== Team functions (full) =====
    private void createTeam() {
        final EditText input = new EditText(this);
        input.setHint("Nhập tên team");

        new AlertDialog.Builder(this)
                .setTitle("Tạo Team")
                .setView(input)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên team không được rỗng", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String ownerId = FirebaseUtil.currentUserId();
                    if (ownerId == null) {
                        Toast.makeText(this, "Bạn chưa đăng nhập.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    teamId = FirebaseUtil.teamsCollection().document().getId();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("name_lower", name.toLowerCase(Locale.ROOT)); // hỗ trợ tìm theo tên
                    data.put("ownerId", ownerId);
                    data.put("members", Arrays.asList(ownerId));
                    data.put("createdAt", Timestamp.now());
                    data.put("updatedAt", Timestamp.now());
                    data.put("lastMessage", "");
                    data.put("lastMessageTimestamp", null);

                    FirebaseUtil.teamRef(teamId).set(data)
                            .addOnSuccessListener(v2 -> {
                                isTeamChat = true;
                                otherUser = null;
                                otherUsername.setText(name);
                                messageInput.setEnabled(true);
                                sendMessageBtn.setEnabled(true);
                                setupTeamRecyclerView();
                                if (adapter != null) adapter.startListening();

                                saveLastTeam(teamId, name);
                                restoredTeam = true;
                                if (btnAddMember != null) btnAddMember.setVisibility(View.VISIBLE);

                                Toast.makeText(this, "Đã tạo team: " + name, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Tạo team thất bại", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void renameTeam() {
        if (!isTeamChat || teamId == null) {
            Toast.makeText(this, "Hãy tạo/mở team trước", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("Tên mới");

        new AlertDialog.Builder(this)
                .setTitle("Đổi tên team")
                .setView(input)
                .setPositiveButton("Đổi", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    FirebaseUtil.teamRef(teamId)
                            .update("name", name,
                                    "name_lower", name.toLowerCase(Locale.ROOT),
                                    "updatedAt", Timestamp.now())
                            .addOnSuccessListener(v -> {
                                otherUsername.setText(name);
                                saveLastTeam(teamId, name);
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTeam() {
        if (!isTeamChat || teamId == null) {
            Toast.makeText(this, "Hãy tạo/mở team trước", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xoá team?")
                .setMessage("Hành động này không thể hoàn tác.")
                .setPositiveButton("Xoá", (d, w) -> {
                    FirebaseUtil.teamRef(teamId).delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Đã xoá team", Toast.LENGTH_SHORT).show();
                                isTeamChat = false;
                                teamId = null;
                                enterEmptyMode();
                                if (adapter != null) adapter.stopListening();
                                recyclerView.setAdapter(null);

                                clearLastTeam();
                                restoredTeam = false;
                                if (btnAddMember != null) btnAddMember.setVisibility(View.GONE);
                                otherUsername.setText("Chưa chọn Team");
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Xoá thất bại", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
