package name.codemax.mininject.container;

/**
 * Basic bean container interface.
 *
 * @author Maksim Osipov
 */
public interface BeanContainer {
    <T> T get(String name);

    default <T> T get(Class<T> type) {
        return get(type.getName());
    }
}
