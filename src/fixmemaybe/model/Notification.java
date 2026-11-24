package fixmemaybe.model;

import fixmemaybe.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** A message shown under the bell icon (e.g. "Your request #002 is now In Progress"). */
public class Notification {

    private final String id;
    private final String userId;
    private final int requestId;
    private final String title;
    private final String message;
    private final String type;      // reported / progress / fixed / info
    private boolean read;
    private final String createdAt;

    public Notification(String id, String userId, int requestId, String title, String message, String type, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.requestId = requestId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
    }

    public void markRead() { this.read = true; }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getRequestId() { return requestId; }
    public boolean isRead() { return read; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("userId", userId);
        m.put("requestId", requestId);
        m.put("title", title);
        m.put("message", message);
        m.put("type", type);
        m.put("read", read);
        m.put("createdAt", createdAt);
        return m;
    }

    public static Notification fromMap(Map<String, Object> m) {
        Notification n = new Notification(Json.str(m, "id"), Json.str(m, "userId"), (int) Json.num(m, "requestId", 0),
                Json.str(m, "title"), Json.str(m, "message"), Json.str(m, "type"), Json.str(m, "createdAt"));
        n.read = Json.bool(m, "read");
        return n;
    }
}
