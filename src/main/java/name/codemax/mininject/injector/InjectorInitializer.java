package name.codemax.mininject.injector;

import name.codemax.mininject.container.ConfigurableBeanContainer;

import java.util.Objects;

/**
 * @author Maksim Osipov
 */
public class InjectorInitializer {
    private BeanInjector injector;

    public InjectorInitializer(ConfigurableBeanContainer beanContainer) {
        injector = new BeanInjector(Objects.requireNonNull(beanContainer));
        InjectorConfiguration defaultConfiguration = new DefaultConfiguration();
        defaultConfiguration.configure(injector);
    }

    public InjectorInitializer(BeanInjector injector) {
        this.injector = Objects.requireNonNull(injector);
    }

    /**
     * Applies specified dependency injection container configurations.
     *
     * @param configurations dependency injection configuration modules
     */
    public void configure(InjectorConfiguration... configurations) {
        for (InjectorConfiguration config : configurations) {
            config.configure(injector);
        }
    }

    /**
     * Performs initial dependency injection according to applied configurations.
     */
    public void perform() {
        injector.perform();
    }
}
