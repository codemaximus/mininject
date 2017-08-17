package name.codemax.mininject.resolvers;

import name.codemax.mininject.container.ListableBeanContainer;
import name.codemax.mininject.util.TypeUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Maksim Osipov
 */
public class BeanListResolver implements BeanResolver {
    @SuppressWarnings("unchecked")
    @Override
    public <T> T resolveBean(Type type, ListableBeanContainer container) {
        if (!List.class.getName().equals(TypeUtils.getRawClass(type).getName()) ||
                1 != TypeUtils.getGenericParametersCount(type)) {
            return null;
        }
        return (T) container.list(TypeUtils.getFirstGeneric(type));
    }
}
