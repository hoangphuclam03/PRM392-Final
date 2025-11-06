package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;

import java.util.ArrayList;
import java.util.List;

import models.CalendarEvents;

public class TaskEventAdapter extends RecyclerView.Adapter<TaskEventAdapter.VH> {

    private final List<CalendarEvents> list = new ArrayList<>();

    public void submit(List<CalendarEvents> data) {
        list.clear();
        if (data != null) list.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_event_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        CalendarEvents ev = list.get(pos);
        holder.tvTaskId.setText("Task ID: " + ev.getTaskId());
        holder.tvEventDate.setText("Date: " + ev.getEventDate());
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTaskId, tvEventDate;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvTaskId = itemView.findViewById(R.id.tvTaskId);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
        }
    }
}
