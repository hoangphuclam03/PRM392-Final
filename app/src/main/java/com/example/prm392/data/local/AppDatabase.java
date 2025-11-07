package com.example.prm392.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.prm392.models.UserEntity;
import com.example.prm392.models.ProjectEntity;
import com.example.prm392.models.TaskEntity;
import com.example.prm392.models.ChatEntity;
import com.example.prm392.models.NotificationEntity;

@Database(
        entities = {
                UserEntity.class,
                ProjectEntity.class,
                TaskEntity.class,
                ChatEntity.class,
                NotificationEntity.class
        },
        version = 2, // ✅ bump when schema changes
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    // DAOs
    public abstract UserDAO userDAO();

    public abstract ProjectDAO projectDAO();

    public abstract TaskDAO taskDAO();

    public abstract ChatDAO chatDAO();

    public abstract NotificationDAO notificationDAO();

    // ✅ Example migration from version 1 → 2
    //private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    // @Override
    // public void migrate(@NonNull SupportSQLiteDatabase db) {
    // Example: add a new column 'role' to users table
    //   db.execSQL("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'member' NOT NULL");
    // }
    // };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "team_management_db"
                            )
                            // ✅ Register migrations here
                            //.addMigrations(MIGRATION_1_2)

                            // Optional: fallback only on downgrade
                            .fallbackToDestructiveMigrationOnDowngrade()

                            // Optional: pre-populate data or logs
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
