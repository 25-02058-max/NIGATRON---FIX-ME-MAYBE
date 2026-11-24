package fixmemaybe.service;

import fixmemaybe.data.AppConfig;
import fixmemaybe.data.DataStore;
import fixmemaybe.data.PhotoStorage;
import fixmemaybe.data.SessionManager;
import fixmemaybe.exception.ApiException;
import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.model.Faculty;
import fixmemaybe.model.MaintenanceStaff;
import fixmemaybe.model.Student;
import fixmemaybe.model.User;
import fixmemaybe.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Profile editing, password changes, and admin user management. */
public class UserService {

    private final DataStore store;
    private final AppConfig config;
    private final PhotoStorage photos;
    private final SessionManager sessions;

    public UserService(DataStore store, AppConfig config, PhotoStorage photos, SessionManager sessions) {
        this.store = store;
        this.config = config;
        this.photos = photos;
        this.sessions = sessions;
    }

    // ------------------------------------------------------------ own profile
    public User updateProfile(User user, Map<String, Object> body) {
        String fullName = Json.str(body, "fullName");
        String username = Json.str(body, "username");
        String email = Json.str(body, "email").toLowerCase();
        String idNumber = Json.str(body, "idNumber");
        String group = Json.str(body, "group");

        AuthService.validateName(fullName);
        AuthService.validateUsername(username);
        AuthService.validateEmail(email);
        User byEmail = store.findByEmail(email);
        if (byEmail != null && !byEmail.getId().equals(user.getId())) throw new InvalidInputException("That email is already used.");
        User byName = store.findByUsername(username);
        if (byName != null && !byName.getId().equals(user.getId())) throw new InvalidInputException("That username is already taken.");
        // Security: nobody can turn themselves into an admin by changing email.
        if (!user.isAdmin() && config.isAdminEmail(email)) throw new InvalidInputException("That email is reserved for administrators.");
        if (user.isAdmin() && !config.isAdminEmail(email)) {
            throw new InvalidInputException("Admins must keep an email listed in admin.emails (config/app.properties).");
        }

        synchronized (store) {
            user.setFullName(fullName);
            user.setUsername(username);
            user.setEmail(email);
            // POLYMORPHISM in action: each subclass stores its own ID/group fields
            if (user instanceof Student) {
                ((Student) user).setStudentId(idNumber);
                ((Student) user).setSection(group);
            } else if (user instanceof Faculty) {
                ((Faculty) user).setEmployeeId(idNumber);
                ((Faculty) user).setDepartment(group);
            } else if (user instanceof MaintenanceStaff) {
                ((MaintenanceStaff) user).setStaffId(idNumber);
                ((MaintenanceStaff) user).setAssignedArea(group);
            }
            store.saveUsers();
        }
        return user;
    }

    public void changePassword(User user, Map<String, Object> body) {
        if (!user.login(Json.str(body, "currentPassword"))) throw new InvalidInputException("Current password is incorrect.");
        String newPass = Json.str(body, "newPassword");
        AuthService.validatePassword(newPass);
        if (!newPass.equals(Json.str(body, "confirmPassword"))) throw new InvalidInputException("New passwords do not match.");
        synchronized (store) {
            user.changePassword(newPass);
            store.saveUsers();
        }
    }

    public User setPhoto(User user, Map<String, Object> body) {
        String data = Json.str(body, "photo");
        synchronized (store) {
            String old = user.getPhoto();
            user.setPhoto(data.isEmpty() ? null : photos.save(data, "avatar-" + user.getUsername()));
            photos.delete(old);
            store.saveUsers();
        }
        return user;
    }

    // ------------------------------------------------------------ admin
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : store.getAllUsers()) out.add(u.toPublicMap());
        out.sort((a, b) -> String.valueOf(a.get("fullName")).compareToIgnoreCase(String.valueOf(b.get("fullName"))));
        return out;
    }

    public void resetPassword(User admin, String userId, Map<String, Object> body) {
        User target = store.findById(userId);
        if (target == null) throw ApiException.notFound("User");
        String newPass = Json.str(body, "newPassword");
        AuthService.validatePassword(newPass);
        synchronized (store) {
            target.changePassword(newPass);
            store.saveUsers();
        }
        if (!target.getId().equals(admin.getId())) sessions.removeAllFor(target.getId());
        System.out.println("[INFO] " + admin.getFullName() + " reset the password of " + target.getEmail());
    }

    public void deleteUser(User admin, String userId) {
        User target = store.findById(userId);
        if (target == null) throw ApiException.notFound("User");
        if (target.getId().equals(admin.getId())) throw new InvalidInputException("You cannot delete your own account.");
        if (target.isAdmin()) throw new InvalidInputException("Remove this email from admin.emails first, then delete the account.");
        photos.delete(target.getPhoto());
        sessions.removeAllFor(target.getId());
        store.removeUser(target);
    }
}
