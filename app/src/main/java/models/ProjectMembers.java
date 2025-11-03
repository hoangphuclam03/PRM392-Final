package models;

public class ProjectMembers {
    private int id;
    private int projectId;
    private int userId;
    private String role;
    private String name; // 👈 thêm trường này để hiển thị tên

    public ProjectMembers() {}

    // Constructor gốc (giữ lại)
    public ProjectMembers(int id, int projectId, int userId, String role) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    // ✅ Constructor mới — dùng khi hiển thị (có cả tên)
    public ProjectMembers(int id, int projectId, int userId, String name, String role) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ProjectMembers{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
