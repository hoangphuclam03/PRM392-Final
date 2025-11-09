package com.example.prm392.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
        version = 9, // ⬅ incremented version to fix schema mismatch
        exportSchema = true
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
                // Keep migrations for production use
                .addMigrations(
                        MigrationManager.MIGRATION_1_2,
                        MigrationManager.MIGRATION_2_3,
                        MigrationManager.MIGRATION_3_4
                )
                // Developer safe mode — rebuilds DB on mismatch
                .fallbackToDestructiveMigration()
                // Room will open DB asynchronously to avoid main-thread blocking
                .setQueryExecutor(databaseWriteExecutor)
                .setTransactionExecutor(databaseWriteExecutor)
                .addCallback(new Callback() {
                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // Optional: run lightweight checks/logs
                        databaseWriteExecutor.execute(() -> {
                            // Example: preload small lookup data if needed
                        });
                    }
                })
                .build();
    }
}
