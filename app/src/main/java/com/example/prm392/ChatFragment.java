package com.example.prm392;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.prm392.adapter.ChatRecyclerAdapter;
import com.example.prm392.data.local.AppDatabase;
import com.example.prm392.data.local.ChatDAO;
import com.example.prm392.models.ChatEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatRecyclerAdapter adapter;
    private ChatDAO chatDAO;

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize DAO
        chatDAO = AppDatabase.getInstance(getContext()).chatDAO();

        // Initialize adapter (show sender names)
        adapter = new ChatRecyclerAdapter(getContext(), true);
        recyclerView.setAdapter(adapter);

        loadChats();

        return view;
    }

    private void loadChats() {
        // Run DB query on background thread
        ExecutorService executor = AppDatabase.databaseWriteExecutor;
        executor.execute(() -> {
            // Assuming you want all chats ordered by timestamp ASC
            List<ChatEntity> chats = chatDAO.getByProject("team_project_id"); // replace with your actual projectId

            // Update RecyclerView on UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> adapter.setChats(chats));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // reload chats in case new ones were added locally
        loadChats();
    }
}
