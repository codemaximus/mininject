package name.codemax.mininject.injector;

import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.container.impl.BeanContainerImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Maksim Osipov
 */
public class BeanInitializerTest {
    private ConfigurableBeanContainer beanContainer;
    private BeanInitializer initializer;

    private static class InjectableBean {
    }

    private static class TestBean {
        private InjectableBean field;
        @Inject
        private InjectableBean annotatedField;
        private InjectableBean init;
        private InjectableBean initAnnotated;

        public void init(InjectableBean value) {
            init = value;
        }

        @Inject
        public void initAnnotated(InjectableBean value) {
            initAnnotated = value;
        }
    }

    private static class BeanWithInjectConstructor {
        private InjectableBean injectable;

        @Inject
        public BeanWithInjectConstructor(InjectableBean bean) {
            injectable = bean;
        }
    }

    private static class BeanWithTwoInjectConstructors {
        @Inject
        public BeanWithTwoInjectConstructors(InjectableBean bean) {
        }

        @Inject
        public BeanWithTwoInjectConstructors(InjectableBean bean1, InjectableBean bean2) {
        }
    }

    private static class BeanWithDefaultConstructor {
        private int field = 5;

        public BeanWithDefaultConstructor() {
            field = 42;
        }

        public BeanWithDefaultConstructor(int value) {
            field = value;
        }
    }

    private static class BeanWithNoDefaultConstructor {
        public BeanWithNoDefaultConstructor(int value) {
        }
    }

    private interface TestInterface {
    }

    private static class BeanWithExceptionInConstructor {
        public BeanWithExceptionInConstructor() throws Exception {
            throw new Exception("Error in constructor.");
        }
    }

    private static class BeanWithExceptionInInjectMethod {
        @Inject
        public void test() throws Exception {
            throw new Exception("Error in @Inject method.");
        }
    }

    @Before
    public void setUp() {
        beanContainer = new BeanContainerImpl();
        initializer = new BeanInitializer(beanContainer);
    }

    @After
    public void tearDown() {
        initializer = null;
        beanContainer = null;
    }

    @Test
    public void testInjectField() throws NoSuchFieldException {
        TestBean bean = new TestBean();
        Field field = TestBean.class.getDeclaredField("field");
        Assert.assertNull(bean.field);
        initializer.injectField(bean, field);
        Assert.assertNotNull(bean.field);
        Field annotatedField = TestBean.class.getDeclaredField("annotatedField");
        Assert.assertNull(bean.annotatedField);
        initializer.injectField(bean, annotatedField);
        Assert.assertNotNull(bean.annotatedField);
    }

    @Test
    public void testInvokeInjectMethod() throws NoSuchMethodException {
        TestBean bean = new TestBean();
        Method method = TestBean.class.getDeclaredMethod("init", InjectableBean.class);
        Assert.assertNull(bean.init);
        initializer.invokeInjectMethod(bean, method);
        Assert.assertNotNull(bean.init);
        Method annotatedMethod = TestBean.class.getDeclaredMethod("initAnnotated", InjectableBean.class);
        Assert.assertNull(bean.initAnnotated);
        initializer.invokeInjectMethod(bean, annotatedMethod);
        Assert.assertNotNull(bean.initAnnotated);
    }

    @Test
    public void testInjectBean() {
        TestBean bean = new TestBean();
        Assert.assertNull(bean.field);
        Assert.assertNull(bean.annotatedField);
        Assert.assertNull(bean.init);
        Assert.assertNull(bean.initAnnotated);
        initializer.initializeBean(bean);
        Assert.assertNull(bean.field);
        Assert.assertNotNull(bean.annotatedField);
        Assert.assertNull(bean.init);
        Assert.assertNotNull(bean.initAnnotated);
    }

    @Test
    public void testCreateBeanWithInjectConstructor() {
        BeanWithInjectConstructor bean = initializer.createBean(BeanWithInjectConstructor.class);
        Assert.assertNotNull(bean.injectable);
    }

    @Test
    public void testCreateBeanWithDefaultConstructor() {
        BeanWithDefaultConstructor bean = initializer.createBean(BeanWithDefaultConstructor.class);
        Assert.assertEquals(42, bean.field);
    }

    @Test
    public void testInjectMode() {
        TestBean bean1 = new TestBean();
        TestBean bean2 = new TestBean();
        initializer.initialize(bean1);
        Assert.assertNull(bean1.field);
        Assert.assertNull(bean1.annotatedField);
        Assert.assertNull(bean1.init);
        Assert.assertNull(bean1.initAnnotated);
        initializer.disableDeferredInjection();
        Assert.assertNull(bean1.field);
        Assert.assertNotNull(bean1.annotatedField);
        Assert.assertNull(bean1.init);
        Assert.assertNotNull(bean1.initAnnotated);
        initializer.initialize(bean2);
        Assert.assertNull(bean2.field);
        Assert.assertNotNull(bean2.annotatedField);
        Assert.assertNull(bean2.init);
        Assert.assertNotNull(bean2.initAnnotated);
    }

    @Test
    public void testFailCreateBeanWithNoDefaultConstructor() {
        try {
            initializer.createBean(BeanWithNoDefaultConstructor.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to instantiate bean " + BeanWithNoDefaultConstructor.class.getName() +
                    " due to it has nor default neither @Inject constructor.", e.getMessage());
        }
    }

    @Test
    public void testFailCreateBeanWithTwoInjectConstructors() {
        try {
            initializer.createBean(BeanWithTwoInjectConstructors.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to instantiate bean " + BeanWithTwoInjectConstructors.class.getName() +
                    " due to it has more than one @Inject constructor.", e.getMessage());
        }
    }

    @Test
    public void testFailInterfaceInstantiation() {
        try {
            initializer.createBean(TestInterface.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Unable to call bean constructor due to " + TestInterface.class.getName() +
                    " is interface.", e.getMessage());
        }
    }

    @Test
    public void testExceptionInConstructor() {
        try {
            initializer.createBean(BeanWithExceptionInConstructor.class);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Error occurred during bean " + BeanWithExceptionInConstructor.class.getName() +
                    " instantiation: Error in constructor.", e.getMessage());
        }
    }

    @Test
    public void testExceptionInInjectMethod() {
        try {
            BeanWithExceptionInInjectMethod bean = initializer.createBean(BeanWithExceptionInInjectMethod.class);
            initializer.initializeBean(bean);
            Assert.fail();
        } catch (InjectionException e) {
            Assert.assertEquals("Error occurred during call @Inject method " +
                            BeanWithExceptionInInjectMethod.class.getName() + "#test: Error in @Inject method.",
                    e.getMessage());
        }
    }
}
