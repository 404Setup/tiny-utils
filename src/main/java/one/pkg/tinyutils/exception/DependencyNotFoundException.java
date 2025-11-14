package one.pkg.tinyutils.exception;

/**
 * This exception is a subclass of {@code RuntimeException} and is typically used to signal
 * scenarios where the required dependency for an operation or process cannot be found or resolved.
 */
public class DependencyNotFoundException extends RuntimeException {
    public DependencyNotFoundException() {
        super();
    }

    public DependencyNotFoundException(String message) {
        super(message);
    }
}
