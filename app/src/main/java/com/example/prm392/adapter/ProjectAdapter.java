package com.example.prm392.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectEntity;

import java.util.List;
import java.util.function.Consumer;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    private final List<ProjectEntity> projects;
    private final String currentUserId;
    private final Consumer<ProjectEntity> onItemClick;
    private final Consumer<ProjectEntity> onEditClick;
    private final Consumer<ProjectEntity> onDeleteClick;
    private final Consumer<ProjectEntity> onJoinClick; // ✅ Thêm tham số thứ 6

    public ProjectAdapter(List<ProjectEntity> projects,
                          String currentUserId,
                          Consumer<ProjectEntity> onItemClick,
                          Consumer<ProjectEntity> onEditClick,
                          Consumer<ProjectEntity> onDeleteClick,
                          Consumer<ProjectEntity> onJoinClick) { // ✅ Constructor có 6 tham số
        this.projects = projects;
        this.currentUserId = currentUserId;
        this.onItemClick = onItemClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.onJoinClick = onJoinClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectEntity project = projects.get(position);

        holder.tvName.setText(project.projectName);
        holder.tvDescription.setText(project.description);

        // ✅ Hiển thị trạng thái Public / Private
        if (project.isPublic) {
            holder.tvVisibility.setText("Public");
            holder.tvVisibility.setTextColor(Color.parseColor("#2E7D32")); // xanh lá
        } else {
            holder.tvVisibility.setText("Private");
            holder.tvVisibility.setTextColor(Color.parseColor("#C62828")); // đỏ
        }

        boolean isOwner = currentUserId != null && currentUserId.equals(project.ownerId);

        // ✅ Nếu là chủ project -> hiện Edit/Delete
        if (isOwner) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnJoin.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);

            // ✅ Nếu project public và user không phải owner -> hiện Join
            if (project.isPublic) {
                holder.btnJoin.setVisibility(View.VISIBLE);
            } else {
                holder.btnJoin.setVisibility(View.GONE);
            }
        }

        // ✅ Gán sự kiện click
        holder.itemView.setOnClickListener(v -> onItemClick.accept(project));
        holder.btnEdit.setOnClickListener(v -> onEditClick.accept(project));
        holder.btnDelete.setOnClickListener(v -> onDeleteClick.accept(project));
        holder.btnJoin.setOnClickListener(v -> onJoinClick.accept(project));
    }

    @Override
    public int getItemCount() {
        return projects != null ? projects.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvVisibility;
        Button btnEdit, btnDelete, btnJoin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvDescription = itemView.findViewById(R.id.tvProjectDescription);
            tvVisibility = itemView.findViewById(R.id.tvProjectVisibility);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }
}
