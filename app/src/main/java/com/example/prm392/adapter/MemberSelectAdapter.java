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
import models.Users;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.ViewHolder> {

    private List<Users> members;
    private List<Integer> selectedUserIds;
    private OnSelectionChangedListener listener;

    // Màu avatar ngẫu nhiên
    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public MemberSelectAdapter(List<Users> members, OnSelectionChangedListener listener) {
        this.members = members;
        this.selectedUserIds = new ArrayList<>();
        this.listener = listener;
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
        Users user = members.get(position);

        // Hiển thị tên đầy đủ
        String fullName = user.getFirstName() + " " + user.getLastName();
        holder.tvMemberName.setText(fullName);
        holder.tvMemberEmail.setText(user.getEmail());

        // Avatar với chữ cái đầu
        String initial = user.getFirstName().substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);

        // Màu ngẫu nhiên cho avatar
        String color = avatarColors[position % avatarColors.length];
        holder.tvAvatar.setBackgroundResource(R.drawable.circle_avatar);
        holder.tvAvatar.getBackground().setTint(Color.parseColor(color));

        // Set trạng thái checkbox
        holder.cbMember.setChecked(selectedUserIds.contains(user.getId()));

        // Xử lý click
        holder.itemView.setOnClickListener(v -> {
            holder.cbMember.setChecked(!holder.cbMember.isChecked());
            handleSelection(user.getId(), holder.cbMember.isChecked());
        });

        holder.cbMember.setOnCheckedChangeListener((buttonView, isChecked) -> {
            handleSelection(user.getId(), isChecked);
        });
    }

    private void handleSelection(int userId, boolean isSelected) {
        if (isSelected) {
            if (!selectedUserIds.contains(userId)) {
                selectedUserIds.add(userId);
            }
        } else {
            selectedUserIds.remove(Integer.valueOf(userId));
        }

        if (listener != null) {
            listener.onSelectionChanged(selectedUserIds.size());
        }
    }

    public List<Integer> getSelectedUserIds() {
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