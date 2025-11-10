package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectEntity;

import java.util.List;

public class PublicProjectAdapter extends RecyclerView.Adapter<PublicProjectAdapter.ViewHolder> {

    private List<ProjectEntity> projects;
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onItemClick(ProjectEntity project); // ✅ chỉ còn 1 hàm
    }

    public PublicProjectAdapter(List<ProjectEntity> projects, OnProjectClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_project_name);
            tvDesc = itemView.findViewById(R.id.tv_project_desc);
        }
    }

    @NonNull
    @Override
    public PublicProjectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_public_project, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectEntity p = projects.get(position);

        holder.tvName.setText(p.projectName);
        holder.tvDesc.setText(
                (p.description == null || p.description.trim().isEmpty())
                        ? "(Không có mô tả)"
                        : p.description
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return projects != null ? projects.size() : 0;
    }
}
