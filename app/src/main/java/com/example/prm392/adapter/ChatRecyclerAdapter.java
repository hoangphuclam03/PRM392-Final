package com.example.prm392.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.utils.FirebaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.ChatVH> {

    private static final String TAG = "ChatAdapter";
    private final List<ChatEntity> data = new ArrayList<>();
    private final boolean isGroup;
    private String myUid; // KHÔNG final: login có thể thay đổi

    // Cache map: senderId -> fullName để không query Firestore nhiều lần
    private final Map<String, String> nameCache = new HashMap<>();

    public ChatRecyclerAdapter(@NonNull Context ctx, boolean isGroup) {
        this.isGroup = isGroup;
        this.myUid = FirebaseUtil.currentUserId();
        setHasStableIds(true); // giúp giảm nháy khi update
    }

    /** Cập nhật list bằng DiffUtil để tránh nháy toàn bộ */
    public void setChats(List<ChatEntity> newList) {
        if (newList == null) newList = new ArrayList<>();

        // ✅ BIẾN FINAL CHO DIFFUTIL
        final List<ChatEntity> finalNewList = newList;

        final String currUid = FirebaseUtil.currentUserId();
        if (!TextUtils.equals(currUid, myUid)) {
            myUid = currUid;
        }

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return data.size();
            }

            @Override
            public int getNewListSize() {
                return finalNewList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ChatEntity o = data.get(oldItemPosition);
                ChatEntity n = finalNewList.get(newItemPosition);

                if (o.chatId != null && n.chatId != null) return o.chatId.equals(n.chatId);
                if (o.localId != 0 && n.localId != 0) return o.localId == n.localId;
                return o.timestamp == n.timestamp && TextUtils.equals(o.senderId, n.senderId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ChatEntity o = data.get(oldItemPosition);
                ChatEntity n = finalNewList.get(newItemPosition);

                return TextUtils.equals(o.message, n.message)
                        && o.timestamp == n.timestamp
                        && TextUtils.equals(o.senderId, n.senderId)
                        && TextUtils.equals(o.projectId, n.projectId)
                        && TextUtils.equals(o.chatId, n.chatId)
                        && o.isPendingSync == n.isPendingSync;
            }
        });

        data.clear();
        data.addAll(finalNewList);
        diff.dispatchUpdatesTo(this);
    }

    /** Upsert 1 item (nếu bạn cần cập nhật đơn lẻ) */
    public void upsertOne(ChatEntity c) {
        int idx = -1;
        for (int i = 0; i < data.size(); i++) {
            ChatEntity it = data.get(i);
            boolean same;
            if (c.chatId != null && it.chatId != null) same = c.chatId.equals(it.chatId);
            else if (c.localId != 0 && it.localId != 0) same = c.localId == it.localId;
            else same = (c.timestamp == it.timestamp && TextUtils.equals(c.senderId, it.senderId));
            if (same) { idx = i; break; }
        }
        if (idx >= 0) {
            data.set(idx, c);
            notifyItemChanged(idx);
        } else {
            int pos = data.size();
            data.add(c);
            notifyItemInserted(pos);
        }
    }

    @Override
    public long getItemId(int position) {
        ChatEntity c = data.get(position);
        if (c.chatId != null) return c.chatId.hashCode();
        if (c.localId != 0) return c.localId;
        // tránh trùng: combine timestamp + sender hash
        return (c.timestamp ^ (c.senderId != null ? c.senderId.hashCode() : 0));
    }

    @NonNull
    @Override
    public ChatVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatVH h, int position) {
        ChatEntity c = data.get(position);
        final boolean isMine = (myUid != null && myUid.equals(c.senderId));
        final String msg = c.message == null ? "" : c.message;

        // Null-guard: nếu thiếu ID trong layout
        if (h.leftContainer == null || h.rightContainer == null ||
                h.leftText == null || h.rightText == null) {
            Log.e(TAG, "Layout ID mismatch. Kiểm tra chat_message_recycler_row.xml có đủ ID: " +
                    "left/right _chat_container, _chat_textview, _sender_name");
            return;
        }

        if (isMine) {
            h.rightContainer.setVisibility(View.VISIBLE);
            h.leftContainer.setVisibility(View.GONE);

            h.rightText.setText(msg);
            if (h.rightSender != null) {
                if (isGroup) {
                    h.rightSender.setVisibility(View.VISIBLE);
                    // nếu muốn cũng có thể hiện fullName của mình, tạm để "Bạn"
                    h.rightSender.setText("Bạn");
                } else h.rightSender.setVisibility(View.GONE);
            }
        } else {
            h.leftContainer.setVisibility(View.VISIBLE);
            h.rightContainer.setVisibility(View.GONE);

            h.leftText.setText(msg);
            if (h.leftSender != null) {
                if (isGroup) {
                    h.leftSender.setVisibility(View.VISIBLE);

                    String senderId = c.senderId;
                    if (TextUtils.isEmpty(senderId)) {
                        h.leftSender.setText("Ẩn danh");
                    } else {
                        // 1. Nếu đã có cache tên -> dùng luôn
                        String cached = nameCache.get(senderId);
                        if (cached != null) {
                            h.leftSender.setText(cached);
                        } else {
                            // 2. Chưa có: tạm hiện UID, rồi query Firestore để lấy fullName
                            h.leftSender.setText(senderId);

                            FirebaseUtil.userRef(senderId).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc == null || !doc.exists()) return;

                                        // ĐỔI "fullName" thành đúng tên field bạn lưu trong Firestore
                                        String fullName = doc.getString("fullName");
                                        if (TextUtils.isEmpty(fullName)) {
                                            fullName = senderId;
                                        }

                                        nameCache.put(senderId, fullName);

                                        int adapterPos = h.getAdapterPosition();
                                        if (adapterPos != RecyclerView.NO_POSITION
                                                && adapterPos < data.size()) {
                                            ChatEntity current = data.get(adapterPos);
                                            if (senderId.equals(current.senderId)) {
                                                h.leftSender.setText(fullName);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "load sender name fail", e)
                                    );
                        }
                    }

                } else h.leftSender.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class ChatVH extends RecyclerView.ViewHolder {
        LinearLayout leftContainer, rightContainer;
        TextView leftText, rightText, leftSender, rightSender;

        ChatVH(@NonNull View itemView) {
            super(itemView);
            leftContainer  = itemView.findViewById(R.id.left_chat_container);
            rightContainer = itemView.findViewById(R.id.right_chat_container);
            leftText       = itemView.findViewById(R.id.left_chat_textview);
            rightText      = itemView.findViewById(R.id.right_chat_textview);
            leftSender     = itemView.findViewById(R.id.left_sender_name);
            rightSender    = itemView.findViewById(R.id.right_sender_name);
        }
    }
}
