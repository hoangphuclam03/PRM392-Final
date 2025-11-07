package com.example.prm392.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import models.ChatMessageModel;
import models.Users;

public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder> {

    private final Context context;

    /** BẬT tên người gửi khi chat team */
    private final boolean showSenderName;
    /** cache uid -> display name để tránh query nhiều */
    private final Map<String, String> nameCache = new HashMap<>();
    private final String myUid = FirebaseUtil.currentUserId();

    /** Giữ nguyên constructor cũ (private chat) */
    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options,
                               Context context) {
        this(options, context, false);
    }

    /** Constructor cho team chat (truyền true để hiện tên) */
    public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options,
                               Context context,
                               boolean showSenderName) {
        super(options);
        this.context = context;
        this.showSenderName = showSenderName;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatModelViewHolder holder,
                                    int position,
                                    @NonNull ChatMessageModel model) {

        final String senderId = model.getSenderId();
        final boolean fromMe = senderId != null && senderId.equals(myUid);

        // --- Hiển thị bong bóng trái/phải như code cũ ---
        if (fromMe) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextview.setText(model.getMessage());
        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextview.setText(model.getMessage());
        }

        // --- Hiển thị tên người gửi cho team chat ---
        if (!showSenderName) {
            // Ẩn cả hai nếu không bật
            if (holder.leftSenderName != null)  holder.leftSenderName.setVisibility(View.GONE);
            if (holder.rightSenderName != null) holder.rightSenderName.setVisibility(View.GONE);
            return;
        }

        // Nếu là mình: hiện "Bạn" phía phải (nếu có TextView), ẩn phía trái
        if (fromMe) {
            if (holder.leftSenderName != null)  holder.leftSenderName.setVisibility(View.GONE);
            if (holder.rightSenderName != null) {
                holder.rightSenderName.setText("Bạn");
                holder.rightSenderName.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Tin nhắn người khác: hiện tên phía trái (nếu có TextView), ẩn phía phải
        if (holder.rightSenderName != null) holder.rightSenderName.setVisibility(View.GONE);
        if (holder.leftSenderName == null) return; // không có view để hiện -> thoát

        if (senderId == null || senderId.isEmpty()) {
            holder.leftSenderName.setText("Unknown");
            holder.leftSenderName.setVisibility(View.VISIBLE);
            return;
        }

        // Có cache rồi
        if (nameCache.containsKey(senderId)) {
            holder.leftSenderName.setText(nameCache.get(senderId));
            holder.leftSenderName.setVisibility(View.VISIBLE);
            return;
        }

        // Chưa có -> lấy từ Firestore
        // Ưu tiên docId == uid
        FirebaseUtil.usersCollection()
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {
                    String dn = extractDisplayName(doc);
                    if (dn == null) {
                        // Fallback whereEqualTo("uid", senderId)
                        FirebaseUtil.usersCollection()
                                .whereEqualTo("uid", senderId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    String name = "Unknown";
                                    if (!snap.isEmpty()) {
                                        Users u = snap.getDocuments().get(0).toObject(Users.class);
                                        name = makeDisplayName(u);
                                        if (name == null || name.isEmpty()) name = "Unknown";
                                    }
                                    nameCache.put(senderId, name);
                                    holder.leftSenderName.setText(name);
                                    holder.leftSenderName.setVisibility(View.VISIBLE);
                                })
                                .addOnFailureListener(e -> {
                                    nameCache.put(senderId, "Unknown");
                                    holder.leftSenderName.setText("Unknown");
                                    holder.leftSenderName.setVisibility(View.VISIBLE);
                                });
                    } else {
                        nameCache.put(senderId, dn);
                        holder.leftSenderName.setText(dn);
                        holder.leftSenderName.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    nameCache.put(senderId, "Unknown");
                    holder.leftSenderName.setText("Unknown");
                    holder.leftSenderName.setVisibility(View.VISIBLE);
                });
    }

    private String extractDisplayName(DocumentSnapshot doc) {
        if (doc != null && doc.exists()) {
            Users u = doc.toObject(Users.class);
            return makeDisplayName(u);
        }
        return null;
    }

    /** Ưu tiên username, fallback lastName + firstName */
    private String makeDisplayName(Users u) {
        if (u == null) return null;
        if (u.getUsername() != null && !u.getUsername().trim().isEmpty())
            return u.getUsername().trim();
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName()  != null ? u.getLastName().trim()  : "";
        String name = (ln + " " + fn).trim();
        return name.isEmpty() ? null : name;
    }

    @NonNull
    @Override
    public ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatModelViewHolder(view);
    }

    class ChatModelViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview;

        // NEW: TextView hiển thị tên người gửi (tùy chọn)
        TextView leftSenderName, rightSenderName;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout   = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout  = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview= itemView.findViewById(R.id.right_chat_textview);

            // Nếu bạn chưa thêm vào layout, các view này sẽ = null (an toàn)
            leftSenderName   = itemView.findViewById(R.id.left_sender_name);
            rightSenderName  = itemView.findViewById(R.id.right_sender_name);
        }
    }
}
