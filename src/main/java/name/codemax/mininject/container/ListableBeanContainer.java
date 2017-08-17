package name.codemax.mininject.container;

import java.util.List;

/**
 * @author Maksim Osipov
 */
public interface ListableBeanContainer extends BeanContainer {
    <T> List<T> list(String name);

    default <T> List<T> list(Class<T> type) {
        return list(type.getName());
    }
}
