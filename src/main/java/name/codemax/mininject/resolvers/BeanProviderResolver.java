package name.codemax.mininject.resolvers;

import name.codemax.mininject.container.ListableBeanContainer;
import name.codemax.mininject.util.TypeUtils;

import javax.inject.Provider;
import java.lang.reflect.Type;

/**
 * @author Maksim Osipov
 */
public class BeanProviderResolver implements BeanResolver {
    @SuppressWarnings("unchecked")
    @Override
    public <T> T resolveBean(Type type, ListableBeanContainer container) {
        if (!Provider.class.getName().equals(TypeUtils.getRawClass(type).getName()) ||
                1 != TypeUtils.getGenericParametersCount(type)) {
            return null;
        }
        final Class<?> beanClass = TypeUtils.getFirstGeneric(type);
        return (T) (Provider<Object>) () -> container.get(beanClass);
    }
}
