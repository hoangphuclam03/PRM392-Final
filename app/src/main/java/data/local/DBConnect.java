package data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import models.Users;
import models.Projects;

/**
 * Local SQLite database for offline caching + sync with Firebase.
 */
public class DBConnect extends SQLiteOpenHelper {

    // Database info
    private static final String dbName = "PRM392.db";
    private static final int dbVersion = 2;

    // USERS table
    private static final String USERS_TABLE = "users";
    private static final String COL_ID = "id";
    private static final String COL_FIRST_NAME = "firstName";
    private static final String COL_LAST_NAME = "lastName";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    public DBConnect(@Nullable Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // USERS
        String usersQuery = "CREATE TABLE " + USERS_TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FIRST_NAME + " TEXT NOT NULL, " +
                COL_LAST_NAME + " TEXT NOT NULL, " +
                COL_EMAIL + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL" +
                ");";
        db.execSQL(usersQuery);

        // PROJECTS
        db.execSQL("CREATE TABLE projects (" +
                "project_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_name TEXT NOT NULL, " +
                "description TEXT, " +
                "created_by INTEGER, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (created_by) REFERENCES users(id)" +
                ");");

        // PROJECT_MEMBERS
        db.execSQL("CREATE TABLE project_members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_id INTEGER, " +
                "user_id INTEGER, " +
                "role TEXT DEFAULT 'member', " +
                "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // TASKS
        db.execSQL("CREATE TABLE tasks (" +
                "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_id INTEGER, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "due_date TEXT, " +
                "status TEXT DEFAULT 'To Do', " +
                "created_by INTEGER, " +
                "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                "FOREIGN KEY (created_by) REFERENCES users(id)" +
                ");");

        // TASK_ASSIGNEES
        db.execSQL("CREATE TABLE task_assignees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER, " +
                "user_id INTEGER, " +
                "FOREIGN KEY (task_id) REFERENCES tasks(task_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // MESSAGES
        db.execSQL("CREATE TABLE messages (" +
                "message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_id INTEGER, " +
                "sender_id INTEGER, " +
                "content TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id)" +
                ");");

        // NOTIFICATIONS
        db.execSQL("CREATE TABLE notifications (" +
                "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "content TEXT, " +
                "is_read INTEGER DEFAULT 0, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // CALENDAR_EVENTS
        db.execSQL("CREATE TABLE calendar_events (" +
                "event_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER, " +
                "event_date TEXT, " +
                "FOREIGN KEY (task_id) REFERENCES tasks(task_id)" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS projects (" +
                    "project_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "created_by INTEGER, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (created_by) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS project_members (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_id INTEGER, " +
                    "user_id INTEGER, " +
                    "role TEXT DEFAULT 'member', " +
                    "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS tasks (" +
                    "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_id INTEGER, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "due_date TEXT, " +
                    "status TEXT DEFAULT 'To Do', " +
                    "created_by INTEGER, " +
                    "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                    "FOREIGN KEY (created_by) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS task_assignees (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER, " +
                    "user_id INTEGER, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(task_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS messages (" +
                    "message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "project_id INTEGER, " +
                    "sender_id INTEGER, " +
                    "content TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                    "FOREIGN KEY (sender_id) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS notifications (" +
                    "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "content TEXT, " +
                    "is_read INTEGER DEFAULT 0, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ");");

            db.execSQL("CREATE TABLE IF NOT EXISTS calendar_events (" +
                    "event_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER, " +
                    "event_date TEXT, " +
                    "FOREIGN KEY (task_id) REFERENCES tasks(task_id)" +
                    ");");
        }
    }

    // ⚙️ Local add user
    public void addUser(Users user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIRST_NAME, user.getFirstName());
        values.put(COL_LAST_NAME, user.getLastName());
        values.put(COL_EMAIL, user.getEmail());
        values.put(COL_PASSWORD, user.getPassword());
        db.insert(USERS_TABLE, null, values);
    }

    // 🔄 Sync helpers (for Firebase integration)
    public void insertOrUpdateProject(Projects project) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Fill defaults if null
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Unnamed Project";
        String description = project.getDescription() != null ? project.getDescription() : "";
        String createdAt = project.getCreatedAt() != null ? project.getCreatedAt() : "";
        int createdBy = project.getCreatedBy(); // optional: 0 if you want a default

        values.put("project_id", project.getProjectId());
        values.put("project_name", projectName);
        values.put("description", description);
        values.put("created_by", createdBy);
        values.put("created_at", createdAt);

        db.insertWithOnConflict("projects", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }



    public List<Projects> getAllProjects() {
        List<Projects> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM projects", null);
        if (cursor.moveToFirst()) {
            do {
                Projects project = new Projects();
                project.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow("project_id")));
                project.setProjectName(cursor.getString(cursor.getColumnIndexOrThrow("project_name")));
                project.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                project.setCreatedBy(cursor.getInt(cursor.getColumnIndexOrThrow("created_by")));
                project.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                list.add(project);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
