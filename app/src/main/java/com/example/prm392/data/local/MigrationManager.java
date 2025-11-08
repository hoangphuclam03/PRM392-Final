package com.example.prm392.data.local;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class MigrationManager {

    // ===== MIGRATION 1 → 2 =====
    // Example: added ChatEntity, NotificationEntity
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `chats` (
                            `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `chatId` TEXT,
                            `senderId` TEXT,
                            `receiverId` TEXT,
                            `projectId` TEXT,
                            `message` TEXT,
                            `timestamp` INTEGER,
                            `isPendingSync` INTEGER NOT NULL DEFAULT 0
                        )
                    """);

            db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `notifications` (
                            `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT,
                            `message` TEXT,
                            `timestamp` INTEGER
                        )
                    """);
        }
    };

    // ===== MIGRATION 2 → 3 =====
    // Example: added TeamEntity table
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `teams` (
                            `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `teamId` TEXT,
                            `name` TEXT,
                            `ownerId` TEXT,
                            `createdAt` INTEGER,
                            `updatedAt` INTEGER
                        )
                    """);
        }
    };

    // ===== MIGRATION 3 → 4 =====
    // Example: add new column isArchived to projects
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE projects ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0");
        }
    };

    // ===== MIGRATION 4 → 5 =====
    // Example: add new column avatarUrl to users
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN avatarUrl TEXT");
        }
    };

    // 🔧 Helper to collect all registered migrations
    public static Migration[] getAllMigrations() {
        return new Migration[]{
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5
        };
    }
}
