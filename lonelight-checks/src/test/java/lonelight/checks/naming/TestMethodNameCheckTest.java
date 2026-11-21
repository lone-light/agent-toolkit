package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void scan_testAnnotations_recognizesJUnitOnly() throws Exception {
        verify(TestMethodNameCheck.class, "class Example { @org.junit.jupiter.api.Test void run() {} }", 1);
        verify(TestMethodNameCheck.class, "import org.junit.jupiter.api.*; class Example { @Test void run_Empty_ReturnsZero() {} }", 0);
        verify(TestMethodNameCheck.class, "import other.Test; class Example { @Test void run() {} }", 0);
        verify(TestMethodNameCheck.class, "class Example { void run() {} }", 0);
    }

    /**
     * 验证优化导入生成通配导入后仍识别 JUnit，并区分局部同名注解
     */
    @Test
    void scan_junitWildcardWithOtherImports_respectsVisibleTestAnnotation() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                import org.junit.jupiter.api.*;
                import org.junit.jupiter.params.*;

                class Example {
                    @Test
                    void run() {}

                    @ParameterizedTest
                    void run_Empty_ReturnsZero() {}

                    static class Local {
                        @interface Test {}

                        @Test
                        void helper() {}
                    }
                }
                """);
        var check = CheckTestSupport.rule(TestMethodNameCheck.class.getName());
        var events = CheckTestSupport.scan(CheckTestSupport.isolate(check, true), filePath);
        assertEquals(List.of(6), events.stream().map(AuditEvent::getLine).toList());
    }
}
