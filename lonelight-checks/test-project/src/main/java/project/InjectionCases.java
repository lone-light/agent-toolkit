// 检查：ConstructorInjectionCheck

package project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * 展示字段注入和构造器依赖字段的对照
 *
 * @author lonelight
 */
@Service
class InjectionCases {
    /**
     * 字段注入会触发检查
     */
    @Autowired
    private Runnable runnable;

    /**
     * 构造器依赖缺少 final 会触发检查
     */
    private Clock clock;

    /**
     * 通过构造器注入时钟
     *
     * @param clock 接口使用的时钟
     */
    InjectionCases(Clock clock) {
        this.clock = clock;
    }

    /**
     * 执行操作并返回时间
     *
     * @return 执行时间戳
     */
    public long run() {
        runnable.run();
        return clock.millis();
    }

    /**
     * 展示私有不可变依赖字段
     *
     * @author lonelight
     */
    @Service
    static class ConstructorExample {
        /**
         * 接口使用的时钟
         */
        private final Clock clock;

        /**
         * 保存注入的时钟
         *
         * @param clock 接口使用的时钟
         */
        ConstructorExample(Clock clock) {
            this.clock = clock;
        }

        /**
         * 获取当前时间
         *
         * @return 当前时间戳
         */
        long now() {
            return clock.millis();
        }
    }
}
