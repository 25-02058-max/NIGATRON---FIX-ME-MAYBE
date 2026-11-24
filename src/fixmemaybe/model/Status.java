package fixmemaybe.model;

import fixmemaybe.exception.InvalidInputException;

/** The 3 stages of a repair request: Reported -> In Progress -> Fixed. */
public enum Status {
    REPORTED("Reported"),
    IN_PROGRESS("In Progress"),
    FIXED("Fixed");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Accepts "Reported", "In Progress", "IN_PROGRESS", "in-progress" ... */
    public static Status fromLabel(String text) {
        if (text != null) {
            String t = text.trim().replace('-', ' ').replace('_', ' ');
            for (Status s : values()) {
                if (s.label.equalsIgnoreCase(t)) return s;
            }
        }
        throw new InvalidInputException("Unknown status: " + text);
    }
}
