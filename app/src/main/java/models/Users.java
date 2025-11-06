package models;

import com.google.firebase.Timestamp;

public class Users {

    private String uid;                // ✅ Thay thế id → UID (String)
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    private String username;
    private Timestamp createdTimestamp;
    private String fcmToken;

    public Users() {}

    public Users(String uid, String firstName, String lastName, String email, String password,
                 String username, Timestamp createdTimestamp, String fcmToken) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdTimestamp = createdTimestamp;
        this.fcmToken = fcmToken;
        refreshUsernameIfEmpty();
    }

    // ===== Helpers =====
    private static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private void refreshUsernameIfEmpty() {
        if (username == null || username.trim().isEmpty()) {
            String fn = clean(firstName);
            String ln = clean(lastName);
            String combined = (fn + " " + ln).trim();
            if (!combined.isEmpty()) {
                this.username = combined;
            }
        }
    }

    // ===== GETTER & SETTER =====

    // ✅ UID thay vì int id
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        refreshUsernameIfEmpty();
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        refreshUsernameIfEmpty();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() {
        if (username == null || username.trim().isEmpty()) {
            String fn = clean(firstName);
            String ln = clean(lastName);
            String combined = (fn + " " + ln).trim();
            return combined.isEmpty() ? null : combined;
        }
        return username;
    }

    public void setUsername(String username) {
        this.username = clean(username);
    }

    public Timestamp getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Timestamp ts) { this.createdTimestamp = ts; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @Override
    public String toString() {
        return "Users{" +
                "uid='" + uid + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", username='" + getUsername() + '\'' +
                ", fcmToken='" + fcmToken + '\'' +
                '}';
    }
}
