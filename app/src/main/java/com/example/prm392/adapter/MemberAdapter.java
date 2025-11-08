package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.R;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<ProjectMemberEntity> memberList = new ArrayList<>();

    public MemberAdapter(List<ProjectMemberEntity> memberList) {
        if (memberList != null) {
            this.memberList = memberList;
        }
    }

    public void setMemberList(List<ProjectMemberEntity> members) {
        this.memberList = (members != null) ? members : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        ProjectMemberEntity member = memberList.get(position);
        if (member == null) return;

        // Display name or fallback
        holder.txtName.setText(
                (member.fullName != null && !member.fullName.isEmpty())
                        ? member.fullName
                        : "Không có tên"
        );

        // Display role
        holder.txtRole.setText(
                (member.role != null && !member.role.isEmpty())
                        ? member.role
                        : "Member"
        );

        // Optionally set a default avatar (initials or icon)
        holder.imgAvatar.setImageResource(R.drawable.ic_person); // fallback drawable
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtMemberName);
            txtRole = itemView.findViewById(R.id.txtMemberRole);
        }
    }
}
