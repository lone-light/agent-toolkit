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
     * 外层测试方法的文档豁免止于签名，局部类构造器仍须说明参数
     */
    @Test
    void scan_constructorInsideTest_checksItsOwnJavadoc() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    @org.junit.jupiter.api.Test
                    void create_withValue_succeeds() {
                        class Local {
                            /** 构建实例 */
                            Local(int value) {}
                        }
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(List.of(6), events.stream().filter(event -> CheckTestSupport.matches(event, "JavadocMethod"))
                .map(AuditEvent::getLine).toList());
    }

    /**
     * 同名嵌套注解只在自己的作用域内遮蔽标准 Override
     */
    @Test
    void scan_shadowedOverride_exemptsOnlyStandardAnnotation() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                public class Example {
                    @Override
                    public String toString() { return "Example"; }
                    public static class Nested {
                        @interface Override {}
                        @Override
                        public int total() { return 0; }
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(List.of(6), events.stream().filter(event -> CheckTestSupport.matches(event, "MissingJavadocMethod"))
                .map(AuditEvent::getLine).toList());
    }

    /**
     * 同包注解优先于隐式导入，显式写出的标准注解仍可豁免
     */
    @Test
    void scan_samePackageOverride_reportsCustomAnnotation() throws Exception {
        CheckTestSupport.write(directoryPath, "Override.java", "package sample; @interface Override {}");
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;
                public class Example {
                    @java.lang.Override
                    public String toString() { return "Example"; }
                    @Override
                    public int total() { return 0; }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(List.of(5), events.stream().filter(event -> CheckTestSupport.matches(event, "MissingJavadocMethod"))
                .map(AuditEvent::getLine).toList());
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

    /**
     * 验证基础配置与项目规则对同包、显式导入和通配导入使用相同的框架注解
     */
    @Test
    void scan_siblingAnnotations_respectsJavaImportsAcrossProfiles() throws Exception {
        CheckTestSupport.write(directoryPath, "Test.java", "package sample; @interface Test {}");
        CheckTestSupport.write(directoryPath, "GetMapping.java", "package sample; @interface GetMapping {}");
        String source = """
                package %s;
                import org.junit.jupiter.api.%s;
                import org.springframework.web.bind.annotation.%s;
                @org.springframework.web.bind.annotation.RestController
                public class Example {
                    @Test public void helper_name() {}
                    @GetMapping public int status() { return 200; }
                }
                """;
        verifyFrameworkImports(CheckTestSupport.write(directoryPath, "Example.java",
                source.formatted("sample", "*", "*")), true);
        verifyFrameworkImports(CheckTestSupport.write(directoryPath, "Example.java",
                source.formatted("sample", "Test", "GetMapping")), false);
        verifyFrameworkImports(CheckTestSupport.write(directoryPath, "Example.java",
                source.formatted("other", "*", "*")), false);
    }

    /**
     * 核对两套配置对同一文件的四项诊断
     *
     * @param file   待扫描的源码
     * @param custom 两处注解是否均为本包自定义类型
     */
    private static void verifyFrameworkImports(Path file, boolean custom) throws Exception {
        var base = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        var project = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file);
        assertEquals(custom, base.stream().anyMatch(event ->
                event.getLine() == 6 && CheckTestSupport.matches(event, "MethodName#productionMethodName")));
        assertEquals(custom, base.stream().anyMatch(event ->
                event.getLine() == 7 && CheckTestSupport.matches(event, "MissingJavadocMethod")));
        assertEquals(!custom, project.stream().anyMatch(event ->
                event.getLine() == 6 && CheckTestSupport.matches(event, "lonelight.checks.naming.TestMethodNameCheck")));
        assertEquals(!custom, project.stream().anyMatch(event ->
                event.getLine() == 7 && CheckTestSupport.matches(event, "lonelight.checks.framework.ControllerApiDocumentationCheck")));
    }
}
