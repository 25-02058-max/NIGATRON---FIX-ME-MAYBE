package fixmemaybe.data;

import fixmemaybe.util.Json;
import fixmemaybe.util.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps track of who is logged in (token -> user id).
 * Saved to data/sessions.json so users stay logged in after a restart.
 */
public class SessionManager {

    public static final String COOKIE = "FMM_SESSION";
    public static final long MAX_AGE_SECONDS = 7L * 24 * 60 * 60; // 7 days

    private final Path file;
    private final Map<String, String> userByToken = new HashMap<>();
    private final Map<String, Long> expiryByToken = new HashMap<>();

    public SessionManager(Path file) {
        this.file = file;
        load();
    }

    public synchronized String create(String userId) {
        String token = PasswordUtil.randomToken();
        userByToken.put(token, userId);
        expiryByToken.put(token, System.currentTimeMillis() + MAX_AGE_SECONDS * 1000);
        save();
        return token;
    }

    /** Returns the user id for this token, or null if missing/expired. */
    public synchronized String getUserId(String token) {
        if (token == null) return null;
        Long exp = expiryByToken.get(token);
        if (exp == null) return null;
        if (exp < System.currentTimeMillis()) {
            remove(token);
            return null;
        }
        return userByToken.get(token);
    }

    public synchronized void remove(String token) {
        if (token == null) return;
        userByToken.remove(token);
        expiryByToken.remove(token);
        save();
    }

    /** Logs a user out everywhere (used after password change by admin). */
    public synchronized void removeAllFor(String userId) {
        Iterator<Map.Entry<String, String>> it = userByToken.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equals(userId)) {
                expiryByToken.remove(e.getKey());
                it.remove();
            }
        }
        save();
    }

    private void load() {
        try {
            if (!Files.exists(file)) return;
            Map<String, Object> root = Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Object> e : root.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> s = (Map<String, Object>) e.getValue();
                long exp = Json.num(s, "expires", 0);
                if (exp > now) {
                    userByToken.put(e.getKey(), Json.str(s, "userId"));
                    expiryByToken.put(e.getKey(), exp);
                }
            }
        } catch (Exception e) {
            System.out.println("[WARN] Could not read sessions: " + e.getMessage());
        }
    }

    private void save() {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : userByToken.entrySet()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("userId", e.getValue());
            s.put("expires", expiryByToken.get(e.getKey()));
            root.put(e.getKey(), s);
        }
        DataStore.writeAtomically(file, Json.stringify(root));
    }
}
