package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.ListableBeanContainer;

import java.util.function.Function;

/**
 * @author Maksim Osipov
 */
class LazyBeanDefinition<T> implements BeanDefinition<T> {
    private final Function<ListableBeanContainer, T> factory;
    private T bean = null;

    public LazyBeanDefinition(Function<ListableBeanContainer, T> factory) {
        this.factory = factory;
    }

    @Override
    public T getBean(ListableBeanContainer container) {
        if (null != bean) {
            return bean;
        }
        synchronized (factory) {
            if (null != bean) {
                return bean;
            }
            bean = factory.apply(container);
            return bean;
        }
    }
}
