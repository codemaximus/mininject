package name.codemax.mininject.container;

import java.util.function.Function;

/**
 * @author Maksim Osipov
 */
public interface ConfigurableBeanContainer extends ListableBeanContainer {
    <T> void register(String name, T bean);

    default <T> void register(Class<T> type, T bean) {
        register(type.getName(), bean);
    }

    <T> void registerLazy(String name, Function<ListableBeanContainer, T> factory);

    default <T> void registerLazy(Class<T> type, Function<ListableBeanContainer, T> factory) {
        registerLazy(type.getName(), factory);
    }

    <T> void registerFactory(String name, Function<ListableBeanContainer, T> factory);

    default <T> void registerFactory(Class<T> type, Function<ListableBeanContainer, T> factory) {
        registerFactory(type.getName(), factory);
    }

    void bind(String name, String implementationName, boolean asPrimary);

    default void bind(String name, String implementationName) {
        bind(name, implementationName, false);
    }

    default <T> void bind(String name, Class<T> implementationType, boolean asPrimary) {
        bind(name, implementationType.getName(), asPrimary);
    }

    default <T> void bind(String name, Class<T> implementationType) {
        bind(name, implementationType.getName());
    }

    default <T> void bind(Class<T> type, Class<? extends T> implementationType, boolean asPrimary) {
        bind(type.getName(), implementationType.getName(), asPrimary);
    }

    default <T> void bind(Class<T> type, Class<? extends T> implementationType) {
        bind(type.getName(), implementationType.getName());
    }
}
