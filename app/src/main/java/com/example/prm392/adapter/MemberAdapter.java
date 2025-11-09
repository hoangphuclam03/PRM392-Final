package com.example.prm392.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectMemberEntity;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<ProjectMemberEntity> memberList = new ArrayList<>();
    private boolean isManager = false;
    private OnMemberDeleteListener onDeleteListener;
    private Context context;
    private boolean isDarkMode = false; // ✅ để xác định theme hiện tại

    // --- Constructor mặc định (cũ, để tương thích với code cũ) ---
    public MemberAdapter(List<ProjectMemberEntity> memberList) {
        if (memberList != null) this.memberList = memberList;
    }

    // --- ✅ Constructor mới có thêm isDarkMode để fix lỗi Expected 1 arg ---
    public MemberAdapter(List<ProjectMemberEntity> memberList, boolean isDarkMode) {
        if (memberList != null) this.memberList = memberList;
        this.isDarkMode = isDarkMode;
    }

    public void setMemberList(List<ProjectMemberEntity> members) {
        this.memberList = (members != null) ? members : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setManager(boolean manager) {
        this.isManager = manager;
    }

    public void setOnMemberDeleteListener(OnMemberDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_project_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        ProjectMemberEntity member = memberList.get(position);
        if (member == null) return;

        holder.tvName.setText(
                (member.fullName != null && !member.fullName.isEmpty())
                        ? member.fullName
                        : "(No Name)"
        );

        holder.tvRole.setText(
                (member.role != null && !member.role.isEmpty())
                        ? member.role
                        : "Member"
        );

        // ✅ Đổi icon theo theme hiện tại
        if (isDarkMode) {
            holder.imgAvatar.setImageResource(R.drawable.ic_person_light);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_person_dark);
        }

        // 🔹 Hiện nút xoá nếu là Manager và không phải chính Manager
        if (isManager && !"Manager".equalsIgnoreCase(member.role)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Xóa thành viên")
                        .setMessage("Bạn có chắc muốn xóa " + member.fullName + " khỏi dự án này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            if (onDeleteListener != null)
                                onDeleteListener.onDelete(member);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnDelete;
        TextView tvName, tvRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvMemberName);
            tvRole = itemView.findViewById(R.id.tvMemberRole);
            btnDelete = itemView.findViewById(R.id.btnDeleteMember);
        }
    }

    public interface OnMemberDeleteListener {
        void onDelete(ProjectMemberEntity member);
    }
}
