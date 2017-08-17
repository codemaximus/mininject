package name.codemax.mininject.container.impl;

import name.codemax.mininject.container.BeanContainer;
import name.codemax.mininject.container.ConfigurableBeanContainer;
import name.codemax.mininject.container.ListableBeanContainer;
import org.junit.Assert;
import org.junit.Test;

public class BeanContainerImplTest {
    private interface SimpleInterface {
        String doSomething();

        int getNumber();

        void setNumber(int num);
    }

    private interface Combiner {
        String combine();
    }

    private static class SimpleInterfaceImpl implements SimpleInterface {
        private int number = 0;

        public SimpleInterfaceImpl() {
        }

        public SimpleInterfaceImpl(int num) {
            number = num;
        }

        @Override
        public String doSomething() {
            return "hello";
        }

        @Override
        public int getNumber() {
            return number;
        }

        @Override
        public void setNumber(int num) {
            number = num;
        }
    }

    private static class CombinerImpl implements Combiner {
        private final SimpleInterface obj;

        public CombinerImpl(SimpleInterface obj) {
            this.obj = obj;
        }

        @Override
        public String combine() {
            return obj.doSomething() + " " + obj.getNumber();
        }
    }

    @Test
    public void testSimpleClass() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        beanContainer.register(new SimpleInterfaceImpl());
        SimpleInterfaceImpl impl = beanContainer.get(SimpleInterfaceImpl.class);
        Assert.assertEquals("hello", impl.doSomething());
    }

    @Test
    public void testSimpleInterface() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        beanContainer.register(SimpleInterface.class, new SimpleInterfaceImpl());
        SimpleInterface impl = beanContainer.get(SimpleInterface.class);
        Assert.assertEquals("hello", impl.doSomething());
    }

    @Test
    public void testSingletonFactory() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        beanContainer.registerLazy(SimpleInterface.class, ctx -> new SimpleInterfaceImpl());
        SimpleInterface impl1 = beanContainer.get(SimpleInterface.class);
        Assert.assertEquals("hello", impl1.doSomething());
        impl1.setNumber(42);
        SimpleInterface impl2 = beanContainer.get(SimpleInterface.class);
        Assert.assertEquals(42, impl2.getNumber());
    }

    @Test
    public void testObjectFactory() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        beanContainer.registerFactory(SimpleInterface.class, ctx -> new SimpleInterfaceImpl());
        SimpleInterface impl1 = beanContainer.get(SimpleInterface.class);
        Assert.assertEquals("hello", impl1.doSomething());
        impl1.setNumber(42);
        SimpleInterface impl2 = beanContainer.get(SimpleInterface.class);
        Assert.assertEquals(0, impl2.getNumber());
    }

    @Test
    public void testLazyDependencies() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        beanContainer.registerLazy(SimpleInterface.class, ctx -> new SimpleInterfaceImpl(42));
        beanContainer.registerLazy(Combiner.class, ctx -> new CombinerImpl(ctx.get(SimpleInterface.class)));
        Combiner impl = beanContainer.get(Combiner.class);
        Assert.assertEquals("hello 42", impl.combine());
    }

    @Test
    public void testSelfRegistration() {
        BeanContainerImpl beanContainer = new BeanContainerImpl();
        Assert.assertTrue(beanContainer == beanContainer.get(BeanContainer.class));
        Assert.assertTrue(beanContainer == beanContainer.get(ListableBeanContainer.class));
        Assert.assertTrue(beanContainer == beanContainer.get(ConfigurableBeanContainer.class));
    }
}
