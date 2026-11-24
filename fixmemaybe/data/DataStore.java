package fixmemaybe.data;

import fixmemaybe.model.Facility;
import fixmemaybe.model.MaintenanceStaff;
import fixmemaybe.model.Notification;
import fixmemaybe.model.RepairRequest;
import fixmemaybe.model.User;
import fixmemaybe.model.UserFactory;
import fixmemaybe.util.Json;
import fixmemaybe.util.PasswordUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LOCAL STORAGE ("database"): everything is saved as JSON files inside the
 * data/ folder on the laptop running the server.
 *
 *   data/users.json          accounts
 *   data/requests.json       repair requests
 *   data/facilities.json     rooms / places that can be reported
 *   data/notifications.json  bell notifications
 *   data/uploads/            photos taken by phones
 */
public class DataStore {

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Path dataDir;
    private final Path uploadsDir;
    private final AppConfig config;

    // ARRAYS / COLLECTIONS holding all objects in memory
    private final List<User> users = new ArrayList<>();
    private final List<RepairRequest> requests = new ArrayList<>();
    private final List<Facility> facilities = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private int nextRequestId = 1;
    private int nextFacilityId = 1;

    public DataStore(Path dataDir, AppConfig config) {
        this.dataDir = dataDir;
        this.uploadsDir = dataDir.resolve("uploads");
        this.config = config;
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create data folder: " + dataDir, e);
        }
        loadUsers();
        loadRequests();
        loadFacilities();
        loadNotifications();
    }

    public Path getDataDir() { return dataDir; }
    public Path getUploadsDir() { return uploadsDir; }

    public static String now() { return LocalDateTime.now().format(TIME); }

    public static String newId() { return UUID.randomUUID().toString(); }

    // ================================================================== USERS
    private synchronized void loadUsers() {
        users.clear();
        for (Map<String, Object> m : readList("users.json", "users")) {
            users.add(UserFactory.fromMap(m, config.isAdminEmail(Json.str(m, "email"))));
        }
    }

    public synchronized void saveUsers() {
        List<Object> list = new ArrayList<>();
        for (User u : users) list.add(u.toMap());
        writeList("users.json", "users", list, null);
    }

    /** Keeps admin rights in sync with the CURRENT admin.emails list. */
    private User refreshRole(User u) {
        boolean shouldBeAdmin = config.isAdminEmail(u.getEmail());
        if (shouldBeAdmin == u.isAdmin()) return u;
        User fresh = UserFactory.fromMap(u.toMap(), shouldBeAdmin);
        users.set(users.indexOf(u), fresh);
        return fresh;
    }

    public synchronized User findById(String id) {
        for (User u : users) if (u.getId().equals(id)) return refreshRole(u);
        return null;
    }

    public synchronized User findByEmail(String email) {
        for (User u : users) if (u.getEmail().equalsIgnoreCase(email)) return refreshRole(u);
        return null;
    }

    public synchronized User findByUsername(String username) {
        for (User u : users) if (u.getUsername().equalsIgnoreCase(username)) return refreshRole(u);
        return null;
    }

    /** Login accepts username OR email. */
    public synchronized User findByLogin(String identifier) {
        User u = findByEmail(identifier);
        return u != null ? u : findByUsername(identifier);
    }

    public synchronized List<User> getAllUsers() {
        List<User> copy = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) copy.add(refreshRole(users.get(i)));
        return copy;
    }

    public synchronized void addUser(User user) {
        users.add(user);
        saveUsers();
    }

    public synchronized void removeUser(User user) {
        users.removeIf(u -> u.getId().equals(user.getId()));
        notifications.removeIf(n -> n.getUserId().equals(user.getId()));
        saveUsers();
        saveNotifications();
    }

    /** Creates accounts for admin emails that have none yet (admin.defaultPassword). */
    public synchronized void ensureAdminAccounts() {
        String defaultPass = config.get("admin.defaultPassword", "admin123");
        boolean changed = false;
        int n = 1;
        for (String email : config.getAdminEmails()) {
            if (findByEmail(email) != null) continue;
            String base = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length())
                    .replaceAll("[^A-Za-z0-9._]", "");
            if (base.length() < 3) base = "admin";
            String username = base;
            while (findByUsername(username) != null) username = base + (n++);
            MaintenanceStaff admin = new MaintenanceStaff(newId(), "Maintenance Admin", username, email,
                    PasswordUtil.hash(defaultPass), "STAFF-" + String.format("%03d", users.size() + 1), "All Buildings");
            admin.setCreatedAt(now());
            users.add(admin);
            changed = true;
            System.out.println("[INFO] Created admin account: " + email + "  (password: " + defaultPass + ")");
        }
        if (changed) saveUsers();
    }

    // ================================================================== REQUESTS
    private synchronized void loadRequests() {
        requests.clear();
        Map<String, Object> root = readRoot("requests.json");
        for (Object o : Json.list(root, "requests")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            try {
                requests.add(RepairRequest.fromMap(m));
            } catch (Exception e) {
                System.out.println("[WARN] Skipped a damaged request: " + e.getMessage());
            }
        }
        int maxId = 0;
        for (RepairRequest r : requests) maxId = Math.max(maxId, r.getRequestId());
        nextRequestId = (int) Math.max(Json.num(root, "nextId", 1), maxId + 1);
    }

    public synchronized void saveRequests() {
        List<Object> list = new ArrayList<>();
        for (RepairRequest r : requests) list.add(r.toMap());
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("nextId", nextRequestId);
        writeList("requests.json", "requests", list, extra);
    }

    public synchronized RepairRequest newRequest() {
        return new RepairRequest(nextRequestId++);
    }

    public synchronized void addRequest(RepairRequest r) {
        requests.add(r);
        saveRequests();
    }

    public synchronized void removeRequest(RepairRequest r) {
        requests.remove(r);
        notifications.removeIf(n -> n.getRequestId() == r.getRequestId());
        saveRequests();
        saveNotifications();
    }

    public synchronized RepairRequest findRequest(int id) {
        for (RepairRequest r : requests) if (r.getRequestId() == id) return r;
        return null;
    }

    public synchronized List<RepairRequest> getAllRequests() {
        return new ArrayList<>(requests);
    }

    // ================================================================== FACILITIES
    private synchronized void loadFacilities() {
        facilities.clear();
        Map<String, Object> root = readRoot("facilities.json");
        for (Object o : Json.list(root, "facilities")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            try { facilities.add(Facility.fromMap(m)); } catch (Exception ignored) { }
        }
        if (!Files.exists(dataDir.resolve("facilities.json"))) seedFacilities();
        int maxId = 0;
        for (Facility f : facilities) maxId = Math.max(maxId, f.getFacilityId());
        nextFacilityId = maxId + 1;
    }

    /** Sample places on first run. Admin can edit them in the Facilities page. */
    private void seedFacilities() {
        String[][] seed = {
            {"Room 101", "Main Building", "Classroom"}, {"Room 102", "Main Building", "Classroom"},
            {"Room 201", "Main Building", "Classroom"}, {"Room 205", "Main Building", "Classroom"},
            {"Room 301", "CICS Building", "Classroom"}, {"Room 305", "CICS Building", "Classroom"},
            {"Computer Lab 1", "CICS Building", "Laboratory"}, {"Computer Lab 2", "CICS Building", "Laboratory"},
            {"Library", "Library Building", "Library"}, {"Gymnasium", "Gym", "Sports Facility"},
            {"Canteen", "Canteen", "Common Area"}, {"Faculty Room", "Main Building", "Office"},
            {"Restroom (Main)", "Main Building", "Restroom"}, {"Hallway", "Main Building", "Common Area"},
        };
        int id = 1;
        for (String[] s : seed) facilities.add(new Facility(id++, s[0], s[1], s[2]));
        saveFacilities();
    }

    public synchronized void saveFacilities() {
        List<Object> list = new ArrayList<>();
        for (Facility f : facilities) {
            Map<String, Object> m = f.toMap();
            m.remove("condition");
            m.remove("openRequests");
            list.add(m);
        }
        writeList("facilities.json", "facilities", list, null);
    }

    public synchronized List<Facility> getAllFacilities() { return new ArrayList<>(facilities); }

    public synchronized Facility findFacility(int id) {
        for (Facility f : facilities) if (f.getFacilityId() == id) return f;
        return null;
    }

    public synchronized Facility findFacilityByName(String name) {
        for (Facility f : facilities) if (f.getName().equalsIgnoreCase(name.trim())) return f;
        return null;
    }

    public synchronized Facility addFacility(String name, String location, String type) {
        Facility f = new Facility(nextFacilityId++, name, location, type);
        facilities.add(f);
        saveFacilities();
        return f;
    }

    public synchronized void removeFacility(Facility f) {
        facilities.remove(f);
        saveFacilities();
    }

    // ================================================================== NOTIFICATIONS
    private synchronized void loadNotifications() {
        notifications.clear();
        for (Map<String, Object> m : readList("notifications.json", "notifications")) {
            try { notifications.add(Notification.fromMap(m)); } catch (Exception ignored) { }
        }
    }

    public synchronized void saveNotifications() {
        // keep only the newest 1000 so the file stays small
        while (notifications.size() > 1000) notifications.remove(0);
        List<Object> list = new ArrayList<>();
        for (Notification n : notifications) list.add(n.toMap());
        writeList("notifications.json", "notifications", list, null);
    }

    public synchronized void addNotification(Notification n) {
        notifications.add(n);
    }

    public synchronized List<Notification> notificationsFor(String userId) {
        List<Notification> list = new ArrayList<>();
        for (int i = notifications.size() - 1; i >= 0; i--) {
            if (notifications.get(i).getUserId().equals(userId)) list.add(notifications.get(i));
        }
        return list; // newest first
    }

    // ================================================================== file helpers
    private Map<String, Object> readRoot(String fileName) {
        Path file = dataDir.resolve(fileName);
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try {
            return Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("[ERROR] " + fileName + " is damaged: " + e.getMessage());
            try {
                Files.copy(file, dataDir.resolve(fileName + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) { }
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readList(String fileName, String key) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : Json.list(readRoot(fileName), key)) out.add((Map<String, Object>) o);
        return out;
    }

    private void writeList(String fileName, String key, List<Object> list, Map<String, Object> extra) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (extra != null) root.putAll(extra);
        root.put(key, list);
        writeAtomically(dataDir.resolve(fileName), Json.pretty(root));
    }

    public static void writeAtomically(Path file, String content) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
