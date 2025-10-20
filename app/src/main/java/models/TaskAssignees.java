package models;

public class TaskAssignees {
    private int id;
    private int taskId;
    private int userId;

    public TaskAssignees() {}

    public TaskAssignees(int id, int taskId, int userId) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "TaskAssignees{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", userId=" + userId +
                '}';
    }
}
