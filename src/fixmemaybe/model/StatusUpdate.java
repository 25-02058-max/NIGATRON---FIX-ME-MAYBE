package fixmemaybe.model;

import fixmemaybe.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/** One entry in a request's status history (who changed it, when, and notes). */
public class StatusUpdate {

    private final Status status;
    private final String note;
    private final String updatedBy;
    private final String updatedByRole;
    private final String at;

    public StatusUpdate(Status status, String note, String updatedBy, String updatedByRole, String at) {
        this.status = status;
        this.note = note == null ? "" : note;
        this.updatedBy = updatedBy;
        this.updatedByRole = updatedByRole;
        this.at = at;
    }

    public Status getStatus() { return status; }
    public String getNote() { return note; }
    public String getUpdatedBy() { return updatedBy; }
    public String getUpdatedByRole() { return updatedByRole; }
    public String getAt() { return at; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.getLabel());
        m.put("note", note);
        m.put("updatedBy", updatedBy);
        m.put("updatedByRole", updatedByRole);
        m.put("at", at);
        return m;
    }

    public static StatusUpdate fromMap(Map<String, Object> m) {
        return new StatusUpdate(Status.fromLabel(Json.str(m, "status")), Json.str(m, "note"),
                Json.str(m, "updatedBy"), Json.str(m, "updatedByRole"), Json.str(m, "at"));
    }
}
