package com.example.prm392.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.activities.ChatActivity;
import com.example.prm392.R;
import com.example.prm392.utils.AndroidUtil;
import com.example.prm392.utils.FirebaseUtil;
import com.example.prm392.models.ChatroomModel;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class RecentChatRecyclerAdapter extends RecyclerView.Adapter<RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    private final Context context;
    private final List<ChatroomModel> chatrooms = new ArrayList<>();

    public RecentChatRecyclerAdapter(Context context) {
        this.context = context;
    }

    public void setChatrooms(List<ChatroomModel> data) {
        chatrooms.clear();
        if (data != null) chatrooms.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position) {
        ChatroomModel model = chatrooms.get(position);
        String otherUserId = model.otherUserId;
        String myId = FirebaseUtil.currentUserId();

        if (otherUserId == null) return;

        // Load user info from Firebase for avatar and username
        FirebaseUtil.usersCollection()
                .document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    UserEntity other = doc.toObject(UserEntity.class);
                    if (other == null) return;

                    // Avatar
                    FirebaseUtil.getOtherProfilePicStorageRef(otherUserId)
                            .getDownloadUrl()
                            .addOnSuccessListener(uri -> AndroidUtil.setProfilePic(context, uri, holder.profilePic));

                    // Username
                    holder.usernameText.setText(other.fullName != null ? other.fullName : "Unknown");

                    // Last message text
                    boolean lastByMe = model.lastMessageSenderId != null && model.lastMessageSenderId.equals(myId);
                    holder.lastMessageText.setText(lastByMe ? "You: " + model.lastMessage : model.lastMessage);

                    // Open ChatActivity on click
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("userId", otherUserId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    });
                });
    }

    @Override
    public int getItemCount() {
        return chatrooms.size();
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, lastMessageText, lastMessageTime;
        ImageView profilePic;

        ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
