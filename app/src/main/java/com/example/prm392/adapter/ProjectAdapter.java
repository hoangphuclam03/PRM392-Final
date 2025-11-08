package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.models.ProjectEntity;
import com.example.prm392.R;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(ProjectEntity project);
    }

    private List<ProjectEntity> projectList;
    private OnProjectClickListener listener;

    public ProjectAdapter(List<ProjectEntity> projectList, OnProjectClickListener listener) {
        this.projectList = (projectList != null) ? projectList : new ArrayList<>();
        this.listener = listener;
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
        ProjectEntity project = projectList.get(position);
        holder.txtProjectName.setText(project.projectName != null ? project.projectName : "(No name)");
        holder.txtDescription.setText(project.description != null ? project.description : "(No description)");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProjectClick(project);
        });
    }

    @Override
    public int getItemCount() {
        return projectList != null ? projectList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProjectName, txtDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProjectName = itemView.findViewById(R.id.txtProjectName);
            txtDescription = itemView.findViewById(R.id.txtProjectDesc);
        }
    }
}
