package lonelight.checks.naming;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证布尔属性的 is 前缀检查
 *
 * @author lonelight
 */
class BooleanFieldNameCheckTest extends CustomCheckTest {
    /**
     * 验证 is 前缀约束仅适用于布尔属性
     */
    @Test
    void checksPropertiesAndLeavesMethodsLocalsStaticsAndArrays() throws Exception {
        verify(BooleanFieldNameCheck.class, "class Example { boolean isReady; Boolean isActive; boolean island; boolean ready; static boolean isCached; boolean[] isFlags; boolean isReady() { boolean isLocal = true; return isLocal; } }", 2);
        verify(BooleanFieldNameCheck.class, "record Example(Boolean isActive) {}", 1);
        verify(BooleanFieldNameCheck.class, "import other.Boolean; class Example { Boolean isActive; }", 0);
    }
}
