package models;

public class CalendarEvents {
    private int eventId;
    private int taskId;
    private String eventDate;

    public CalendarEvents() {}

    public CalendarEvents(int eventId, int taskId, String eventDate) {
        this.eventId = eventId;
        this.taskId = taskId;
        this.eventDate = eventDate;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public String toString() {
        return "CalendarEvents{" +
                "eventId=" + eventId +
                ", taskId=" + taskId +
                ", eventDate='" + eventDate + '\'' +
                '}';
    }
}
