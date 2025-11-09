package com.example.prm392.adapter;

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

public class PublicProjectAdapter extends RecyclerView.Adapter<PublicProjectAdapter.ViewHolder> {

    private List<ProjectEntity> projects;
    private OnProjectClickListener listener;

    // === Interface callback ===
    public interface OnProjectClickListener {
        void onItemClick(ProjectEntity project);

        void onRequestJoinClick(ProjectEntity project);
    }

    // === Constructor ===
    public PublicProjectAdapter(List<ProjectEntity> projects, OnProjectClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    // === ViewHolder ===
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvPublic;
        Button btnJoin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_project_name);
            tvDesc = itemView.findViewById(R.id.tv_project_desc);
            tvPublic = itemView.findViewById(R.id.tv_project_public);
            btnJoin = itemView.findViewById(R.id.btn_request_join);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectEntity project = projects.get(position);
        holder.tvName.setText(project.projectName);
        holder.tvDesc.setText(project.description != null ? project.description : "(Không có mô tả)");
        holder.tvPublic.setText(project.isPublic ? "Public" : "Private");

        // Click vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(project);
        });

        // Nút gửi yêu cầu tham gia
        if (holder.btnJoin != null) {
            holder.btnJoin.setOnClickListener(v -> {
                if (listener != null) listener.onRequestJoinClick(project);
            });
        }
    }

    @Override
    public int getItemCount() {
        return projects != null ? projects.size() : 0;
    }

    // === Cho phép cập nhật lại danh sách (nếu cần search) ===
    public void setData(List<ProjectEntity> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }
}
