package name.codemax.mininject.injector;

import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.container.ListableBeanContainer;
import name.codemax.mininject.resolvers.BeanResolver;

import javax.inject.Singleton;
import java.util.Objects;
import java.util.function.Function;

/**
 * Provides configuration API for bean initializer and container.
 *
 * @author Maksim Osipov
 */
public class BeanInjector {
    private final BeanInitializer initializer;
    private final ConfigurableBeanContainer beanContainer;

    /**
     * Initializes new instance using specified application bean container, which will be used for configuration.
     *
     * @param beanContainer application bean container
     */
    public BeanInjector(ConfigurableBeanContainer beanContainer) {
        this.beanContainer = Objects.requireNonNull(beanContainer);
        initializer = new BeanInitializer(beanContainer);
    }

    private <T> Function<ListableBeanContainer, T> createFactory(Class<T> implClass) {
        return ctx -> {
            T bean = initializer.resolveBean(implClass, true);
            initializer.initialize(bean);
            return bean;
        };
    }

    /**
     * Creates and registers factory in bean container. This factory is associated with given abstraction.
     * It will be used to instantiate specified implementation where abstraction is injected.
     *
     * @param abstraction    base class or interface
     * @param implementation implementation of given abstraction
     * @param <T>            abstraction type
     */
    public <T> void bind(Class<T> abstraction, Class<? extends T> implementation) {
        if (implementation.isAnnotationPresent(Singleton.class)) {
            beanContainer.registerLazy(implementation.getName(), createFactory(implementation));
        } else {
            beanContainer.registerFactory(implementation.getName(), createFactory(implementation));
        }
        beanContainer.bind(abstraction, implementation);
    }

    /**
     * Creates and registers factory in bean container. This factory is associated with given name.
     * It will be used to instantiate specified implementation where named bean is injected.
     *
     * @param name           bean name
     * @param implementation implementation class
     * @param <T>            bean type
     */
    public <T> void bind(String name, Class<? extends T> implementation) {
        if (implementation.isAnnotationPresent(Singleton.class)) {
            beanContainer.registerLazy(implementation.getName(), createFactory(implementation));
        } else {
            beanContainer.registerFactory(implementation.getName(), createFactory(implementation));
        }
        beanContainer.bind(name, implementation);
    }

    /**
     * Creates and registers factory in bean container. It will be used to instantiate bean of implementation type.
     *
     * @param implementation bean implementation class
     * @param <T>            bean type
     */
    public <T> void bind(Class<T> implementation) {
        bind(implementation, implementation);
    }

    /**
     * Performs all deferred injections and disabled deferred injection mode. Usually called after configuration is
     * finished (all beans or its factories are registered in application bean container).
     */
    public void perform() {
        initializer.disableDeferredInjection();
    }

    public void addBeanResolver(BeanResolver resolver) {
        initializer.addBeanResolver(resolver);
    }
}
