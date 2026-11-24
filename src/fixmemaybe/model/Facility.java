package fixmemaybe.model;

import fixmemaybe.exception.InvalidInputException;
import fixmemaybe.util.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A school facility / place that can be reported
 * (from the proposal: facilityId, name, location, type + displayFacility(), updateCondition()).
 */
public class Facility {

    public static final String GOOD = "Good";
    public static final String NEEDS_REPAIR = "Needs Repair";
    public static final String UNDER_REPAIR = "Under Repair";

    private final int facilityId;
    private String name;       // e.g. "Room 301"
    private String location;   // building, e.g. "CICS Building"
    private String type;       // e.g. "Classroom", "Laboratory"
    private String condition = GOOD;
    private int openRequests;

    public Facility(int facilityId, String name, String location, String type) {
        this.facilityId = facilityId;
        setName(name);
        this.location = location == null ? "" : location.trim();
        this.type = type == null || type.isBlank() ? "Other" : type.trim();
    }

    /** Recalculates the condition from the requests that are not yet fixed. */
    public void updateCondition(int open, int inProgress) {
        this.openRequests = open;
        if (open == 0) condition = GOOD;
        else if (inProgress > 0) condition = UNDER_REPAIR;
        else condition = NEEDS_REPAIR;
    }

    public String displayFacility() {
        return name + " (" + type + ", " + location + ") - " + condition;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", facilityId);
        m.put("name", name);
        m.put("location", location);
        m.put("type", type);
        m.put("condition", condition);
        m.put("openRequests", openRequests);
        return m;
    }

    public static Facility fromMap(Map<String, Object> m) {
        return new Facility((int) Json.num(m, "id", 0), Json.str(m, "name"), Json.str(m, "location"), Json.str(m, "type"));
    }

    public int getFacilityId() { return facilityId; }
    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new InvalidInputException("Facility name is required.");
        if (name.length() > 60) throw new InvalidInputException("Facility name is too long.");
        this.name = name.trim();
    }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location == null ? "" : location.trim(); }
    public String getType() { return type; }
    public void setType(String type) { this.type = type == null || type.isBlank() ? "Other" : type.trim(); }
    public String getCondition() { return condition; }
}
