package name.codemax.mininject.injector;

/**
 * Indicates an error occurred during annotation-based dependency injection.
 *
 * @author Maksim Osipov
 */
public class InjectionException extends RuntimeException {
    public InjectionException() {
    }

    public InjectionException(String message) {
        super(message);
    }

    public InjectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InjectionException(Throwable cause) {
        super(cause);
    }
}
