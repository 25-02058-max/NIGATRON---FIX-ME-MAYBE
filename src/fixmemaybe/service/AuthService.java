package fixmemaybe.service;

import fixmemaybe.data.AppConfig;
import fixmemaybe.data.DataStore;
import fixmemaybe.exception.ApiException;
import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.model.Faculty;
import fixmemaybe.model.MaintenanceStaff;
import fixmemaybe.model.Student;
import fixmemaybe.model.User;
import fixmemaybe.util.Json;
import fixmemaybe.util.PasswordUtil;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handles registration, login and input validation.
 */
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._]{3,30}$");

    private final DataStore store;
    private final AppConfig config;

    public AuthService(DataStore store, AppConfig config) {
        this.store = store;
        this.config = config;
    }

    // ------------------------------------------------------------ register
    public User register(Map<String, Object> body) {
        String fullName = Json.str(body, "fullName");
        String username = Json.str(body, "username");
        String email = Json.str(body, "email").toLowerCase();
        String password = Json.str(body, "password");
        String confirm = Json.str(body, "confirmPassword");
        String role = Json.str(body, "role");
        String idNumber = Json.str(body, "idNumber");
        String group = Json.str(body, "group");

        validateName(fullName);
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        if (!password.equals(confirm)) throw new InvalidInputException("Passwords do not match.");
        store.ensureAdminAccounts(); // admin emails always belong to the admin first
        if (config.isAdminEmail(email)) {
            throw new InvalidInputException("That email is an admin email. Log in with it instead (default password is in the config file).");
        }
        if (store.findByEmail(email) != null) throw new InvalidInputException("That email is already registered.");
        if (store.findByUsername(username) != null) throw new InvalidInputException("That username is already taken.");

        String hash = PasswordUtil.hash(password);
        String id = DataStore.newId();
        User user;
        if (config.isAdminEmail(email)) {
            // Authorized email -> becomes admin automatically
            user = new MaintenanceStaff(id, fullName, username, email, hash, idNumber, group.isEmpty() ? "All Buildings" : group);
        } else if (Faculty.ROLE.equalsIgnoreCase(role)) {
            requireField(idNumber, "Employee ID");
            requireField(group, "Department");
            user = new Faculty(id, fullName, username, email, hash, idNumber, group);
        } else if (Student.ROLE.equalsIgnoreCase(role)) {
            requireField(idNumber, "SR-Code");
            requireField(group, "Section");
            user = new Student(id, fullName, username, email, hash, idNumber, group);
        } else {
            throw new InvalidInputException("Please choose Student or Faculty.");
        }
        user.setCreatedAt(DataStore.now());
        store.addUser(user);
        System.out.println("[INFO] New account: " + user.displayInfo());
        return user;
    }

    // ------------------------------------------------------------ login
    public User login(String identifier, String password) {
        if (identifier.isEmpty() || password.isEmpty()) {
            throw new InvalidInputException("Please enter your username/email and password.");
        }
        store.ensureAdminAccounts(); // picks up newly added admin emails
        User user = store.findByLogin(identifier.trim());
        if (user == null || !user.login(password)) {
            throw new ApiException(401, "Incorrect username/email or password.");
        }
        return user;
    }

    /** "Forgot password": flags the account so the admin can reset it. */
    public void requestPasswordReset(String email) {
        validateEmail(email.toLowerCase());
        User user = store.findByEmail(email);
        if (user != null) {
            user.setResetRequested(true);
            store.saveUsers();
        }
        // Same answer whether or not the email exists (privacy).
    }

    // ------------------------------------------------------------ validation (public: reused by profile in Step 4)
    public static void validateName(String name) {
        if (name.length() < 2) throw new InvalidInputException("Please enter your full name.");
        if (name.length() > 80) throw new InvalidInputException("Name is too long.");
    }

    public static void validateUsername(String username) {
        if (!USERNAME.matcher(username).matches()) {
            throw new InvalidInputException("Username must be 3-30 letters, numbers, dots or underscores.");
        }
    }

    public static void validateEmail(String email) {
        if (!EMAIL.matcher(email).matches()) throw new InvalidInputException("Please enter a valid email address.");
    }

    public static void validatePassword(String password) {
        if (password.length() < 6) throw new InvalidInputException("Password must be at least 6 characters.");
    }

    private static void requireField(String value, String label) {
        if (value == null || value.isBlank()) throw new InvalidInputException(label + " is required.");
    }
}
