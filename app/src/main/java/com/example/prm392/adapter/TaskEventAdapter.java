package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.CalendarEvent;

import java.util.ArrayList;
import java.util.List;

public class TaskEventAdapter extends RecyclerView.Adapter<TaskEventAdapter.VH> {

    private final List<CalendarEvent> list = new ArrayList<>();

    /**
     * Submit new data to the adapter. Clears old data and refreshes RecyclerView.
     */
    public void submit(List<CalendarEvent> data) {
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
    public void onBindViewHolder(@NonNull VH holder, int position) {
        CalendarEvent ev = list.get(position);
        holder.tvTaskId.setText("Task ID: " + ev.taskId);
        holder.tvEventDate.setText("Date: " + ev.eventDate);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTaskId, tvEventDate;

        public VH(@NonNull View itemView) {
            super(itemView);
            tvTaskId = itemView.findViewById(R.id.tvTaskId);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
        }
    }
}
