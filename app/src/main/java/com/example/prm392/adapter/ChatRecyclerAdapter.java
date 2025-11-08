package com.example.prm392.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.models.UserEntity;
import com.example.prm392.utils.FirebaseUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.ChatViewHolder> {

    private final Context context;
    private final List<ChatEntity> chatList;
    private final HashMap<String, UserEntity> userCache = new HashMap<>();

    private final boolean showSenderName;
    private final String myUid;

    public ChatRecyclerAdapter(Context context, boolean showSenderName) {
        this.context = context;
        this.chatList = new ArrayList<>();
        this.showSenderName = showSenderName;
        this.myUid = FirebaseUtil.currentUserId();
    }

    public void setChats(List<ChatEntity> chats) {
        chatList.clear();
        if (chats != null) chatList.addAll(chats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.chat_message_recycler_row, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatEntity chat = chatList.get(position);
        boolean fromMe = chat.senderId != null && chat.senderId.equals(myUid);

        // --- Chat bubble logic ---
        if (fromMe) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatText.setText(chat.message);
        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatText.setText(chat.message);
        }

        // --- Show timestamp ---
        String time = new SimpleDateFormat("HH:mm").format(new Date(chat.timestamp));
        holder.leftChatTime.setText(time);
        holder.rightChatTime.setText(time);

        // --- Show sender name if enabled (team chat) ---
        if (!showSenderName) {
            if (holder.leftSenderName != null) holder.leftSenderName.setVisibility(View.GONE);
            if (holder.rightSenderName != null) holder.rightSenderName.setVisibility(View.GONE);
            return;
        }

        if (fromMe) {
            if (holder.leftSenderName != null) holder.leftSenderName.setVisibility(View.GONE);
            if (holder.rightSenderName != null) {
                holder.rightSenderName.setVisibility(View.VISIBLE);
                holder.rightSenderName.setText("You");
                holder.rightSenderName.setTextColor(Color.parseColor("#1976D2"));
            }
        } else {
            if (holder.rightSenderName != null) holder.rightSenderName.setVisibility(View.GONE);
            if (holder.leftSenderName != null) {
                holder.leftSenderName.setVisibility(View.VISIBLE);
                String senderName = getCachedUserName(chat.senderId);
                holder.leftSenderName.setText(senderName != null ? senderName : "Unknown");
                holder.leftSenderName.setTextColor(Color.parseColor("#7B1FA2"));
            }
        }
    }

    private String getCachedUserName(String senderId) {
        UserEntity cached = userCache.get(senderId);
        if (cached != null && cached.fullName != null && !cached.fullName.isEmpty()) {
            return cached.fullName;
        }
        // (Optional) If you have repository access here, you can fetch and cache later.
        return null;
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatText, rightChatText;
        TextView leftChatTime, rightChatTime;
        TextView leftSenderName, rightSenderName;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatText = itemView.findViewById(R.id.left_chat_textview);
            rightChatText = itemView.findViewById(R.id.right_chat_textview);
            leftSenderName = itemView.findViewById(R.id.left_sender_name);
            rightSenderName = itemView.findViewById(R.id.right_sender_name);
        }
    }
}
