package com.example.prm392.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.ViewHolder> {

    private final List<UserEntity> members;
    private final List<String> selectedUserIds;
    private final OnSelectionChangedListener listener;

    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public MemberSelectAdapter(List<UserEntity> members, OnSelectionChangedListener listener) {
        this.members = members != null ? members : new ArrayList<>();
        this.selectedUserIds = new ArrayList<>();
        this.listener = listener;
    }

    public void updateMembers(List<UserEntity> newMembers) {
        members.clear();
        if (newMembers != null) {
            members.addAll(newMembers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserEntity user = members.get(position);
        if (user == null) return;

        // Display name
        holder.tvMemberName.setText(
                user.fullName != null && !user.fullName.isEmpty()
                        ? user.fullName
                        : "Unnamed User"
        );

        holder.tvMemberEmail.setText(
                user.email != null && !user.email.isEmpty()
                        ? user.email
                        : "No email"
        );

        // Avatar letter
        String initial = user.fullName != null && !user.fullName.isEmpty()
                ? user.fullName.substring(0, 1).toUpperCase(Locale.getDefault())
                : "?";
        holder.tvAvatar.setText(initial);

        // Consistent color
        int colorIndex = Math.abs((user.userId != null ? user.userId.hashCode() : position)) % avatarColors.length;
        holder.tvAvatar.setBackgroundResource(R.drawable.circle_avatar);
        holder.tvAvatar.getBackground().setTint(Color.parseColor(avatarColors[colorIndex]));

        // Checkbox logic
        boolean isSelected = selectedUserIds.contains(user.userId);
        holder.cbMember.setChecked(isSelected);

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.cbMember.isChecked();
            holder.cbMember.setChecked(newState);
            handleSelection(user.userId, newState);
        });

        holder.cbMember.setOnCheckedChangeListener((buttonView, isChecked) ->
                handleSelection(user.userId, isChecked)
        );
    }

    private void handleSelection(String userId, boolean isSelected) {
        if (userId == null) return;
        if (isSelected) {
            if (!selectedUserIds.contains(userId)) selectedUserIds.add(userId);
        } else {
            selectedUserIds.remove(userId);
        }
        if (listener != null) listener.onSelectionChanged(selectedUserIds.size());
    }

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbMember;
        TextView tvAvatar;
        TextView tvMemberName;
        TextView tvMemberEmail;

        ViewHolder(View itemView) {
            super(itemView);
            cbMember = itemView.findViewById(R.id.cbMember);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberEmail = itemView.findViewById(R.id.tvMemberEmail);
        }
    }
}
