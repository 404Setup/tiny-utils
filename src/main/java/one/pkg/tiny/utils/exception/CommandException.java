package one.pkg.tiny.utils.exception;

/**
 * A custom exception that represents errors occurring during the execution
 * or processing of commands in the application.
 */
public class CommandException extends RuntimeException {
    public CommandException(String message) {
        super(message);
    }

    public CommandException() {
        super();
    }

    public CommandException(Exception exception) {
        super(exception);
    }
}
