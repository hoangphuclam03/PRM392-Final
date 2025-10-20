package utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import models.Users;

public class DBConnect extends SQLiteOpenHelper {

    // Tên database, bảng và version
    private static final String dbName = "PRM392.db";
    private static final String dbTable = "users";
    // 🔺 Tăng version lên để SQLite gọi lại onUpgrade() và tạo bảng mới
    private static final int dbVersion = 2;

    // Các cột trong bảng users
    private static final String id = "id";
    private static final String firstName = "firstName";
    private static final String lastName = "lastName";
    private static final String email = "email";
    private static final String password = "password";

    public DBConnect(@Nullable Context context) {
        super(context, dbName, null, dbVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1️⃣ Bảng USERS (giữ nguyên)
        String query = "CREATE TABLE " + dbTable + " (" +
                id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                firstName + " TEXT NOT NULL, " +
                lastName + " TEXT NOT NULL, " +
                email + " TEXT UNIQUE NOT NULL, " +
                password + " TEXT NOT NULL" +
                ");";
        db.execSQL(query);

        // 2️⃣ Bảng PROJECTS
        db.execSQL("CREATE TABLE projects (" +
                "project_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_name TEXT NOT NULL, " +
                "description TEXT, " +
                "created_by INTEGER, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (created_by) REFERENCES users(id)" +
                ");");

        // 3️⃣ Bảng PROJECT_MEMBERS
        db.execSQL("CREATE TABLE project_members (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_id INTEGER, " +
                "user_id INTEGER, " +
                "role TEXT DEFAULT 'member', " +
                "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // 4️⃣ Bảng TASKS
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

        // 5️⃣ Bảng TASK_ASSIGNEES
        db.execSQL("CREATE TABLE task_assignees (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER, " +
                "user_id INTEGER, " +
                "FOREIGN KEY (task_id) REFERENCES tasks(task_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // 6️⃣ Bảng MESSAGES (chat)
        db.execSQL("CREATE TABLE messages (" +
                "message_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "project_id INTEGER, " +
                "sender_id INTEGER, " +
                "content TEXT NOT NULL, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (project_id) REFERENCES projects(project_id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id)" +
                ");");

        // 7️⃣ Bảng NOTIFICATIONS
        db.execSQL("CREATE TABLE notifications (" +
                "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "content TEXT, " +
                "is_read INTEGER DEFAULT 0, " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // 8️⃣ Bảng CALENDAR_EVENTS (optional)
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

    // ⚙️ Hàm thêm user (giữ nguyên code cũ)
    public void addUser(Users user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(firstName, user.getFirstName());
        values.put(lastName, user.getLastName());
        values.put(email, user.getEmail());
        values.put(password, user.getPassword());
        db.insert(dbTable, null, values);
    }
}
