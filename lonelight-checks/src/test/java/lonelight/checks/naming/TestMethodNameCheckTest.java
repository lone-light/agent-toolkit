package lonelight.checks.naming;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证 JUnit 测试方法的分段命名检查
 *
 * @author lonelight
 */
class TestMethodNameCheckTest extends CustomCheckTest {
    /**
     * 验证只有可识别的 JUnit 测试方法接受命名检查
     */
    @Test
    void resolvesJUnitWithoutMatchingUnrelatedTestAnnotations() throws Exception {
        verify(TestMethodNameCheck.class, "class Example { @org.junit.jupiter.api.Test void run() {} }", 1);
        verify(TestMethodNameCheck.class, "import org.junit.jupiter.api.*; class Example { @Test void run_Empty_ReturnsZero() {} }", 0);
        verify(TestMethodNameCheck.class, "import other.Test; class Example { @Test void run() {} }", 0);
        verify(TestMethodNameCheck.class, "class Example { void run() {} }", 0);
    }
}
