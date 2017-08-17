package name.codemax.mininject.injector;

import name.codemax.mininject.resolvers.BeanListResolver;
import name.codemax.mininject.resolvers.BeanProviderResolver;

/**
 * Default injector configuration.
 *
 * @author Maksim Osipov
 */
public class DefaultConfiguration implements InjectorConfiguration {
    @Override
    public void configure(BeanInjector injector) {
        injector.addBeanResolver(new BeanListResolver());
        injector.addBeanResolver(new BeanProviderResolver());
    }
}
