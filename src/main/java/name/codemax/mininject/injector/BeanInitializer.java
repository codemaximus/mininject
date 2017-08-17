package name.codemax.mininject.injector;

import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.resolvers.BeanResolver;
import name.codemax.mininject.util.TypeUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Performs dependency injection. Injects beans using {@link Inject} annotations on fields, methods and constructors.
 * Uses {@link ConfigurableBeanContainer} for resolving existing or lazy initialized beans. Also registers in container
 * initialized {@link Singleton} instances. If bean or its factory is not in bean container, creates new instance using
 * annotated or default constructor. Injectable bean can have only one annotated constructor.
 * Supports {@link Named} injection.
 *
 * @author Maksim Osipov
 */
public class BeanInitializer {
    private final ConfigurableBeanContainer beanContainer;
    private final Queue<Object> injectionQueue = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<Set<Class<?>>> resolvingTypesContainer = new ThreadLocal<>();
    private final AtomicBoolean deferredInjectionMode = new AtomicBoolean(true);
    private final CopyOnWriteArrayList<BeanResolver> beanResolvers = new CopyOnWriteArrayList<>();

    /**
     * Initializes new instance using specified application bean container.
     *
     * @param beanContainer application bean container
     */
    public BeanInitializer(ConfigurableBeanContainer beanContainer) {
        this.beanContainer = Objects.requireNonNull(beanContainer);
    }

    @SuppressWarnings("unchecked")
    private <T> Constructor<T> getInjectConstructor(Class<T> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if (0 == constructors.length) {
            return null;
        }
        Constructor<?> injectConstructor = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                if (null != injectConstructor) {
                    throw createException(
                            "Unable to instantiate bean %s due to it has more than one @Inject constructor.",
                            beanClass.getName());
                }
                injectConstructor = constructor;
            }
        }
        if (null == injectConstructor) {
            injectConstructor = Arrays.stream(constructors).filter(
                    c -> 0 == c.getParameterCount()).findFirst()
                    .orElseThrow(() -> createException(
                            "Unable to instantiate bean %s due to it has nor default neither @Inject constructor.",
                            beanClass.getName()));
        }
        return (Constructor<T>) injectConstructor;
    }

    private InjectionException createException(String message, String... arguments) {
        return new InjectionException(String.format(message, (Object[]) arguments));
    }

    private InjectionException createException(String message, Exception cause, String... arguments) {
        return new InjectionException(String.format(message, (Object[]) arguments), cause);
    }

    protected Object[] resolveParameters(Executable method) {
        Class<?>[] classes = method.getParameterTypes();
        Class<?>[] types = method.getParameterTypes();
        Annotation[][] annotations = method.getParameterAnnotations();
        Object[] values = new Object[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            Named named = Arrays.stream(annotations[i])
                    .filter(Named.class::isInstance).map(Named.class::cast)
                    .findFirst().orElse(null);
            values[i] = null == named ? resolveBean(types[i]) :
                    resolveBean(named.value(), classes[i]);
        }
        return values;
    }

    protected void injectField(Object object, Field field) {
        Named named = field.getAnnotation(Named.class);
        Object bean = null == named ? resolveBean(field.getGenericType()) :
                resolveBean(named.value(), field.getType());
        boolean isAccessible = field.isAccessible();
        field.setAccessible(true);
        try {
            field.set(object, bean);
        } catch (IllegalAccessException e) {
            throw createException("Unable to inject value of field %s: " + e.getMessage(), e,
                    field.getDeclaringClass().getName() + '#' + field.getName());
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    protected void invokeInjectMethod(Object object, Method method) {
        boolean isAccessible = method.isAccessible();
        method.setAccessible(true);
        try {
            method.invoke(object, resolveParameters(method));
        } catch (InvocationTargetException e) {
            throw createException("Error occurred during call @Inject method %s: " + e.getCause().getMessage(), e,
                    method.getDeclaringClass().getName() + '#' + method.getName());
        } catch (IllegalAccessException e) {
            throw createException("Unable to call @Inject method %s: " + e.getMessage(), e,
                    method.getDeclaringClass().getName() + '#' + method.getName());
        } finally {
            method.setAccessible(isAccessible);
        }
    }

    protected <T> T createBean(Class<T> beanClass) {
        if (beanClass.isInterface()) {
            throw createException("Unable to call bean constructor due to %s is interface.", beanClass.getName());
        } else if (Modifier.isAbstract(beanClass.getModifiers())) {
            throw createException("Unable to call bean constructor due to %s is abstract.", beanClass.getName());
        }
        Constructor<T> constructor = getInjectConstructor(beanClass);
        try {
            T instance;
            if (null == constructor) {
                instance = beanClass.newInstance();
            } else {
                boolean isAccessible = constructor.isAccessible();
                constructor.setAccessible(true);
                try {
                    instance = constructor.newInstance(resolveParameters(constructor));
                } finally {
                    constructor.setAccessible(isAccessible);
                }
            }
            if (beanClass.isAnnotationPresent(Singleton.class)) {
                beanContainer.register(beanClass, instance);
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw createException("Unable to instantiate bean %s: " + e.getMessage(), e, beanClass.getName());
        } catch (InvocationTargetException e) {
            throw createException("Error occurred during bean %s instantiation: " + e.getCause().getMessage(), e,
                    beanClass.getName());
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T resolveBean(String beanName, Type beanType) {
        Class<?> beanClass = TypeUtils.getRawClass(beanType);
        Object bean = beanContainer.get(beanName);
        if (!beanClass.isInstance(bean)) {
            throw createException("Unable to resolve bean %s due to there is no such beans of type %s registered.",
                    beanName, beanClass.getName());
        }
        return (T) bean;
    }

    protected <T> T resolveBean(Type beanType) {
        return resolveBean(beanType, false);
    }

    protected <T> T resolveBean(Type beanType, boolean forceCreate) {
        Class<T> beanClass = (Class<T>) TypeUtils.getRawClass(beanType);
        Set<Class<?>> resolvingTypes = resolvingTypesContainer.get();
        if (null == resolvingTypes) {
            resolvingTypes = new HashSet<>();
            resolvingTypesContainer.set(resolvingTypes);
        }
        if (!resolvingTypes.add(beanClass)) {
            throw createException(
                    "Unable to resolve bean %s due to cyclic reference on itself in dependencies.",
                    beanClass.getName());
        }
        try {
            if (!forceCreate) {
                for (BeanResolver resolver : beanResolvers) {
                    T bean = resolver.resolveBean(beanType, beanContainer);
                    if (null != bean) {
                        return bean;
                    }
                }
                T bean = beanContainer.get(beanClass);
                if (null != bean) {
                    return bean;
                }
            }
            return createBean(beanClass);
        } finally {
            resolvingTypes.remove(beanClass);
            if (resolvingTypes.isEmpty()) {
                resolvingTypesContainer.remove();
            }
        }
    }

    protected void initializeBean(Object bean) {
        Class<?> beanClass = bean.getClass();
        for (Field field : beanClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Inject.class)) {
                continue;
            }
            injectField(bean, field);
        }
        for (Method method : beanClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Inject.class)) {
                continue;
            }
            invokeInjectMethod(bean, method);
        }
    }

    /**
     * Performs injection into specified object, initializes annotated fields and calls annotated methods.
     * If deferred injection mode is disabled (enabled by default), initialization is performed immediately.
     * Otherwise, real initialization is deferred until {@link #disableDeferredInjection()} is called.
     *
     * @param bean object needs to be initialized
     */
    public void initialize(Object bean) {
        if (deferredInjectionMode.get()) {
            injectionQueue.add(bean);
        } else {
            initializeBean(bean);
        }
    }

    /**
     * Returns value indicating current state of deferred injection mode.
     *
     * @return true if deferred injection mode is enabled, false otherwise
     */
    public boolean isDeferredInjectionEnabled() {
        return deferredInjectionMode.get();
    }

    /**
     * Enables deferred injection mode.
     */
    public void enableDeferredInjection() {
        deferredInjectionMode.set(true);
    }

    /**
     * Performs all deferred injections and disables deferred injection mode. Usually called after configuration is
     * finished (all beans or its factories are registered in application bean container).
     */
    public void disableDeferredInjection() {
        for (Object bean; null != (bean = injectionQueue.poll()); ) {
            initializeBean(bean);
        }
        deferredInjectionMode.set(false);
    }

    public void addBeanResolver(BeanResolver resolver) {
        beanResolvers.add(Objects.requireNonNull(resolver));
    }
}
