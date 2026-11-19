// 检查：TestMethodNameCheck
// 使用实际 JUnit 注解，三段式方法名和自定义同名注解都应通过

package project;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class JUnitNamingCases {
    // 测试目录放宽字段说明、数字字面量和重复字符串检查
    private int fixtureLimit = 42;

    String expectedLabels(boolean complete) {
        return complete ? "测试订单" : "测试订单";
    }
    @Test
    void subtotal() { }

    @Test
    void subtotal_validOrder_returnsAmount() { }

    // 命名规则认可的其他 JUnit 入口
    @org.junit.Test
    public void legacySubtotal() { }

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

    static class LocalCases {
        @interface Test { }

        @LocalCases.Test
        void helper() { }
    }
}
