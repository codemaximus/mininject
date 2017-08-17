package name.codemax.mininject.injector;

import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.container.impl.BeanContainerImpl;
import name.codemax.mininject.resolvers.BeanListResolver;
import name.codemax.mininject.resolvers.BeanProviderResolver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

/**
 * @author Maksim Osipov
 */
public class DependencyInjectionTest {
    private ConfigurableBeanContainer beanContainer;
    private BeanInjector beanInjector;

    private static class SelfConstructorInjectedBean {
        @Inject
        public SelfConstructorInjectedBean(SelfConstructorInjectedBean bean) {
        }
    }

    private static class SelfFieldInjectedBean {
        @Inject
        private SelfFieldInjectedBean bean;
    }

    @Singleton
    private static class SelfFieldInjectedSingleton {
        @Inject
        private SelfFieldInjectedSingleton bean;
    }

    private static class SelfMethodInjectedBean {
        @Inject
        public void init(SelfMethodInjectedBean bean) {
        }
    }

    @Singleton
    private static class SelfMethodInjectedSingleton {
        private SelfMethodInjectedSingleton bean;

        @Inject
        public void init(SelfMethodInjectedSingleton bean) {
            this.bean = bean;
        }
    }

    private interface InstanceCounter {
    }

    private static class InstanceCounterImpl implements InstanceCounter {
        private static int count = 0;

        public InstanceCounterImpl() {
            ++count;
        }
    }

    @Singleton
    private static class InstanceCounterSingleton implements InstanceCounter {
        private static int count = 0;

        public InstanceCounterSingleton() {
            ++count;
        }
    }

    private interface CounterContainer {
    }

    private static class CounterContainerImpl implements CounterContainer {
        @Inject
        private InstanceCounter counter1;
        @Inject
        private InstanceCounter counter2;
    }

    @Singleton
    private static class CounterContainerSingleton implements CounterContainer {
        @Inject
        private InstanceCounter counter1;
        @Inject
        private InstanceCounter counter2;
    }

    private static class NamedCounterContainer {
        @Inject
        @Named("instanceCounter")
        private InstanceCounter counter;
    }

    private static class NamedCounterContainerWithConstructor {
        private InstanceCounter namedInstanceCounter;
        private InstanceCounter defaultInstanceCounter;

        @Inject
        public NamedCounterContainerWithConstructor(
                @Named("instanceCounter") InstanceCounter namedInstanceCounter,
                InstanceCounter defaultInstanceCounter) {
            this.namedInstanceCounter = namedInstanceCounter;
            this.defaultInstanceCounter = defaultInstanceCounter;
        }
    }

    private static class IncorrectNamedBeanContainer {
        @Inject
        @Named("instanceCounter")
        private CounterContainer counter;
    }

    private interface TestComponentInterface {
        void perform();
    }

    private static class TestComponentImpl implements TestComponentInterface {
        @Override
        public void perform() {
        }
    }

    @Singleton
    private static class TestComponentSingleton implements TestComponentInterface {
        @Override
        public void perform() {
        }
    }

    private static class TestComponentList {
        @Inject
        private List<TestComponentInterface> componentList;
    }

    private static class TestComponentProvider {
        @Inject
        private Provider<TestComponentInterface> componentProvider;
    }

    @Before
    public void setUp() {
        beanContainer = new BeanContainerImpl();
        beanInjector = new BeanInjector(beanContainer);
    }

    @After
    public void tearDown() {
        beanInjector = null;
        beanContainer = null;
    }

    @Test
    public void testSelfInjectionInConstructor() {
        try {
            beanInjector.bind(SelfConstructorInjectedBean.class);
            beanInjector.perform();
            beanContainer.get(SelfConstructorInjectedBean.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to resolve bean " + SelfConstructorInjectedBean.class.getName() +
                    " due to cyclic reference on itself in dependencies.", e.getMessage());
        }
    }

    @Test
    public void testSelfInjectionInField() {
        try {
            beanInjector.bind(SelfFieldInjectedBean.class);
            beanInjector.perform();
            beanContainer.get(SelfFieldInjectedBean.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to resolve bean " + SelfFieldInjectedBean.class.getName() +
                    " due to cyclic reference on itself in dependencies.", e.getMessage());
        }
    }

    @Test
    public void testSingletonSelfInjectionInField() {
        beanInjector.bind(SelfFieldInjectedSingleton.class);
        beanInjector.perform();
        SelfFieldInjectedSingleton bean = beanContainer.get(SelfFieldInjectedSingleton.class);
        Assert.assertEquals(bean, bean.bean);
    }

    @Test
    public void testSelfInjectionInMethod() {
        try {
            beanInjector.bind(SelfMethodInjectedBean.class);
            beanInjector.perform();
            beanContainer.get(SelfMethodInjectedBean.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to resolve bean " + SelfMethodInjectedBean.class.getName() +
                    " due to cyclic reference on itself in dependencies.", e.getMessage());
        }
    }

    @Test
    public void testSingletonSelfInjectionInMethod() {
        beanInjector.bind(SelfMethodInjectedSingleton.class);
        beanInjector.perform();
        SelfMethodInjectedSingleton bean = beanContainer.get(SelfMethodInjectedSingleton.class);
        Assert.assertEquals(bean, bean.bean);
    }

    @Test
    public void testInjectSingletonIntoSingleton() {
        beanInjector.bind(InstanceCounter.class, InstanceCounterSingleton.class);
        beanInjector.bind(CounterContainer.class, CounterContainerSingleton.class);
        InstanceCounterSingleton.count = 0;
        beanInjector.perform();

        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
    }

    @Test
    public void testInjectSingletonIntoInstantiable() {
        beanInjector.bind(InstanceCounter.class, InstanceCounterSingleton.class);
        beanInjector.bind(CounterContainer.class, CounterContainerImpl.class);
        InstanceCounterSingleton.count = 0;
        beanInjector.perform();

        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(1, InstanceCounterSingleton.count);
    }

    @Test
    public void testInjectInstantiableIntoSingleton() {
        beanInjector.bind(InstanceCounter.class, InstanceCounterImpl.class);
        beanInjector.bind(CounterContainer.class, CounterContainerSingleton.class);
        InstanceCounterImpl.count = 0;
        beanInjector.perform();

        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(2, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(2, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(2, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(2, InstanceCounterImpl.count);
    }

    @Test
    public void testInjectInstantiableIntoInstantiable() {
        beanInjector.bind(InstanceCounter.class, InstanceCounterImpl.class);
        beanInjector.bind(CounterContainer.class, CounterContainerImpl.class);
        InstanceCounterImpl.count = 0;
        beanInjector.perform();

        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(2, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(4, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(6, InstanceCounterImpl.count);
        beanContainer.get(CounterContainer.class);
        Assert.assertEquals(8, InstanceCounterImpl.count);
    }

    @Test
    public void testNamedInject() {
        beanInjector.bind("instanceCounter", InstanceCounterImpl.class);
        beanInjector.bind(NamedCounterContainer.class);
        beanInjector.perform();

        NamedCounterContainer bean = beanContainer.get(NamedCounterContainer.class);
        Assert.assertTrue(bean.counter instanceof InstanceCounterImpl);
    }

    @Test
    public void testConstructorNamedInject() {
        beanInjector.bind("instanceCounter", InstanceCounterImpl.class);
        beanInjector.bind(InstanceCounter.class, InstanceCounterSingleton.class);
        beanInjector.bind(NamedCounterContainerWithConstructor.class);
        beanInjector.perform();

        NamedCounterContainerWithConstructor bean = beanContainer.get(NamedCounterContainerWithConstructor.class);
        Assert.assertTrue(bean.defaultInstanceCounter instanceof InstanceCounterSingleton);
        Assert.assertTrue(bean.namedInstanceCounter instanceof InstanceCounterImpl);
    }

    @Test
    public void testIncorrectNamedInject() {
        try {
            beanInjector.bind("instanceCounter", InstanceCounterImpl.class);
            beanInjector.bind(IncorrectNamedBeanContainer.class);
            beanInjector.perform();

            beanContainer.get(IncorrectNamedBeanContainer.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to resolve bean instanceCounter due to there is no such beans of type " +
                    CounterContainer.class.getName() + " registered.", e.getMessage());
        }
    }

    @Test
    public void testListBinding() {
        beanInjector.addBeanResolver(new BeanListResolver());
        beanInjector.bind(TestComponentInterface.class, TestComponentImpl.class);
        beanInjector.bind(TestComponentInterface.class, TestComponentSingleton.class);
        beanInjector.bind(TestComponentList.class);
        beanInjector.perform();

        TestComponentList list1 = beanContainer.get(TestComponentList.class);
        TestComponentList list2 = beanContainer.get(TestComponentList.class);

        Assert.assertEquals(2, list1.componentList.size());
        Assert.assertEquals(2, list2.componentList.size());
        Assert.assertTrue(list1.componentList.get(0) != list2.componentList.get(0));
        Assert.assertTrue(list1.componentList.get(1) == list2.componentList.get(1));
    }

    @Test
    public void testProviderBinding() {
        beanInjector.addBeanResolver(new BeanProviderResolver());
        beanInjector.bind(TestComponentInterface.class, TestComponentImpl.class);
        beanInjector.bind(TestComponentProvider.class);
        beanInjector.perform();

        TestComponentProvider provider1 = beanContainer.get(TestComponentProvider.class);
        TestComponentProvider provider2 = beanContainer.get(TestComponentProvider.class);
        Assert.assertTrue(provider1.componentProvider.get() != provider2.componentProvider.get());
    }

    @Test
    public void testProviderBindingSingleton() {
        beanInjector.addBeanResolver(new BeanProviderResolver());
        beanInjector.bind(TestComponentInterface.class, TestComponentSingleton.class);
        beanInjector.bind(TestComponentProvider.class);
        beanInjector.perform();

        TestComponentProvider provider1 = beanContainer.get(TestComponentProvider.class);
        TestComponentProvider provider2 = beanContainer.get(TestComponentProvider.class);
        Assert.assertTrue(provider1.componentProvider.get() == provider2.componentProvider.get());
    }
}
