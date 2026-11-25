package fixmemaybe.model;

import fixmemaybe.util.Json;

import java.util.Map;

/**
 * Builds the correct User subclass from saved data.
 * If the email is an authorized admin email, the user ALWAYS becomes
 * MaintenanceStaff (admin) no matter what role was saved.
 */
public final class UserFactory {

    private UserFactory() { }

    public static User fromMap(Map<String, Object> m, boolean isAdminEmail) {
        String id = Json.str(m, "id");
        String name = Json.str(m, "fullName");
        String username = Json.str(m, "username");
        String email = Json.str(m, "email");
        String hash = Json.str(m, "passwordHash");
        String idNumber = Json.str(m, "idNumber");
        String group = Json.str(m, "group");
        String role = Json.str(m, "role");

        User user;
        if (isAdminEmail) {
            user = new MaintenanceStaff(id, name, username, email, hash, idNumber, group);
        } else if (Faculty.ROLE.equalsIgnoreCase(role)) {
            user = new Faculty(id, name, username, email, hash, idNumber, group);
        } else {
            user = new Student(id, name, username, email, hash, idNumber, group);
        }
        user.setCreatedAt(Json.str(m, "createdAt"));
        String photo = Json.str(m, "photo");
        user.setPhoto(photo.isEmpty() || "null".equals(photo) ? null : photo);
        user.setResetRequested(Json.bool(m, "resetRequested"));
        return user;
    }
}
