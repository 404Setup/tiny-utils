package one.pkg.tinyutils.exception;

/**
 * This exception is a subclass of {@code RuntimeException} and is typically used to signal
 * scenarios where a particular version of a component, resource, or specification cannot be
 * processed or is not supported by the system.
 */
public class UnsupportedVersionException extends RuntimeException {
    public UnsupportedVersionException() {
        super();
    }

    public UnsupportedVersionException(String message) {
        super(message);
    }
}
