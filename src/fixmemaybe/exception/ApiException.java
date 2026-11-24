package fixmemaybe.exception;

/**
 * Base exception for errors that should be shown to the user.
 * Carries an HTTP status code (400, 401, 403, 404 ...).
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException unauthorized() {
        return new ApiException(401, "Please log in first.");
    }

    public static ApiException forbidden() {
        return new ApiException(403, "You are not allowed to do that.");
    }

    public static ApiException notFound(String what) {
        return new ApiException(404, what + " not found.");
    }
}
