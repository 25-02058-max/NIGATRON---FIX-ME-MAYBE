package fixmemaybe.model;

import fixmemaybe.util.PasswordUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ABSTRACTION: a general user of the system. It cannot be created directly;
 * only Student, Faculty and MaintenanceStaff objects exist.
 *
 * ENCAPSULATION: every field is private and accessed through getters/setters.
 */
public abstract class User {

    private String id;
    private String fullName;
    private String username;
    private String email;
    private String passwordHash;
    private String createdAt;
    private String photo;              // profile picture path (optional)
    private boolean resetRequested;    // user clicked "Forgot password"

    protected User(String id, String fullName, String username, String email, String passwordHash) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // ---------------------------------------------------------- abstract (POLYMORPHISM)
    /** Display name of the role, e.g. "Student". */
    public abstract String getRole();

    /** True only for admin / maintenance accounts. */
    public abstract boolean isAdmin();

    /** Which page this user lands on after logging in. */
    public abstract String getDashboardPage();

    /** Role-specific fields (studentId, department, ...). */
    protected abstract void writeExtraInfo(Map<String, Object> map);

    // ---------------------------------------------------------- shared behaviour
    /** Checks the password. Returns true when it matches. */
    public boolean login(String password) {
        return PasswordUtil.verify(password, passwordHash);
    }

    public void changePassword(String newPassword) {
        this.passwordHash = PasswordUtil.hash(newPassword);
        this.resetRequested = false;
    }

    /** Short text description; subclasses add their own details. */
    public String displayInfo() {
        return fullName + " (" + getRole() + ") - " + email;
    }

    /** Full data for saving to the file (includes password hash). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = toPublicMap();
        map.put("passwordHash", passwordHash);
        return map;
    }

    /** Safe data to send to the browser (NO password hash). */
    public Map<String, Object> toPublicMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("role", getRole());
        map.put("isAdmin", isAdmin());
        map.put("fullName", fullName);
        map.put("username", username);
        map.put("email", email);
        map.put("photo", photo);
        map.put("createdAt", createdAt);
        map.put("resetRequested", resetRequested);
        writeExtraInfo(map);
        return map;
    }

    // ---------------------------------------------------------- getters / setters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public boolean isResetRequested() { return resetRequested; }
    public void setResetRequested(boolean resetRequested) { this.resetRequested = resetRequested; }
}
