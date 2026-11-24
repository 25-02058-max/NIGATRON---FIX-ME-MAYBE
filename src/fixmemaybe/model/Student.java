package fixmemaybe.model;

import java.util.Map;

/**
 * INHERITANCE: a Student IS a User.
 * Students can submit repair requests and view their own requests.
 */
public class Student extends User {

    public static final String ROLE = "Student";

    private String studentId;   // SR-Code
    private String section;     // course / section, e.g. IT-2107

    public Student(String id, String fullName, String username, String email,
                   String passwordHash, String studentId, String section) {
        super(id, fullName, username, email, passwordHash);
        this.studentId = studentId;
        this.section = section;
    }

    @Override public String getRole() { return ROLE; }
    @Override public boolean isAdmin() { return false; }
    @Override public String getDashboardPage() { return "user.html"; }

    @Override
    protected void writeExtraInfo(Map<String, Object> map) {
        map.put("idNumber", studentId);
        map.put("idLabel", "SR-Code");
        map.put("group", section);
        map.put("groupLabel", "Section");
    }

    @Override
    public String displayInfo() {
        return super.displayInfo() + " | SR-Code: " + studentId + " | Section: " + section;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
