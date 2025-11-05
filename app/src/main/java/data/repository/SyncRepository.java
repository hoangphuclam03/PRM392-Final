package data.repository;

import android.content.Context;
import android.util.Log;

import data.local.DBConnect;
import data.remote.FirebaseUtils;
import models.Projects;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class SyncRepository {
    private final DBConnect localDb;
    private static final String TAG = "SyncRepo";
    public SyncRepository(Context context) {
        this.localDb = new DBConnect(context);
    }

    // 🔽 Download Firebase → SQLite
    public void syncProjectsFromFirebase() {
        FirebaseUtils.getProjectsCollection()
                .get()
                .addOnSuccessListener(query -> {
                    Log.d(TAG, "Downloading " + query.size() + " projects from Firebase");

                    for (var doc : query.getDocuments()) {
                        Projects project = doc.toObject(Projects.class);

                        if (project.getProjectName() == null) project.setProjectName("Unnamed Project");
                        if (project.getDescription() == null) project.setDescription("");
                        if (project.getCreatedAt() == null) project.setCreatedAt("");

                        localDb.insertOrUpdateProject(project);
                        Log.d(TAG, "Downloaded project: " + project.getProjectName() +
                                " (ID: " + project.getProjectId() + ")");
                    }

                    Log.d(TAG, "Projects synced from Firebase successfully");
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to download projects: " + e.getMessage()));
    }


    // 🔼 Upload SQLite → Firebase
    public void syncProjectsToFirebase() {
        for (Projects project : localDb.getAllProjects()) {
            Map<String, Object> map = new HashMap<>();
            map.put("project_name", project.getProjectName());
            map.put("description", project.getDescription());
            map.put("created_by", project.getCreatedBy());
            map.put("created_at", project.getCreatedAt());

            FirebaseUtils.getProjectsCollection()
                    .document(String.valueOf(project.getProjectId()))
                    .set(map)
                    .addOnFailureListener(e ->
                            Log.e("SyncRepo", "Failed to upload project: " + e.getMessage())
                    );
        }
    }
}
