package fixmemaybe.model;

import java.util.Map;

/**
 * INHERITANCE: Maintenance staff / admin IS a User.
 * Any account whose email is listed in config/app.properties
 * (admin.emails) becomes a MaintenanceStaff and gets the admin dashboard.
 */
public class MaintenanceStaff extends User {

    public static final String ROLE = "Maintenance Staff";

    private String staffId;
    private String assignedArea;

    public MaintenanceStaff(String id, String fullName, String username, String email,
                            String passwordHash, String staffId, String assignedArea) {
        super(id, fullName, username, email, passwordHash);
        this.staffId = staffId;
        this.assignedArea = assignedArea;
    }

    @Override public String getRole() { return ROLE; }
    @Override public boolean isAdmin() { return true; }
    @Override public String getDashboardPage() { return "admin.html"; }

    @Override
    protected void writeExtraInfo(Map<String, Object> map) {
        map.put("idNumber", staffId);
        map.put("idLabel", "Staff ID");
        map.put("group", assignedArea);
        map.put("groupLabel", "Assigned Area");
    }

    @Override
    public String displayInfo() {
        return super.displayInfo() + " | Staff ID: " + staffId + " | Area: " + assignedArea;
    }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public String getAssignedArea() { return assignedArea; }
    public void setAssignedArea(String assignedArea) { this.assignedArea = assignedArea; }
}
