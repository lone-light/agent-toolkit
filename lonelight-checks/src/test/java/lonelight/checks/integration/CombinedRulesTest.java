package lonelight.checks.integration;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证两份完整配置并用时的规则行为
 *
 * @author lonelight
 */
class CombinedRulesTest {
    @TempDir
    Path directory;

    /**
     * 验证完整合规样例可通过两份配置
     */
    @Test
    void completeAcceptedExamplePassesBothProfiles() throws Exception {
        Path file = FixtureCatalog.directory("base").resolve("OrderSummary.java");
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            var events = CheckTestSupport.scan(CheckTestSupport.load(profile), file);
            assertTrue(events.isEmpty(), () -> profile + events.stream().map(AuditEvent::getMessage).toList());
        }
    }

    /**
     * 验证项目规则能扫描空文件和仅含注释的文件
     */
    @Test
    void scan_withoutDeclarations_hasNoDiagnostics() throws Exception {
        Path emptyPath = CheckTestSupport.write(directory, "Empty.java", "");
        Path commentPath = CheckTestSupport.write(directory, "Comment.java", "// 预留文件\n");
        var configuration = CheckTestSupport.load("lonelight-project");
        var events = CheckTestSupport.scan(configuration, List.of(emptyPath, commentPath), getClass().getClassLoader());
        assertTrue(events.isEmpty(), () -> events.stream().map(AuditEvent::getMessage).toList().toString());
    }

    /**
     * 验证两份配置并用时各自报告对应的问题
     */
    @Test
    void combinedProfilesPreserveEachRulesResponsibility() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
                package sample;

                import app.UserDTO;

                /**
                 * 示例
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
        var events = new ArrayList<>(CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file));
        events.addAll(CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file));
        assertEquals(1, events.size(), () -> events.stream().map(AuditEvent::getMessage).toList().toString());
        assertTrue(CheckTestSupport.matches(events.getFirst(), "lonelight.checks.naming.VariableTypeNameCheck"));
    }

    /**
     * 验证自定义规则的局部豁免不会影响其他规则
     */
    @Test
    void localSuppressionWorksForCustomChecksAndDoesNotSilenceOthers() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", "class Example { @SuppressWarnings(\"checkstyle:VariableTypeName\") UserDTO request; UserDTO other; }");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "lonelight.checks.naming.VariableTypeNameCheck")).count());
        assertEquals(2, events.stream().filter(event -> CheckTestSupport.matches(event, "lonelight.checks.javadoc.FieldJavadocCheck")).count());
    }

    /**
     * 验证导入与全限定调用各有唯一的诊断来源
     */
    @Test
    void importedAndFullyQualifiedCopyCallsHaveOneOwnerEach() throws Exception {
        for (String source : List.of(
                "import org.springframework.beans.BeanUtils; class Example { void run() { BeanUtils.copyProperties(a, b); } }",
                "import static org.springframework.beans.BeanUtils.copyProperties; class Example { void run() { copyProperties(a, b); } }",
                "import static org.apache.commons.beanutils.BeanUtils.*; class Example { void run() { copyProperties(a, b); } }",
                "class Example { void run() { org.springframework.beans.BeanUtils.copyProperties(a, b); } }")) {
            Path file = CheckTestSupport.write(directory, "Example.java", source);
            var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                    .filter(event -> CheckTestSupport.matches(event, "IllegalImport#reflectionCopyImports")
                            || CheckTestSupport.matches(event, "lonelight.checks.framework.BeanUtilsCallCheck")).toList();
            assertEquals(1, events.size(), source);
        }
    }

    /**
     * 验证异常变量命名和返回标签各由对应规则报告
     */
    @Test
    void catchNamesAndReturnTagsHaveOneOwnerEach() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
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
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        assertEquals(2, events.size(), () -> events.stream().map(AuditEvent::getSourceName).toList().toString());
        assertTrue(events.stream().anyMatch(event -> CheckTestSupport.matches(event, "CatchParameterName")));
        assertTrue(events.stream().anyMatch(event -> CheckTestSupport.matches(event, "JavadocMethod")));
    }
}
