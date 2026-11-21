// 检查：TestMethodNameCheck
// 使用实际 JUnit 注解，三段式方法名和自定义同名注解都应通过

package project;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

/**
 * 对照 JUnit Jupiter 入口和同名自定义注解的方法命名
 *
 * @author lonelight
 */
class JUnitNamingCases {
    @Test
    void subtotal() { }

    @Test
    void subtotal_validOrder_returnsAmount() { }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void parameterizedSubtotal(int count) { }

    @RepeatedTest(2)
    void repeatedSubtotal() { }

    @TestFactory
    List<DynamicTest> subtotalCases() {
        return List.of();
    }

    @TestTemplate
    void subtotalTemplate() { }

    /**
     * 声明同名注解供普通方法使用
     *
     * @author lonelight
     */
    static class LocalCases {
        /**
         * 标记普通方法
         *
         * @author lonelight
         */
        @interface Test { }

        @Test
        void helper() { }
    }
}
