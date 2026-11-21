package lonelight.checks.integration;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证两份完整配置并用时的规则行为
 *
 * @author lonelight
 */
class CombinedRulesTest {
    @TempDir
    Path directoryPath;

    /**
     * 验证完整合规样例可通过两份配置
     */
    @Test
    void scan_compliantExample_passesBothProfiles() throws Exception {
        Path filePath = FixtureCatalog.directory("base").resolve("OrderSummary.java");
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            var events = CheckTestSupport.scan(CheckTestSupport.load(profile), filePath);
            assertTrue(events.isEmpty(), () -> profile + events.stream().map(AuditEvent::getMessage).toList());
        }
    }

    /**
     * 验证项目规则能扫描空文件和仅含注释的文件
     */
    @Test
    void scan_withoutDeclarations_hasNoDiagnostics() throws Exception {
        Path emptyPath = CheckTestSupport.write(directoryPath, "Empty.java", "");
        Path commentPath = CheckTestSupport.write(directoryPath, "Comment.java", "// 预留文件\n");
        var configuration = CheckTestSupport.load("lonelight-project");
        var events = CheckTestSupport.scan(configuration, List.of(emptyPath, commentPath), getClass().getClassLoader());
        assertTrue(events.isEmpty(), () -> events.stream().map(AuditEvent::getMessage).toList().toString());
    }

    /**
     * 验证两份配置并用时各自报告对应的问题
     */
    @Test
    void scan_combinedProfiles_keepsOneOwnerPerFinding() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;

                import app.UserDTO;

                /**
                 * 示例
                 *
                 * @author lonelight
                 */
                public class Example {
                    /**
                     * 输入
                     */
                    private UserDTO request;

                    /**
                     * 获取输入
                     *
                     * @return 输入
                     */
                    public UserDTO getUserDTO() {
                        return request;
                    }
                }
                """);
        var events = new ArrayList<>(CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath));
        events.addAll(CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), filePath));
        assertEquals(1, events.size(), () -> events.stream().map(AuditEvent::getMessage).toList().toString());
        assertTrue(CheckTestSupport.matches(events.getFirst(), "lonelight.checks.naming.VariableTypeNameCheck"));
    }

    /**
     * 验证自定义规则的局部豁免不会影响其他规则
     */
    @Test
    void suppress_customCheck_keepsOtherFindings() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", "class Example { @SuppressWarnings(\"checkstyle:VariableTypeName\") UserDTO request; UserDTO other; }");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), filePath);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "lonelight.checks.naming.VariableTypeNameCheck")).count());
        assertEquals(2, events.stream().filter(event -> CheckTestSupport.matches(event, "lonelight.checks.javadoc.FieldJavadocCheck")).count());
    }

    /**
     * 验证导入与全限定调用各有唯一的诊断来源
     */
    @Test
    void scan_copyCalls_reportsEachThroughItsRule() throws Exception {
        for (String source : List.of(
                "import org.springframework.beans.BeanUtils; class Example { void run() { BeanUtils.copyProperties(a, b); } }",
                "import static org.springframework.beans.BeanUtils.copyProperties; class Example { void run() { copyProperties(a, b); } }",
                "import static org.apache.commons.beanutils.BeanUtils.*; class Example { void run() { copyProperties(a, b); } }",
                "class Example { void run() { org.springframework.beans.BeanUtils.copyProperties(a, b); } }")) {
            Path filePath = CheckTestSupport.write(directoryPath, "Example.java", source);
            var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), filePath).stream()
                    .filter(event -> CheckTestSupport.matches(event, "IllegalImport#reflectionCopyImports")
                            || CheckTestSupport.matches(event, "lonelight.checks.framework.BeanUtilsCallCheck")).toList();
            assertEquals(1, events.size(), source);
        }
    }

    /**
     * 验证异常变量命名和返回标签各由对应规则报告
     */
    @Test
    void scan_catchNamesAndReturnTags_reportsEachOnce() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;

                /** 示例 */
                public class Example {
                    /**
                     * 检查
                     *
                     * @return
                     */
                    public String execute() {
                        try {
                            throw new IllegalArgumentException();
                        } catch (final IllegalArgumentException UpperCase) {
                            return UpperCase.getMessage();
                        }
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(2, events.size(), () -> events.stream().map(AuditEvent::getSourceName).toList().toString());
        assertTrue(events.stream().anyMatch(event -> CheckTestSupport.matches(event, "CatchParameterName")));
        assertTrue(events.stream().anyMatch(event -> CheckTestSupport.matches(event, "JavadocMethod")));
    }

    /**
     * 验证两份完整配置都接受带说明的行内返回标签
     */
    @Test
    void scan_inlineReturnTag_passesBothProfiles() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                package sample;

                /**
                 * 示例
                 *
                 * @author lonelight
                 */
                public class Example {
                    /**
                     * {@return the total}
                     */
                    public int total() {
                        return 0;
                    }
                }
                """);
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            var events = CheckTestSupport.scan(CheckTestSupport.load(profile), filePath);
            assertTrue(events.isEmpty(), () -> profile + events.stream().map(AuditEvent::getMessage).toList());
        }
    }
}
