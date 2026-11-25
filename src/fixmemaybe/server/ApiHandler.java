package fixmemaybe.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fixmemaybe.data.AppConfig;
import fixmemaybe.data.DataStore;
import fixmemaybe.data.SessionManager;
import fixmemaybe.exception.ApiException;
import fixmemaybe.model.Facility;
import fixmemaybe.model.RepairRequest;
import fixmemaybe.model.User;
import fixmemaybe.service.AuthService;
import fixmemaybe.service.FacilityService;
import fixmemaybe.service.NotificationService;
import fixmemaybe.service.RequestService;
import fixmemaybe.service.UserService;
import fixmemaybe.util.Json;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All /api/... routes. Each route reads JSON and answers with JSON.
 * Errors are caught here (EXCEPTION HANDLING) so the server never crashes.
 */
public class ApiHandler implements HttpHandler {

    private final AppConfig config;
    private final DataStore store;
    private final SessionManager sessions;
    private final AuthService auth;
    private final RequestService requests;
    private final FacilityService facilities;
    private final NotificationService notifications;
    private final UserService users;

    public ApiHandler(AppConfig config, DataStore store, SessionManager sessions, AuthService auth,
                      RequestService requests, FacilityService facilities,
                      NotificationService notifications, UserService users) {
        this.config = config;
        this.store = store;
        this.sessions = sessions;
        this.auth = auth;
        this.requests = requests;
        this.facilities = facilities;
        this.notifications = notifications;
        this.users = users;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath().replaceAll("/+$", "");
            route(ex, method, path.split("/"));
        } catch (ApiException e) {
            HttpUtil.sendJson(ex, e.getStatus(), HttpUtil.error(e.getMessage()));
        } catch (NumberFormatException e) {
            HttpUtil.sendJson(ex, 400, HttpUtil.error("Invalid ID."));
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendJson(ex, 500, HttpUtil.error("Something went wrong on the server. Please try again."));
        } finally {
            ex.close();
        }
    }

    /** p = ["", "api", "requests", "5", "status"] */
    private void route(HttpExchange ex, String m, String[] p) throws IOException {
        String a = p.length > 2 ? p[2] : "";
        String b = p.length > 3 ? p[3] : null;
        String c = p.length > 4 ? p[4] : null;

        switch (a) {
            case "config":
                if (m.equals("GET")) { getConfig(ex); return; }
                break;
            case "network":
                if (m.equals("GET")) { network(ex); return; }
                break;
            case "auth":
                if (m.equals("POST") && "register".equals(b)) { register(ex); return; }
                if (m.equals("POST") && "login".equals(b)) { login(ex); return; }
                if (m.equals("POST") && "logout".equals(b)) { logout(ex); return; }
                if (m.equals("POST") && "forgot".equals(b)) { forgot(ex); return; }
                break;
            case "me":
                if (b == null && m.equals("GET")) { me(ex); return; }
                if (b == null && m.equals("PUT")) { updateProfile(ex); return; }
                if ("password".equals(b) && m.equals("POST")) { changePassword(ex); return; }
                if ("photo".equals(b) && m.equals("POST")) { setPhoto(ex); return; }
                break;
            case "options":
                if (m.equals("GET")) { options(ex); return; }
                break;
            case "stats":
                if (m.equals("GET")) { stats(ex); return; }
                break;
            case "requests":
                if (b == null && m.equals("GET")) { listRequests(ex); return; }
                if (b == null && m.equals("POST")) { submitRequest(ex); return; }
                if (b != null && c == null && m.equals("GET")) { getRequest(ex, Integer.parseInt(b)); return; }
                if (b != null && c == null && m.equals("DELETE")) { deleteRequest(ex, Integer.parseInt(b)); return; }
                if (b != null && "status".equals(c) && m.equals("POST")) { updateStatus(ex, Integer.parseInt(b)); return; }
                break;
            case "facilities":
                if (b == null && m.equals("GET")) { listFacilities(ex); return; }
                if (b == null && m.equals("POST")) { createFacility(ex); return; }
                if (b != null && m.equals("PUT")) { updateFacility(ex, Integer.parseInt(b)); return; }
                if (b != null && m.equals("DELETE")) { deleteFacility(ex, Integer.parseInt(b)); return; }
                break;
            case "notifications":
                if (b == null && m.equals("GET")) { listNotifications(ex); return; }
                if ("read-all".equals(b) && m.equals("POST")) { readAll(ex); return; }
                if (b != null && "read".equals(c) && m.equals("POST")) { readOne(ex, b); return; }
                break;
            case "users":
                if (b == null && m.equals("GET")) { listUsers(ex); return; }
                if (b != null && "reset-password".equals(c) && m.equals("POST")) { resetPassword(ex, b); return; }
                if (b != null && c == null && m.equals("DELETE")) { deleteUser(ex, b); return; }
                break;
            case "admin":
                if ("settings".equals(b) && m.equals("GET")) { adminSettings(ex); return; }
                break;
            case "export":
                if ("requests.csv".equals(b) && m.equals("GET")) { exportCsv(ex); return; }
                break;
            default:
        }
        throw new ApiException(404, "Unknown API route: " + m + " " + String.join("/", p));
    }

    // ================================================================ session helpers
    public User requireUser(HttpExchange ex) {
        String userId = sessions.getUserId(HttpUtil.getCookie(ex, SessionManager.COOKIE));
        User user = userId == null ? null : store.findById(userId);
        if (user == null) throw ApiException.unauthorized();
        return user;
    }

    public User requireAdmin(HttpExchange ex) {
        User user = requireUser(ex);
        if (!user.isAdmin()) throw ApiException.forbidden();
        return user;
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> q = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return q;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            q.put(k, v);
        }
        return q;
    }

    private static Map<String, Object> ok(String key, Object value) {
        Map<String, Object> res = HttpUtil.ok();
        res.put(key, value);
        return res;
    }

    // ================================================================ config / network
    private void getConfig(HttpExchange ex) throws IOException {
        HttpUtil.sendJson(ex, 200, ok("branding", config.getBranding()));
    }

    private void network(HttpExchange ex) throws IOException {
        int httpPort = config.getInt("server.httpPort", 8080);
        int httpsPort = config.getInt("server.httpsPort", 8443);
        List<Object> links = new ArrayList<>();
        for (String ip : WebServer.lanAddresses()) {
            Map<String, Object> l = new LinkedHashMap<>();
            l.put("ip", ip);
            l.put("https", WebServer.httpsRunning ? "https://" + ip + ":" + httpsPort : null);
            l.put("http", "http://" + ip + ":" + httpPort);
            links.add(l);
        }
        Map<String, Object> res = ok("links", links);
        res.put("httpsRunning", WebServer.httpsRunning);
        HttpUtil.sendJson(ex, 200, res);
    }

    // ================================================================ auth
    private void register(HttpExchange ex) throws IOException {
        startSession(ex, auth.register(HttpUtil.readJson(ex)), 201);
    }

    private void login(HttpExchange ex) throws IOException {
        Map<String, Object> body = HttpUtil.readJson(ex);
        startSession(ex, auth.login(Json.str(body, "identifier"), Json.str(body, "password")), 200);
    }

    private void startSession(HttpExchange ex, User user, int status) throws IOException {
        String token = sessions.create(user.getId());
        HttpUtil.setCookie(ex, SessionManager.COOKIE, token, SessionManager.MAX_AGE_SECONDS);
        Map<String, Object> res = ok("user", user.toPublicMap());
        res.put("redirect", user.getDashboardPage()); // POLYMORPHISM: admin.html or user.html
        HttpUtil.sendJson(ex, status, res);
    }

    private void logout(HttpExchange ex) throws IOException {
        sessions.remove(HttpUtil.getCookie(ex, SessionManager.COOKIE));
        HttpUtil.setCookie(ex, SessionManager.COOKIE, "", 0);
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void forgot(HttpExchange ex) throws IOException {
        auth.requestPasswordReset(Json.str(HttpUtil.readJson(ex), "email"));
        HttpUtil.sendJson(ex, 200, ok("message", "Request sent! The maintenance admin will reset your password. "
                + "Please check with them in person or by email."));
    }

    // ================================================================ me / profile
    private void me(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        Map<String, Object> res = ok("user", user.toPublicMap());
        res.put("redirect", user.getDashboardPage());
        res.put("unread", notifications.unreadCount(user));
        HttpUtil.sendJson(ex, 200, res);
    }

    private void updateProfile(HttpExchange ex) throws IOException {
        User user = users.updateProfile(requireUser(ex), HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 200, ok("user", user.toPublicMap()));
    }

    private void changePassword(HttpExchange ex) throws IOException {
        users.changePassword(requireUser(ex), HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void setPhoto(HttpExchange ex) throws IOException {
        User user = users.setPhoto(requireUser(ex), HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 200, ok("user", user.toPublicMap()));
    }

    // ================================================================ requests
    private void options(HttpExchange ex) throws IOException {
        requireUser(ex);
        Map<String, Object> res = ok("locations", facilities.names());
        res.put("types", requests.getTypes());
        res.put("statuses", List.of("Reported", "In Progress", "Fixed"));
        res.put("photoRequired", requests.isPhotoRequired());
        res.put("today", LocalDate.now().toString());
        HttpUtil.sendJson(ex, 200, res);
    }

    private void stats(HttpExchange ex) throws IOException {
        HttpUtil.sendJson(ex, 200, ok("stats", requests.stats(requireUser(ex))));
    }

    private void listRequests(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        Map<String, String> q = query(ex);
        List<Object> list = new ArrayList<>();
        for (RepairRequest r : requests.list(user, q.get("status"), q.get("location"), q.get("type"),
                q.get("q"), "1".equals(q.get("mine")))) {
            list.add(r.toMap());
        }
        HttpUtil.sendJson(ex, 200, ok("requests", list));
    }

    private void submitRequest(HttpExchange ex) throws IOException {
        RepairRequest r = requests.submit(requireUser(ex), HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 201, ok("request", r.toMap()));
    }

    private void getRequest(HttpExchange ex, int id) throws IOException {
        HttpUtil.sendJson(ex, 200, ok("request", requests.get(requireUser(ex), id).toMap()));
    }

    private void deleteRequest(HttpExchange ex, int id) throws IOException {
        requests.delete(requireUser(ex), id);
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void updateStatus(HttpExchange ex, int id) throws IOException {
        RepairRequest r = requests.updateStatus(requireAdmin(ex), id, HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 200, ok("request", r.toMap()));
    }

    // ================================================================ facilities
    private void listFacilities(HttpExchange ex) throws IOException {
        requireUser(ex);
        List<Object> list = new ArrayList<>();
        for (Facility f : facilities.list()) list.add(f.toMap());
        HttpUtil.sendJson(ex, 200, ok("facilities", list));
    }

    private void createFacility(HttpExchange ex) throws IOException {
        requireAdmin(ex);
        HttpUtil.sendJson(ex, 201, ok("facility", facilities.create(HttpUtil.readJson(ex)).toMap()));
    }

    private void updateFacility(HttpExchange ex, int id) throws IOException {
        requireAdmin(ex);
        HttpUtil.sendJson(ex, 200, ok("facility", facilities.update(id, HttpUtil.readJson(ex)).toMap()));
    }

    private void deleteFacility(HttpExchange ex, int id) throws IOException {
        requireAdmin(ex);
        facilities.delete(id);
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    // ================================================================ notifications
    private void listNotifications(HttpExchange ex) throws IOException {
        User user = requireUser(ex);
        Map<String, Object> res = ok("notifications", notifications.list(user));
        res.put("unread", notifications.unreadCount(user));
        HttpUtil.sendJson(ex, 200, res);
    }

    private void readAll(HttpExchange ex) throws IOException {
        notifications.markAllRead(requireUser(ex));
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void readOne(HttpExchange ex, String id) throws IOException {
        notifications.markRead(requireUser(ex), id);
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    // ================================================================ users (admin)
    private void listUsers(HttpExchange ex) throws IOException {
        requireAdmin(ex);
        HttpUtil.sendJson(ex, 200, ok("users", users.listUsers()));
    }

    private void resetPassword(HttpExchange ex, String id) throws IOException {
        users.resetPassword(requireAdmin(ex), id, HttpUtil.readJson(ex));
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void deleteUser(HttpExchange ex, String id) throws IOException {
        users.deleteUser(requireAdmin(ex), id);
        HttpUtil.sendJson(ex, 200, HttpUtil.ok());
    }

    private void adminSettings(HttpExchange ex) throws IOException {
        requireAdmin(ex);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("adminEmails", new ArrayList<>(config.getAdminEmails()));
        s.put("dataFolder", store.getDataDir().toAbsolutePath().toString());
        s.put("configFile", store.getDataDir().toAbsolutePath().getParent().resolve("config").resolve("app.properties").toString());
        s.put("userCount", store.getAllUsers().size());
        s.put("requestCount", store.getAllRequests().size());
        s.put("facilityCount", store.getAllFacilities().size());
        s.put("types", requests.getTypes());
        s.put("photoRequired", requests.isPhotoRequired());
        s.put("branding", config.getBranding());
        HttpUtil.sendJson(ex, 200, ok("settings", s));
    }

    private void exportCsv(HttpExchange ex) throws IOException {
        requireAdmin(ex);
        byte[] bytes = requests.exportCsv().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        ex.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"repair-requests-" + LocalDate.now() + ".csv\"");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
