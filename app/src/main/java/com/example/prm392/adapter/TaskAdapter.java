package com.example.prm392.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private Context context;
    private List<TaskEntity> tasksList;
    private List<TaskEntity> tasksListFiltered;
    private Map<String, ProjectEntity> projectsMap;       // key = projectId
    private Map<String, UserEntity> usersMap;             // key = userId
    private OnTaskClickListener listener;

    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public interface OnTaskClickListener {
        void onTaskClick(TaskEntity task);

        void onTaskLongClick(TaskEntity task);
    }

    public TaskAdapter(Context context, OnTaskClickListener listener) {
        this.context = context;
        this.tasksList = new ArrayList<>();
        this.tasksListFiltered = new ArrayList<>();
        this.projectsMap = new HashMap<>();
        this.usersMap = new HashMap<>();
        this.listener = listener;
    }

    public void setTasks(List<TaskEntity> tasks) {
        this.tasksList = tasks;
        this.tasksListFiltered = new ArrayList<>(tasks);
        notifyDataSetChanged();
    }

    public void setProjects(List<ProjectEntity> projects) {
        this.projectsMap.clear();
        for (ProjectEntity project : projects) {
            this.projectsMap.put(project.projectId, project);
        }
        notifyDataSetChanged();
    }

    public void setUsers(List<UserEntity> users) {
        this.usersMap.clear();
        for (UserEntity user : users) {
            this.usersMap.put(user.userId, user);
        }
        notifyDataSetChanged();
    }

    public void filter(String status) {
        tasksListFiltered.clear();
        if (status == null || status.equalsIgnoreCase("ALL")) {
            tasksListFiltered.addAll(tasksList);
        } else {
            for (TaskEntity task : tasksList) {
                if (task.status != null && task.status.equalsIgnoreCase(status)) {
                    tasksListFiltered.add(task);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TaskEntity task = tasksListFiltered.get(position);

        // Title & Description
        holder.tvTaskTitle.setText(task.title);
        holder.tvTaskDescription.setText(
                task.description != null && !task.description.isEmpty()
                        ? task.description
                        : "Không có mô tả"
        );

        // Due date
        holder.tvDueDate.setText("📅 " + task.dueDate);

        // Status badge
        setStatusBadge(holder.tvStatus, task.status);

        // Project name
        ProjectEntity project = projectsMap.get(task.projectId);
        holder.tvProjectName.setText(project != null ? project.projectName : "Unknown Project");

        // Assignee
        displayAssignee(holder.layoutAssignees, task.assignedTo);

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(task);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onTaskLongClick(task);
            return true;
        });
    }

    private void setStatusBadge(TextView tvStatus, String status) {
        if (status == null) status = "TODO";

        switch (status.toUpperCase()) {
            case "TODO":
                tvStatus.setText("📋 TO DO");
                tvStatus.setBackgroundResource(R.drawable.badge_todo);
                break;
            case "INPROGRESS":
                tvStatus.setText("⚡ IN PROGRESS");
                tvStatus.setBackgroundResource(R.drawable.badge_in_progress);
                break;
            case "INREVIEW":
                tvStatus.setText("👀 IN REVIEW");
                tvStatus.setBackgroundResource(R.drawable.badge_in_review);
                break;
            case "DONE":
                tvStatus.setText("✅ DONE");
                tvStatus.setBackgroundResource(R.drawable.badge_done);
                break;
            default:
                tvStatus.setText(status);
                tvStatus.setBackgroundResource(R.drawable.badge_todo);
        }
    }

    private void displayAssignee(LinearLayout container, String userId) {
        container.removeAllViews();

        if (userId == null || !usersMap.containsKey(userId)) {
            TextView tvNoAssignee = new TextView(context);
            tvNoAssignee.setText("Chưa giao");
            tvNoAssignee.setTextSize(12);
            tvNoAssignee.setTextColor(Color.parseColor("#999999"));
            container.addView(tvNoAssignee);
            return;
        }

        UserEntity user = usersMap.get(userId);
        TextView avatar = createAvatarView(user, 0);
        container.addView(avatar);
    }

    private TextView createAvatarView(UserEntity user, int index) {
        TextView avatar = new TextView(context);

        // First letter
        String initial = user.fullName != null && !user.fullName.isEmpty()
                ? user.fullName.substring(0, 1).toUpperCase()
                : "?";
        avatar.setText(initial);

        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(12);
        avatar.setGravity(Gravity.CENTER);

        // Size
        int size = (int) (28 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        avatar.setLayoutParams(params);

        // Background
        avatar.setBackgroundResource(R.drawable.circle_avatar);
        String color = avatarColors[index % avatarColors.length];
        avatar.getBackground().setTint(Color.parseColor(color));

        return avatar;
    }

    @Override
    public int getItemCount() {
        return tasksListFiltered.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus;
        TextView tvDueDate;
        TextView tvTaskTitle;
        TextView tvTaskDescription;
        TextView tvProjectName;
        LinearLayout layoutAssignees;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            layoutAssignees = itemView.findViewById(R.id.layoutAssignees);
        }
    }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvAssignedTo, tvStatus, tvDueDate;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvStatus     = itemView.findViewById(R.id.tvStatus);
            tvDueDate    = itemView.findViewById(R.id.tvDueDate);
        }

        void bind(TaskEntity t) {
            tvAssignedTo.setText("Assigned to: " + (t.assignedTo == null ? "-" : t.assignedTo));
            tvStatus.setText("Status: " + (t.status == null ? "-" : t.status));
            tvDueDate.setText("Due: " + (t.dueDate == null ? "-" : t.dueDate));
        }
    }

}
