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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PublicProjectAdapter extends RecyclerView.Adapter<PublicProjectAdapter.ViewHolder> {

    private List<ProjectEntity> projects;
    private OnProjectClickListener listener;

    // ✅ Danh sách projectId mà user đã tham gia (để ẩn nút Join)
    private Set<String> joinedProjectIds = new HashSet<>();

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

    // ✅ API để truyền danh sách project đã join từ Activity
    public void setJoinedProjects(Set<String> joinedIds) {
        this.joinedProjectIds = joinedIds != null ? joinedIds : new HashSet<>();
        notifyDataSetChanged();
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
                .inflate(R.layout.item_public_project, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectEntity project = projects.get(position);

        // Tên & mô tả
        holder.tvName.setText(project.projectName);
        holder.tvDesc.setText(project.description != null ? project.description : "(Không có mô tả)");
        holder.tvPublic.setText(project.isPublic ? "Public" : "Private");

        // ==============================
        // ✅ CLICK ITEM → mở chat
        // ==============================
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(project);
        });

        // ==============================
        // ✅ Xử lý nút Join
        // ==============================

        // 1) Nếu project không public → ẩn nút Join
        if (!project.isPublic) {
            holder.btnJoin.setVisibility(View.GONE);
            return;
        }

        // 2) Nếu user đã join → ẩn nút Join
        if (joinedProjectIds.contains(project.projectId)) {
            holder.btnJoin.setVisibility(View.GONE);
            return;
        }

        // 3) Còn lại → hiện nút Join
        holder.btnJoin.setVisibility(View.VISIBLE);
        holder.btnJoin.setOnClickListener(v -> {
            if (listener != null) listener.onRequestJoinClick(project);
        });
    }

    @Override
    public int getItemCount() {
        return projects != null ? projects.size() : 0;
    }

    // ✅ API cập nhật danh sách project (sau khi reload)
    public void setData(List<ProjectEntity> newProjects) {
        this.projects = newProjects;
        notifyDataSetChanged();
    }
}
