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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Projects;
import models.Tasks;
import models.Users;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private Context context;
    private List<Tasks> tasksList;
    private List<Tasks> tasksListFiltered;
    private Map<Integer, Projects> projectsMap;
    private Map<Integer, List<Users>> taskAssigneesMap; // taskId -> List<Users>
    private OnTaskClickListener listener;

    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public interface OnTaskClickListener {
        void onTaskClick(Tasks task);
        void onTaskLongClick(Tasks task); // For quick actions
    }

    public TaskAdapter(Context context, OnTaskClickListener listener) {
        this.context = context;
        this.tasksList = new ArrayList<>();
        this.tasksListFiltered = new ArrayList<>();
        this.projectsMap = new HashMap<>();
        this.taskAssigneesMap = new HashMap<>();
        this.listener = listener;
    }

    public void setTasks(List<Tasks> tasks) {
        this.tasksList = tasks;
        this.tasksListFiltered = new ArrayList<>(tasks);
        notifyDataSetChanged();
    }

    public void setProjects(Map<Integer, Projects> projects) {
        this.projectsMap = projects;
        notifyDataSetChanged();
    }

    public void setTaskAssignees(Map<Integer, List<Users>> assignees) {
        this.taskAssigneesMap = assignees;
        notifyDataSetChanged();
    }

    public void filter(String status) {
        tasksListFiltered.clear();
        if (status == null || status.equals("ALL")) {
            tasksListFiltered.addAll(tasksList);
        } else {
            for (Tasks task : tasksList) {
                if (task.getStatus().equals(status)) {
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
        Tasks task = tasksListFiltered.get(position);

        // Title & Description
        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskDescription.setText(
                task.getDescription() != null && !task.getDescription().isEmpty()
                        ? task.getDescription()
                        : "Không có mô tả"
        );

        // Due Date
        holder.tvDueDate.setText("📅 " + task.getDueDate());

        // Status
        setStatusBadge(holder.tvStatus, task.getStatus());

        // Project Name
        Projects project = projectsMap.get(task.getProjectId());
        if (project != null) {
            holder.tvProjectName.setText(project.getProjectName());
        } else {
            holder.tvProjectName.setText("Unknown Project");
        }

        // Assignees
        displayAssignees(holder.layoutAssignees, task.getTaskId());

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    private void setStatusBadge(TextView tvStatus, String status) {
        switch (status) {
            case "TODO":
                tvStatus.setText("📋 TO DO");
                tvStatus.setBackgroundResource(R.drawable.badge_todo);
                break;
            case "IN_PROGRESS":
                tvStatus.setText("⚡ IN PROGRESS");
                tvStatus.setBackgroundResource(R.drawable.badge_in_progress);
                break;
            case "IN_REVIEW":
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

    private void displayAssignees(LinearLayout container, int taskId) {
        container.removeAllViews();

        List<Users> assignees = taskAssigneesMap.get(taskId);
        if (assignees == null || assignees.isEmpty()) {
            TextView tvNoAssignee = new TextView(context);
            tvNoAssignee.setText("Chưa giao");
            tvNoAssignee.setTextSize(12);
            tvNoAssignee.setTextColor(Color.parseColor("#999999"));
            container.addView(tvNoAssignee);
            return;
        }

        // Hiển thị tối đa 3 avatars
        int maxDisplay = Math.min(3, assignees.size());
        for (int i = 0; i < maxDisplay; i++) {
            Users user = assignees.get(i);
            TextView avatar = createAvatarView(user, i);
            container.addView(avatar);
        }

        // Hiển thị số lượng còn lại
        if (assignees.size() > 3) {
            TextView tvMore = new TextView(context);
            tvMore.setText("+" + (assignees.size() - 3));
            tvMore.setTextSize(12);
            tvMore.setTextColor(Color.parseColor("#666666"));
            tvMore.setBackgroundColor(Color.parseColor("#E0E0E0"));
            tvMore.setPadding(12, 4, 12, 4);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 0, 0, 0);
            tvMore.setLayoutParams(params);

            container.addView(tvMore);
        }
    }

    private TextView createAvatarView(Users user, int index) {
        TextView avatar = new TextView(context);

        // Lấy chữ cái đầu
        String initial = user.getFirstName().substring(0, 1).toUpperCase();
        avatar.setText(initial);

        // Style
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(12);
        avatar.setGravity(android.view.Gravity.CENTER);

        // Size
        int size = (int) (28 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        if (index > 0) {
            params.setMargins(-8, 0, 0, 0); // Overlap avatars
        }
        avatar.setLayoutParams(params);

        // Background color
        String color = avatarColors[index % avatarColors.length];
        avatar.setBackgroundResource(R.drawable.circle_avatar);
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

        ViewHolder(View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            layoutAssignees = itemView.findViewById(R.id.layoutAssignees);
        }
    }
}