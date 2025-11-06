package com.example.prm392;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import models.Projects;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Projects project);
    }

    private List<Projects> projects;
    private OnItemClickListener listener;

    public ProjectAdapter(List<Projects> projects, OnItemClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Projects project = projects.get(position);
        holder.txtName.setText(project.getProjectName());
        holder.txtDesc.setText(project.getDescription());

        // 👇 Thêm phần này nếu chưa có
        holder.itemView.setOnClickListener(v -> listener.onItemClick(project));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDesc;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtProjectName);
            txtDesc = itemView.findViewById(R.id.txtProjectDesc);
        }
    }
}
