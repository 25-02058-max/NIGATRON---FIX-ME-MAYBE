package fixmemaybe.data;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Reads config/app.properties.
 * The file is re-read automatically when it changes, so admin emails
 * and branding can be edited while the server is running.
 */
public class AppConfig {

    private final Path file;
    private Properties props = new Properties();
    private long lastModified = -1;

    public AppConfig(Path file) {
        this.file = file;
        reloadIfChanged();
    }

    public synchronized void reloadIfChanged() {
        try {
            if (!Files.exists(file)) {
                System.out.println("[WARN] Config file not found: " + file);
                return;
            }
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == lastModified) return;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            props = p;
            lastModified = modified;
        } catch (Exception e) {
            System.out.println("[WARN] Could not read config: " + e.getMessage());
        }
    }

    public String get(String key, String def) {
        reloadIfChanged();
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(get(key, String.valueOf(def))); } catch (NumberFormatException e) { return def; }
    }

    public boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(get(key, String.valueOf(def)));
    }

    /** All authorized admin emails, lower-case. */
    public Set<String> getAdminEmails() {
        Set<String> set = new LinkedHashSet<>();
        for (String part : get("admin.emails", "").split(",")) {
            String email = part.trim().toLowerCase();
            if (!email.isEmpty()) set.add(email);
        }
        return set;
    }

    public boolean isAdminEmail(String email) {
        return email != null && getAdminEmails().contains(email.trim().toLowerCase());
    }

    /** Branding values sent to the web pages. */
    public Map<String, Object> getBranding() {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("appName", get("app.name", "Fix Me Maybe"));
        b.put("tagline", get("app.tagline", "School Facility Repair System"));
        List<String> motto = new ArrayList<>();
        for (String m : get("app.motto", "Report,Track,Fix").split(",")) if (!m.isBlank()) motto.add(m.trim());
        b.put("motto", motto);
        b.put("schoolName", get("school.name", ""));
        b.put("schoolSubtitle", get("school.subtitle", ""));
        b.put("logo", get("brand.logo", "assets/logo.svg"));
        b.put("logoLight", get("brand.logoLight", "assets/logo-white.svg"));
        b.put("schoolLogo", get("brand.schoolLogo", "assets/school-logo.svg"));
        b.put("loginBackground", get("brand.loginBackground", "assets/login-bg.svg"));
        b.put("splashBackground", get("brand.splashBackground", "assets/login-bg.svg"));
        b.put("primaryColor", get("brand.primaryColor", "#8B1A1F"));
        b.put("primaryDark", get("brand.primaryDark", "#6B0F14"));
        return b;
    }
}
