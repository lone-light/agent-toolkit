package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证框架方法的命名和文档豁免
 *
 * @author lonelight
 */
class FrameworkMethodFilterTest {
    @TempDir
    Path directoryPath;

    /**
     * 验证 JUnit 和 HTTP 注解只豁免适用的方法规则
     */
    @Test
    void scan_frameworkMethods_appliesTargetedExemptions() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", "public class Example { @org.junit.jupiter.api.Test public void run_Empty_ReturnsZero() {} @other.Test public void bad_name() {} @org.springframework.web.bind.annotation.GetMapping public int get() { return 0; } } ");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MethodName#productionMethodName")).count());
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MissingJavadocMethod")).count());
    }

    /**
     * 验证 JUnit 豁免不会扩散至局部类的方法
     */
    @Test
    void scan_localClasses_reportsNonTestMethods() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;

                class Example {
                    @org.junit.jupiter.api.Test
                    void run_Empty_ReturnsZero() {
                        class Local {
                            void bad_name() { }
                        }
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MethodName#productionMethodName")).count());
    }

    /**
     * 验证多组通配导入只豁免 JUnit 测试方法
     */
    @Test
    void scan_junitWildcardWithOtherImports_preservesExemption() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;

                import org.junit.jupiter.api.*;
                import org.junit.jupiter.params.*;

                class Example {
                    @Test
                    void run_Empty_ReturnsZero() {}

                    static class Local {
                        @interface Test {}

                        @Test
                        void helper_bad() {}
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        var names = events.stream().filter(event -> CheckTestSupport.matches(event, "MethodName#productionMethodName"))
                .map(AuditEvent::getLine).toList();
        assertEquals(List.of(14), names);
    }
}
