package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.ListableBeanContainer;

import java.util.Objects;

/**
 * @author Maksim Osipov
 */
class StoredBeanDefinition<T> implements BeanDefinition<T> {
    private final T bean;

    public StoredBeanDefinition(T bean) {
        this.bean = Objects.requireNonNull(bean);
    }

    @Override
    public T getBean(ListableBeanContainer container) {
        return bean;
    }
}
