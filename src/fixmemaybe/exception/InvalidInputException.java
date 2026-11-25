package fixmemaybe.exception;

/**
 * Thrown when the user types invalid information
 * (empty fields, wrong email format, short password, etc.).
 * The server catches it and shows the message instead of crashing.
 */
public class InvalidInputException extends ApiException {

    public InvalidInputException(String message) {
        super(400, message);
    }
}
