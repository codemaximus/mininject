package name.codemax.mininject.resolvers;

import name.codemax.mininject.container.ListableBeanContainer;

import java.lang.reflect.Type;

/**
 * @author Maksim Osipov
 */
public interface BeanResolver {
    <T> T resolveBean(Type type, ListableBeanContainer container);
}
