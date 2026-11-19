// 检查：ExecutorFactoryCheck, BigDecimalFloatingConstructorCheck
package base;

import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 对照线程池工厂和十进制数构造方式
 *
 * @author lonelight
 */
class RuntimeApiCases {
    /**
     * 展示传统线程池工厂的诊断
     *
     * @param workers 工作线程数
     * @return 线程池
     */
    ExecutorService defaultPool(int workers) {
        return Executors.newFixedThreadPool(workers);
    }

    /**
     * 展示显式配置线程和任务队列
     *
     * @param workers 工作线程数
     * @param capacity 等待队列容量
     * @return 有界线程池
     */
    ExecutorService boundedPool(int workers, int capacity) {
        return new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(capacity));
    }

    /**
     * 展示 Java 21 虚拟线程执行器
     *
     * @return 虚拟线程执行器
     */
    ExecutorService virtualExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 展示从浮点参数构造时的诊断
     *
     * @param amount 浮点金额
     * @return 转换结果
     */
    BigDecimal decimalFromFloating(double amount) {
        return new BigDecimal(amount);
    }

    /**
     * 使用十进制文本保留输入值的精度
     *
     * @param amount 十进制金额文本
     * @return 转换结果
     */
    BigDecimal decimalFromText(String amount) {
        return new BigDecimal(amount);
    }
}
