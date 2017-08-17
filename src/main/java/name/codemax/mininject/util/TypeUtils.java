package name.codemax.mininject.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Maksim Osipov
 */
public class TypeUtils {
    public static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<Object>) type;
        }
        if (type instanceof ParameterizedType) {
            return getRawClass(((ParameterizedType) type).getRawType());
        }
        return null;
    }

    public static Class<?> getFirstGeneric(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] parameters = ((ParameterizedType) type).getActualTypeArguments();
            if (parameters.length > 0) {
                return getRawClass(parameters[0]);
            }
        }
        return null;
    }

    public static int getGenericParametersCount(Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments().length;
        }
        return 0;
    }
}
