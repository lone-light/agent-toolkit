package lonelight.checks.framework;

import java.nio.file.Path;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证框架方法的命名和文档豁免
 *
 * @author lonelight
 */
class FrameworkMethodFilterTest {
    @TempDir
    Path directory;

    /**
     * 验证 JUnit 和 HTTP 注解只豁免适用的方法规则
     */
    @Test
    void avoidsJUnitAndHttpConflictsButChecksUnrelatedAnnotations() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", "public class Example { @org.junit.jupiter.api.Test public void run_Empty_ReturnsZero() {} @other.Test public void bad_name() {} @org.springframework.web.bind.annotation.GetMapping public int get() { return 0; } } ");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MethodName#productionMethodName")).count());
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MissingJavadocMethod")).count());
    }

    /**
     * 验证 JUnit 豁免不会扩散至局部类的方法
     */
    @Test
    void junitExemptionDoesNotExtendToMethodsInLocalClasses() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
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
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "MethodName#productionMethodName")).count());
    }
}
