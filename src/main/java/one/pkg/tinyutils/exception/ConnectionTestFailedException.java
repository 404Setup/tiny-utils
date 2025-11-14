package one.pkg.tinyutils.exception;

public class ConnectionTestFailedException extends RuntimeException {
    public ConnectionTestFailedException() {
        super();
    }

    public ConnectionTestFailedException(String message) {
        super(message);
    }

    public ConnectionTestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
