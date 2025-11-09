package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectMemberEntity;

import java.util.ArrayList;
import java.util.List;

public class MemberSelectAdapter extends RecyclerView.Adapter<MemberSelectAdapter.ViewHolder> {

    private List<ProjectMemberEntity> members;
    private int selectedPosition = -1;
    private OnMemberSelectedListener listener;

    public List<String> getSelectedUserIds() {
        return new ArrayList<>();
    }


    public interface OnMemberSelectedListener {
        void onMemberSelected(ProjectMemberEntity member);
    }

    public MemberSelectAdapter(List<ProjectMemberEntity> members, OnMemberSelectedListener listener) {
        this.members = members != null ? members : new ArrayList<>();
        this.listener = listener;
    }

    public void submitList(List<ProjectMemberEntity> newMembers) {
        this.members = newMembers;
        notifyDataSetChanged();
    }

    public ProjectMemberEntity getSelectedMember() {
        if (selectedPosition >= 0 && selectedPosition < members.size()) {
            return members.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProjectMemberEntity member = members.get(position);
        holder.tvMemberName.setText(member.fullName);
        holder.rbSelectMember.setChecked(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
            if (listener != null) listener.onMemberSelected(member);
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton rbSelectMember;
        TextView tvMemberName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rbSelectMember = itemView.findViewById(R.id.rbSelectMember);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
        }
    }
}
