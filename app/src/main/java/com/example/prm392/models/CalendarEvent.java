package com.example.prm392.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "calendar_events")
public class CalendarEvent {

    @PrimaryKey(autoGenerate = true)
    public int localId;

    public String taskId;      // linked task Firestore ID
    public String eventDate;   // "YYYY-MM-DD" or any string format

    public CalendarEvent(String taskId, String eventDate) {
        this.taskId = taskId;
        this.eventDate = eventDate;
    }

    // Getters (optional)
    public String getTaskId() {
        return taskId;
    }

    public String getEventDate() {
        return eventDate;
    }
}
