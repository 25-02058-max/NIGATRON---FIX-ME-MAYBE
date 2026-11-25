package fixmemaybe.model;

import java.util.Map;

/**
 * INHERITANCE: a Faculty member IS a User.
 * Faculty can report classroom problems and monitor their requests.
 */
public class Faculty extends User {

    public static final String ROLE = "Faculty";

    private String employeeId;
    private String department;

    public Faculty(String id, String fullName, String username, String email,
                   String passwordHash, String employeeId, String department) {
        super(id, fullName, username, email, passwordHash);
        this.employeeId = employeeId;
        this.department = department;
    }

    @Override public String getRole() { return ROLE; }
    @Override public boolean isAdmin() { return false; }
    @Override public String getDashboardPage() { return "user.html"; }

    @Override
    protected void writeExtraInfo(Map<String, Object> map) {
        map.put("idNumber", employeeId);
        map.put("idLabel", "Employee ID");
        map.put("group", department);
        map.put("groupLabel", "Department");
    }

    @Override
    public String displayInfo() {
        return super.displayInfo() + " | Employee ID: " + employeeId + " | Dept: " + department;
    }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
