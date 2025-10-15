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
    private static final int dbVersion = 1;

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
        // Lệnh tạo bảng users
        String query = "CREATE TABLE " + dbTable + " (" +
                id + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                firstName + " TEXT NOT NULL, " +
                lastName + " TEXT NOT NULL, " +
                email + " TEXT UNIQUE NOT NULL, " +
                password + " TEXT NOT NULL" +
                ");";

        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nếu có thay đổi version, xóa bảng cũ và tạo lại
        db.execSQL("DROP TABLE IF EXISTS " + dbTable);
        onCreate(db);
    }

    // hàm add user
    public void addUser(Users user){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(firstName, user.getFirstName());
        values.put(lastName, user.getLastName());
        values.put(email, user.getEmail());
        values.put(password, user.getPassword());
        db.insert(dbTable, null,values);
    }

}
