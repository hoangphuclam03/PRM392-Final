package com.example.prm392;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import models.ProjectMembers;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<ProjectMembers> memberList;

    public MemberAdapter(List<ProjectMembers> memberList) {
        this.memberList = memberList;
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
        ProjectMembers member = memberList.get(position);
        holder.txtName.setText(member.getName());
        holder.txtRole.setText(member.getRole());
    }

    @Override
    public int getItemCount() {
        return memberList.size();
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
