package lonelight.checks.concurrent;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证传统线程池工厂检查的调用方式与作用域
 *
 * @author lonelight
 */
class ExecutorFactoryCheckTest extends CustomCheckTest {
    /**
     * 验证全部目标工厂及虚拟线程、普通工具方法的区别
     */
    @Test
    void scan_executorFactories_reportsCallsAndReferences() throws Exception {
        verify(ExecutorFactoryCheck.class, """
                import java.util.concurrent.Executors;
                import java.util.function.IntFunction;
                import static java.util.concurrent.Executors.newCachedThreadPool;
                class Example {
                    void factories() {
                        Executors.newFixedThreadPool(1);
                        newCachedThreadPool();
                        java.util.concurrent.Executors.newSingleThreadExecutor();
                        Executors.newScheduledThreadPool(1);
                        Executors.newSingleThreadScheduledExecutor();
                        IntFunction<?> factory = Executors::newFixedThreadPool;
                        Executors.newVirtualThreadPerTaskExecutor();
                        Executors.newWorkStealingPool();
                        Executors.defaultThreadFactory();
                    }
                }
                """, 6);
    }

    /**
     * 验证静态通配导入与本地方法和变量的遮蔽
     */
    @Test
    void scan_staticFactoriesAndLocalMethods_reportsJdkOnly() throws Exception {
        verify(ExecutorFactoryCheck.class, """
                import java.util.concurrent.Executors;
                import static java.util.concurrent.Executors.*;
                class Example {
                    void factories(Custom Executors) {
                        Executors.newFixedThreadPool(1);
                        newFixedThreadPool(1);
                        newSingleThreadExecutor();
                    }
                    void newSingleThreadExecutor() {}
                    static class Custom {
                        void newFixedThreadPool(int count) {}
                    }
                }
                """, 1);
        verify(ExecutorFactoryCheck.class, """
                import app.Executors;
                class Example {
                    void factories() { Executors.newFixedThreadPool(1); }
                }
                """, 0);
        verify(ExecutorFactoryCheck.class, """
                import static app.Other.newFixedThreadPool;
                import static java.util.concurrent.Executors.*;
                class Example {
                    void factories() { newFixedThreadPool(1); }
                }
                """, 0);
        verify(ExecutorFactoryCheck.class, """
                import java.util.concurrent.Executors;
                class Example {
                    void factories(Object candidate) {
                        if (candidate instanceof Custom Executors) {
                            Executors.newFixedThreadPool(1);
                        }
                    }
                    static class Custom {
                        void newFixedThreadPool(int count) {}
                    }
                }
                """, 0);
    }
}
