package fixmemaybe.model;

import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.util.Json;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A reported repair problem (from the proposal: requestId, description,
 * location, status, dateReported + submitRequest(), updateStatus(), displayRequest()).
 */
public class RepairRequest {

    private final int requestId;
    private String problem;          // short title, e.g. "Broken Chair"
    private String description;      // extra details (optional)
    private String location;         // facility name, e.g. "Room 301"
    private String facilityType;     // e.g. "Chair", "Electrical / Light"
    private String dateReported;     // yyyy-MM-dd
    private String photo;            // /uploads/....jpg
    private String reporterId;
    private String reporterName;
    private String reporterRole;
    private Status status;
    private final List<StatusUpdate> history = new ArrayList<>();
    private String createdAt;
    private String updatedAt;

    public RepairRequest(int requestId) {
        this.requestId = requestId;
    }

    // ------------------------------------------------------------ behaviour
    /** Validates the details and records the first "Reported" entry. */
    public void submitRequest(User reporter, String now) {
        if (problem == null || problem.isBlank()) throw new InvalidInputException("Please describe the problem.");
        if (problem.length() > 120) throw new InvalidInputException("Problem title is too long (max 120 characters).");
        if (location == null || location.isBlank()) throw new InvalidInputException("Please choose a location.");
        if (facilityType == null || facilityType.isBlank()) throw new InvalidInputException("Please choose a facility type.");
        if (description != null && description.length() > 1000) throw new InvalidInputException("Details are too long (max 1000 characters).");
        validateDate(dateReported);

        this.reporterId = reporter.getId();
        this.reporterName = reporter.getFullName();
        this.reporterRole = reporter.getRole();
        this.status = Status.REPORTED;
        this.createdAt = now;
        this.updatedAt = now;
        history.clear();
        history.add(new StatusUpdate(Status.REPORTED, "Request submitted", reporterName, reporterRole, now));
    }

    /** Changes the status and keeps a history entry. */
    public void updateStatus(Status newStatus, String note, User staff, String now) {
        if (newStatus == null) throw new InvalidInputException("Please choose a status.");
        String cleanNote = note == null ? "" : note.trim();
        if (newStatus == status && cleanNote.isEmpty()) {
            throw new InvalidInputException("Status is already " + status.getLabel() + ". Add a note or choose another status.");
        }
        if (cleanNote.length() > 500) throw new InvalidInputException("Note is too long (max 500 characters).");
        this.status = newStatus;
        this.updatedAt = now;
        history.add(new StatusUpdate(newStatus, cleanNote, staff.getFullName(), staff.getRole(), now));
    }

    public String displayRequest() {
        return getCode() + " " + problem + " @ " + location + " [" + status.getLabel() + "] reported by " + reporterName;
    }

    public String getCode() {
        return "#" + String.format("%03d", requestId);
    }

    public boolean isOpen() {
        return status != Status.FIXED;
    }

    private static void validateDate(String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            if (d.isAfter(LocalDate.now())) throw new InvalidInputException("Date reported cannot be in the future.");
            if (d.isBefore(LocalDate.now().minusYears(1))) throw new InvalidInputException("Date reported is too old.");
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidInputException("Please choose a valid date.");
        }
    }

    // ------------------------------------------------------------ save / load
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", requestId);
        m.put("code", getCode());
        m.put("problem", problem);
        m.put("description", description);
        m.put("location", location);
        m.put("facilityType", facilityType);
        m.put("dateReported", dateReported);
        m.put("photo", photo);
        m.put("reporterId", reporterId);
        m.put("reporterName", reporterName);
        m.put("reporterRole", reporterRole);
        m.put("status", status.getLabel());
        List<Object> h = new ArrayList<>();
        for (StatusUpdate u : history) h.add(u.toMap());
        m.put("history", h);
        m.put("createdAt", createdAt);
        m.put("updatedAt", updatedAt);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static RepairRequest fromMap(Map<String, Object> m) {
        RepairRequest r = new RepairRequest((int) Json.num(m, "id", 0));
        r.problem = Json.str(m, "problem");
        r.description = Json.str(m, "description");
        r.location = Json.str(m, "location");
        r.facilityType = Json.str(m, "facilityType");
        r.dateReported = Json.str(m, "dateReported");
        String p = Json.str(m, "photo");
        r.photo = p.isEmpty() || "null".equals(p) ? null : p;
        r.reporterId = Json.str(m, "reporterId");
        r.reporterName = Json.str(m, "reporterName");
        r.reporterRole = Json.str(m, "reporterRole");
        r.status = Status.fromLabel(Json.str(m, "status"));
        for (Object o : Json.list(m, "history")) r.history.add(StatusUpdate.fromMap((Map<String, Object>) o));
        r.createdAt = Json.str(m, "createdAt");
        r.updatedAt = Json.str(m, "updatedAt");
        return r;
    }

    // ------------------------------------------------------------ getters / setters
    public int getRequestId() { return requestId; }
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }
    public String getDateReported() { return dateReported; }
    public void setDateReported(String dateReported) { this.dateReported = dateReported; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public String getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public String getReporterRole() { return reporterRole; }
    public Status getStatus() { return status; }
    public List<StatusUpdate> getHistory() { return Collections.unmodifiableList(history); }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
