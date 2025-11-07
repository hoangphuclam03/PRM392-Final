package com.example.prm392.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;

import java.util.ArrayList;
import java.util.List;

import models.Users;

public class AssigneeDisplayAdapter extends RecyclerView.Adapter<AssigneeDisplayAdapter.ViewHolder> {

    private Context context;
    private List<Users> assignees;

    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public AssigneeDisplayAdapter(Context context) {
        this.context = context;
        this.assignees = new ArrayList<>();
    }

    public void setAssignees(List<Users> assignees) {
        this.assignees = assignees != null ? assignees : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_assignee_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users user = assignees.get(position);

        // Full name
        String fullName = user.getFirstName() + " " + user.getLastName();
        holder.tvAssigneeName.setText(fullName);
        holder.tvAssigneeEmail.setText(user.getEmail());

        // Avatar
        String initial = user.getFirstName().substring(0, 1).toUpperCase();
        holder.tvAssigneeAvatar.setText(initial);

        // Avatar color
        String color = avatarColors[position % avatarColors.length];
        holder.tvAssigneeAvatar.setBackgroundResource(R.drawable.circle_avatar);
        holder.tvAssigneeAvatar.getBackground().setTint(Color.parseColor(color));
    }

    @Override
    public int getItemCount() {
        return assignees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAssigneeAvatar;
        TextView tvAssigneeName;
        TextView tvAssigneeEmail;

        ViewHolder(View itemView) {
            super(itemView);
            tvAssigneeAvatar = itemView.findViewById(R.id.tvAssigneeAvatar);
            tvAssigneeName = itemView.findViewById(R.id.tvAssigneeName);
            tvAssigneeEmail = itemView.findViewById(R.id.tvAssigneeEmail);
        }
    }
}