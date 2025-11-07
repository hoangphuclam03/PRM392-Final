package com.example.prm392.adapter;

import android.content.ClipData;
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
import java.util.List;
import java.util.Map;

import models.Tasks;
import models.Users;

public class KanbanAdapter extends RecyclerView.Adapter<KanbanAdapter.ViewHolder> {

    private Context context;
    private List<Tasks> tasksList;
    private Map<Integer, List<Users>> taskAssigneesMap;
    private OnTaskClickListener clickListener;
    private OnTaskDragListener dragListener;

    private final String[] avatarColors = {
            "#1976D2", "#388E3C", "#D32F2F", "#7B1FA2",
            "#F57C00", "#0097A7", "#C2185B", "#5D4037"
    };

    public interface OnTaskClickListener {
        void onTaskClick(Tasks task);
    }

    public interface OnTaskDragListener {
        void onTaskDragStarted(Tasks task, int position);
    }

    public KanbanAdapter(Context context, OnTaskClickListener clickListener, OnTaskDragListener dragListener) {
        this.context = context;
        this.tasksList = new ArrayList<>();
        this.clickListener = clickListener;
        this.dragListener = dragListener;
    }

    public void setTasks(List<Tasks> tasks) {
        this.tasksList = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setTaskAssignees(Map<Integer, List<Users>> assignees) {
        this.taskAssigneesMap = assignees;
        notifyDataSetChanged();
    }

    public void removeTask(int position) {
        if (position >= 0 && position < tasksList.size()) {
            tasksList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addTask(Tasks task) {
        tasksList.add(task);
        notifyItemInserted(tasksList.size() - 1);
    }

    public void addTask(int position, Tasks task) {
        tasksList.add(position, task);
        notifyItemInserted(position);
    }

    public List<Tasks> getTasks() {
        return new ArrayList<>(tasksList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_kanban_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tasks task = tasksList.get(position);

        // Title
        holder.tvTaskTitle.setText(task.getTitle());

        // Due Date
        holder.tvDueDate.setText("📅 " + task.getDueDate());

        // Assignees
        displayAssignees(holder.layoutAssignees, task.getTaskId());

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });

        // Long click to start drag
        holder.itemView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("taskId", String.valueOf(task.getTaskId()));
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, task, 0);

            if (dragListener != null) {
                dragListener.onTaskDragStarted(task, position);
            }

            // Visual feedback
            v.setAlpha(0.5f);
            return true;
        });
    }

    private void displayAssignees(LinearLayout container, int taskId) {
        container.removeAllViews();

        if (taskAssigneesMap == null) {
            return;
        }

        List<Users> assignees = taskAssigneesMap.get(taskId);
        if (assignees == null || assignees.isEmpty()) {
            return;
        }

        // Hiển thị tối đa 3 avatars
        int maxDisplay = Math.min(3, assignees.size());
        for (int i = 0; i < maxDisplay; i++) {
            Users user = assignees.get(i);
            TextView avatar = createSmallAvatar(user, i);
            container.addView(avatar);
        }

        // Số lượng còn lại
        if (assignees.size() > 3) {
            TextView tvMore = new TextView(context);
            tvMore.setText("+" + (assignees.size() - 3));
            tvMore.setTextSize(10);
            tvMore.setTextColor(Color.parseColor("#666666"));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 0, 0, 0);
            tvMore.setLayoutParams(params);

            container.addView(tvMore);
        }
    }

    private TextView createSmallAvatar(Users user, int index) {
        TextView avatar = new TextView(context);

        String initial = user.getFirstName().substring(0, 1).toUpperCase();
        avatar.setText(initial);

        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(10);
        avatar.setGravity(android.view.Gravity.CENTER);

        int size = (int) (24 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        if (index > 0) {
            params.setMargins(-6, 0, 0, 0);
        }
        avatar.setLayoutParams(params);

        String color = avatarColors[index % avatarColors.length];
        avatar.setBackgroundResource(R.drawable.circle_avatar);
        avatar.getBackground().setTint(Color.parseColor(color));

        return avatar;
    }

    @Override
    public int getItemCount() {
        return tasksList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle;
        TextView tvDueDate;
        LinearLayout layoutAssignees;

        ViewHolder(View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tvKanbanTaskTitle);
            tvDueDate = itemView.findViewById(R.id.tvKanbanDueDate);
            layoutAssignees = itemView.findViewById(R.id.layoutKanbanAssignees);
        }
    }
}