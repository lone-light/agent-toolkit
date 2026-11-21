package lonelight.checks.framework;

import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证未使用私有字段诊断的框架豁免
 *
 * @author lonelight
 */
class FrameworkFieldUsageFilterTest {
    @TempDir
    Path directoryPath;

    /**
     * 统计按框架约定过滤后的未使用字段诊断
     *
     * @param source 待扫描的源码
     * @return 报告的未使用字段数量
     * @throws Exception 样例写入或扫描失败时
     */
    private int unused(String source) throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", source);
        return (int) CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath).stream()
                .filter(event -> CheckTestSupport.matches(event, "UnusedPrivateField")).count();
    }

    /**
     * 验证只有明确的框架注解或 unused 标记触发豁免
     */
    @Test
    void suppress_frameworkFields_requiresMatchingAnnotation() throws Exception {
        assertEquals(0, unused("@lombok.Data class Example { private String name; }"));
        assertEquals(0, unused("class Example { @org.springframework.beans.factory.annotation.Autowired private Object value; }"));
        assertEquals(0, unused("class Example { @SuppressWarnings(\"unused\") private Object value; }"));
        assertEquals(1, unused("class Example { @SuppressWarnings(\"unchecked\") private Object value; }"));
        assertEquals(1, unused("import other.Getter; class Example { @Getter private Object value; }"));
        assertEquals(1, unused("class Example { @lombok.Getter(lombok.AccessLevel.NONE) private Object value; }"));
        assertEquals(1, unused("class Example { private Object value; }"));
    }

    /**
     * 验证字段级访问器覆盖类型设置，普通注解属性不受 Lombok 访问级别影响
     */
    @Test
    void suppress_fieldOverrides_filtersOnlyGeneratedAccessors() throws Exception {
        assertEquals(1, unused("@lombok.Getter class Example { @lombok.Getter(lombok.AccessLevel.NONE) private String name; }"));
        assertEquals(0, unused("@lombok.Getter @lombok.Setter class Example { @lombok.Getter(lombok.AccessLevel.NONE) private String name; }"));
        assertEquals(1, unused("@lombok.Getter class Example { private static String name; }"));
        assertEquals(1, unused("@lombok.Setter class Example { private final String name = \"value\"; }"));
        assertEquals(0, unused("""
                class Example {
                    static final String NONE = "none";
                    @com.fasterxml.jackson.annotation.JsonProperty(NONE) private Object value;
                }
                """));
    }

    /**
     * 验证同包注解遮蔽通配导入，显式导入或限定名仍指向 Lombok
     */
    @Test
    void suppress_samePackageGetter_respectsImportPrecedence() throws Exception {
        CheckTestSupport.write(directoryPath, "Getter.java", "package flags; public @interface Getter {}");
        assertEquals(2, unused("""
                package flags;
                import lombok.*;
                import java.util.*;
                @Getter class Example { private int first; @Getter private int second; }
                """));
        assertEquals(0, unused("""
                package flags;
                import lombok.Getter;
                @Getter class Example { private int first; private int second; }
                """));
        assertEquals(0, unused("""
                package flags;
                import lombok.*;
                @lombok.Getter class Example { @Getter private int first; }
                """));
        assertEquals(0, unused("""
                package other;
                import lombok.*;
                import java.util.*;
                @Getter class Example { private int value; }
                """));
    }
}
