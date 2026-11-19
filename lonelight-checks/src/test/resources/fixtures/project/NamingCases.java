// 检查：VariableTypeNameCheck, TestMethodNameCheck, BooleanFieldNameCheck
package project;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

class NamingCases {
    boolean isReady;
    Boolean isActive;
    static boolean isShared;
    boolean[] isSelected;
    OrderShape item;
    OrderShape orderShape;
    OrderShape targetOrderShape;
    ObjectMapper mapper;

    void naming(OrderShape value, String text, List<OrderShape> orders) {
        OrderShape current = value;
        var inferred = current;
        OrderShape[] items = {inferred};
        System.out.println(text + orders + items.length);
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

    record OrderShape(boolean isAvailable, Boolean enabled) { }

    record OrderGroup(OrderShape item, OrderShape primaryOrderShape) { }

    static class LocalCases {
        @interface Test { }

        @LocalCases.Test
        void helper() { }
    }
}
