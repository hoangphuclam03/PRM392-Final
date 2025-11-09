package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.TaskEntity;

import java.util.ArrayList;
import java.util.List;

public class CalendarTaskAdapter extends RecyclerView.Adapter<CalendarTaskAdapter.VH> {

    private final List<TaskEntity> data = new ArrayList<>();

    public void submit(List<TaskEntity> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_event_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAssignedTo, tvStatus, tvDueDate;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvStatus     = itemView.findViewById(R.id.tvStatus);
            tvDueDate    = itemView.findViewById(R.id.tvDueDate);
        }

        void bind(TaskEntity t) {
            tvAssignedTo.setText("Assigned to: " + (t.assignedTo == null ? "-" : t.assignedTo));
            tvStatus.setText("Status: " + (t.status == null ? "-" : t.status));
            tvDueDate.setText("Due: " + (t.dueDate == null ? "-" : t.dueDate));
        }
    }
}
