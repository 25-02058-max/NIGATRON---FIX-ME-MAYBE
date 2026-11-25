package fixmemaybe.service;

import fixmemaybe.data.DataStore;
import fixmemaybe.exception.ApiException;
import fixmemaybe.model.Notification;
import fixmemaybe.model.RepairRequest;
import fixmemaybe.model.Status;
import fixmemaybe.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Creates and reads the bell notifications. */
public class NotificationService {

    private final DataStore store;

    public NotificationService(DataStore store) {
        this.store = store;
    }

    /** Tells every admin that a new request arrived. */
    public void newRequest(RepairRequest r) {
        for (User u : store.getAllUsers()) {
            if (!u.isAdmin()) continue;
            store.addNotification(new Notification(DataStore.newId(), u.getId(), r.getRequestId(),
                    "New report: " + r.getProblem(),
                    r.getReporterName() + " (" + r.getReporterRole() + ") reported a problem at " + r.getLocation() + ".",
                    "reported", DataStore.now()));
        }
        store.saveNotifications();
    }

    /** Tells the reporter that the status of their request changed. */
    public void statusChanged(RepairRequest r, String note) {
        if (r.getReporterId() == null || store.findById(r.getReporterId()) == null) return;
        Status s = r.getStatus();
        String type = s == Status.FIXED ? "fixed" : s == Status.IN_PROGRESS ? "progress" : "reported";
        String msg = "Your request " + r.getCode() + " (" + r.getProblem() + ") is now " + s.getLabel() + ".";
        if (note != null && !note.isBlank()) msg += " Note: " + note.trim();
        store.addNotification(new Notification(DataStore.newId(), r.getReporterId(), r.getRequestId(),
                r.getProblem() + " - " + s.getLabel(), msg, type, DataStore.now()));
        store.saveNotifications();
    }

    public List<Map<String, Object>> list(User user) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Notification n : store.notificationsFor(user.getId())) out.add(n.toMap());
        return out;
    }

    public int unreadCount(User user) {
        int c = 0;
        for (Notification n : store.notificationsFor(user.getId())) if (!n.isRead()) c++;
        return c;
    }

    public void markRead(User user, String id) {
        for (Notification n : store.notificationsFor(user.getId())) {
            if (n.getId().equals(id)) {
                n.markRead();
                store.saveNotifications();
                return;
            }
        }
        throw ApiException.notFound("Notification");
    }

    public void markAllRead(User user) {
        for (Notification n : store.notificationsFor(user.getId())) n.markRead();
        store.saveNotifications();
    }
}
