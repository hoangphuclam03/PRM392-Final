package com.example.prm392.data.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.prm392.data.repository.SyncRepository;
import com.example.prm392.models.CalendarEvent;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.models.NotificationEntity;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.ProjectMemberEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                CalendarEvent.class,
                ChatEntity.class,
                NotificationEntity.class,
                ProjectEntity.class,
                ProjectMemberEntity.class,
                TaskEntity.class,
                UserEntity.class
        },
        version = 3, // ✅ Reset version — new schema baseline
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // ======= DAO bindings =======
    public abstract ProjectDAO projectDAO();
    public abstract ProjectMemberDAO projectMemberDAO();
    public abstract TaskDAO taskDAO();
    public abstract UserDAO userDAO();
    public abstract ChatDAO chatDAO();
    public abstract NotificationDAO notificationDAO();

    // ======= INSTANCE =======
    private static volatile AppDatabase instance;

    // ======= THREAD EXECUTOR =======
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // ======= SINGLETON BUILDER =======
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static AppDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                        context,
                        AppDatabase.class,
                        "team_manager_db"
                )
                // 🔹 Always drop & recreate database if schema mismatches
                .fallbackToDestructiveMigration()
                // 🔹 Async background operations
                .setQueryExecutor(databaseWriteExecutor)
                .setTransactionExecutor(databaseWriteExecutor)
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        databaseWriteExecutor.execute(() -> {
                            try {
                                Log.d("AppDatabase", "🆕 Room created — syncing from Firestore...");
                                new com.example.prm392.data.repository.SyncRepository(context).syncAll();
                            } catch (Exception e) {
                                Log.e("AppDatabase", "❌ Auto Firestore sync failed: " + e.getMessage(), e);
                            }
                        });
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // Optional lightweight check or refresh
                    }
                })
                .build();
    }
}
