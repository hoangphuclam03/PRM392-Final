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
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class AssigneeDisplayAdapter extends RecyclerView.Adapter<AssigneeDisplayAdapter.ViewHolder> {

    private final Context context;
    private List<UserEntity> assignees;

    // Predefined avatar color palette
    private static final String[] AVATAR_COLORS = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public AssigneeDisplayAdapter(Context context) {
        this.context = context;
        this.assignees = new ArrayList<>();
    }

    public void setAssignees(List<UserEntity> assignees) {
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
        UserEntity user = assignees.get(position);

        // Full name (fallback if null)
        String fullName = (user.fullName != null && !user.fullName.isEmpty())
                ? user.fullName : "Unknown User";
        holder.tvAssigneeName.setText(fullName);

        // Email display
        holder.tvAssigneeEmail.setText(
                (user.email != null && !user.email.isEmpty()) ? user.email : "(no email)"
        );

        // Avatar letter
        String initial = fullName.substring(0, 1).toUpperCase();
        holder.tvAssigneeAvatar.setText(initial);

        // Avatar color (cycled)
        String color = AVATAR_COLORS[position % AVATAR_COLORS.length];
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
