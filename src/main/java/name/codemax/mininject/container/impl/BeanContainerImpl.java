package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.BeanContainer;
import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.container.ListableBeanContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BeanContainerImpl implements ConfigurableBeanContainer {
    private static class Binding {
        private List<String> beanNames;
        private String primaryBeanName;

        private Binding() {
            this(new ArrayList<>(), null);
        }

        private Binding(List<String> beanNames, String primaryBeanName) {
            this.beanNames = new ArrayList<>(beanNames);
            this.primaryBeanName = primaryBeanName;
        }
    }

    private final ConcurrentHashMap<String, BeanDefinition<?>> beanDefinitions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Binding> bindings = new ConcurrentHashMap<>();

    public BeanContainerImpl() {
        register(BeanContainerImpl.class, this);
        bind(BeanContainer.class, BeanContainerImpl.class);
        bind(ListableBeanContainer.class, BeanContainerImpl.class);
        bind(ConfigurableBeanContainer.class, BeanContainerImpl.class);
    }

    public <T> void register(T bean) {
        register(bean.getClass().getName(), bean);
    }

    @Override
    public <T> void register(String name, T bean) {
        beanDefinitions.put(name, new StoredBeanDefinition<>(bean));
    }

    @Override
    public <T> void registerLazy(String name, Function<ListableBeanContainer, T> factory) {
        beanDefinitions.put(name, new LazyBeanDefinition<>(factory));
    }

    @Override
    public <T> void registerFactory(String name, Function<ListableBeanContainer, T> factory) {
        beanDefinitions.put(name, new FactoryBeanDefinition<>(factory));
    }

    @Override
    public void bind(String name, String implementationName, boolean asPrimary) {
        if (Objects.equals(name, implementationName)) {
            return;
        }
        Binding oldBinding = bindings.get(name);
        Binding binding = null == oldBinding
                ? new Binding()
                : new Binding(oldBinding.beanNames, oldBinding.primaryBeanName);
        if (asPrimary) {
            binding.primaryBeanName = implementationName;
        }
        if (!binding.beanNames.contains(implementationName)) {
            binding.beanNames.add(implementationName);
        }
        bindings.put(name, binding);
    }

    @Override
    public <T> T get(String name) {
        T bean = getSingleBean(name);
        if (null != bean) {
            return bean;
        }
        Binding binding = bindings.get(name);
        if (null != binding) {
            if (null != binding.primaryBeanName) {
                return getSingleBean(binding.primaryBeanName);
            }
            if (1 == binding.beanNames.size()) {
                return getSingleBean(binding.beanNames.iterator().next());
            }
            throw new IllegalStateException("There are more than one implementation registered for " + name + '.');
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSingleBean(String name) {
        BeanDefinition<?> beanDefinition = beanDefinitions.get(name);
        if (null != beanDefinition) {
            return (T) beanDefinition.getBean(this);
        }
        return null;
    }

    @Override
    public <T> List<T> list(String name) {
        List<T> beans = new ArrayList<>();
        T single = getSingleBean(name);
        if (null != single) {
            beans.add(single);
        } else {
            Binding binding = bindings.get(name);
            if (null != binding) {
                binding.beanNames.stream()
                        .map(this::<T>getSingleBean)
                        .filter(Objects::nonNull)
                        .forEachOrdered(beans::add);
            }
        }
        return beans;
    }
}
