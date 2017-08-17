package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.ListableBeanContainer;

/**
 * @author Maksim Osipov
 */
interface BeanDefinition<T> {
    T getBean(ListableBeanContainer container);
}
