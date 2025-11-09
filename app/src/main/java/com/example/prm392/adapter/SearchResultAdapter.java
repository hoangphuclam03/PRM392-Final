package com.example.prm392.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prm392.R;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.SearchResultItem;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SearchResultItem> items = new ArrayList<>();

    public void setItems(List<SearchResultItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SearchResultItem.TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_search_result, parent, false);
            return new ResultViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchResultItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(item.getTitle());
        } else if (holder instanceof ResultViewHolder) {
            Object data = item.getData();
            String text = "";
            if (data instanceof ProjectEntity) {
                text = ((ProjectEntity) data).projectName;
            } else if (data instanceof TaskEntity) {
                text = ((TaskEntity) data).title;
            } else if (data instanceof UserEntity) {
                text = ((UserEntity) data).fullName;
            }
            ((ResultViewHolder) holder).title.setText(text);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textHeader);
        }
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textItemTitle);
        }
    }
}
