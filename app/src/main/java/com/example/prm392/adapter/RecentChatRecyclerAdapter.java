package com.example.prm392.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.ChatActivity;
import com.example.prm392.R;
import com.example.prm392.utils.AndroidUtil;
import com.example.prm392.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

import models.ChatroomModel;
import models.Users;

public class RecentChatRecyclerAdapter
        extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options,
                                     Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder,
                                    int position,
                                    @NonNull ChatroomModel model) {

        List<String> ids = model.getUserIds();
        String myId = FirebaseUtil.currentUserId();

        String otherId = null;
        if (ids != null) {
            for (String id : ids) {
                if (!id.equals(myId)) {
                    otherId = id;
                    break;
                }
            }
        }

        if (otherId == null) return;

        final String otherIdFinal = otherId; // ✅ quan trọng

        FirebaseUtil.allUserCollectionReference()
                .whereEqualTo("id", Integer.parseInt(otherIdFinal))
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Users other = snap.getDocuments().get(0).toObject(Users.class);
                        if (other == null) return;

                        FirebaseUtil.getOtherProfilePicStorageRef(otherIdFinal)
                                .getDownloadUrl()
                                .addOnSuccessListener(uri -> AndroidUtil.setProfilePic(context, uri, holder.profilePic));

                        holder.usernameText.setText(other.getUsername());

                        boolean lastByMe = model.getLastMessageSenderId().equals(myId);
                        holder.lastMessageText.setText(lastByMe ? "You: " + model.getLastMessage()
                                : model.getLastMessage());

                        holder.lastMessageTime.setText(
                                FirebaseUtil.timestampToString(model.getLastMessageTimestamp())
                        );

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra("userId", otherIdFinal);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    }
                });
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(v);
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
