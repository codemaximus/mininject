package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.ListableBeanContainer;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author Maksim Osipov
 */
class FactoryBeanDefinition<T> implements BeanDefinition<T> {
    private final Function<ListableBeanContainer, T> factory;

    public FactoryBeanDefinition(Function<ListableBeanContainer, T> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    @Override
    public T getBean(ListableBeanContainer container) {
        return factory.apply(container);
    }
}
