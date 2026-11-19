package lonelight.checks.imports;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 IDEA 导入布局和注解简写规则
 *
 * @author lonelight
 */
class IdeaStyleChecksTest {
    private static final Path BASE_EXAMPLES = FixtureCatalog.directory("base");

    @TempDir
    Path directoryPath;

    /**
     * 验证 IDEA 默认导入空行及 java 与 javax 的分组
     */
    @Test
    void scan_defaultImportGroups_appliesIdeaSpacing() throws Exception {
        String valid = """
                import app.Order;

                import javax.sql.DataSource;
                import java.util.List;

                import static java.util.Collections.emptyList;

                class Example {}
                """;
        assertTrue(findings(valid, "ImportGroupSpacing").isEmpty());
        assertEquals(List.of(2, 4, 7), findings(valid
                .replace("Order;\n\n", "Order;\n")
                .replace("DataSource;\n", "DataSource;\n\n")
                .replace("List;\n\n", "List;\n\n\n"), "ImportGroupSpacing"));
        assertTrue(findings(valid.replace("Order;\n\n", "Order;\n// JDBC types\n"), "ImportGroupSpacing").isEmpty());
        assertEquals(List.of(2), findings("""
                import app.Order;
                import static java.util.Collections.emptyList;

                class Example {}
                """, "ImportGroupSpacing"));
    }

    /**
     * 验证全限定注解在无命名冲突时才提示简写
     */
    @Test
    void scan_qualifiedAnnotations_reportsSafeShortNames() throws Exception {
        String shortNames = """
                import jakarta.validation.constraints.NotNull;
                class Example {
                    @NotNull String title;
                }
                """;
        String qualified = shortNames.replace("@NotNull", "@jakarta.validation.constraints.NotNull");
        assertTrue(findings(shortNames, "QualifiedAnnotation").isEmpty());
        assertEquals(List.of(3), findings(qualified, "QualifiedAnnotation"));
        assertEquals(List.of(2), findings(qualified.substring(qualified.indexOf("class Example")), "QualifiedAnnotation"));
        assertTrue(findings(qualified.replace("import jakarta.validation.constraints.NotNull;", "import other.NotNull;"),
                "QualifiedAnnotation").isEmpty());
        assertTrue(findings("""
                class Example {
                    @a.Flag @b.Flag String title;
                    @Outer.Marker String subtitle;
                }
                """, "QualifiedAnnotation").isEmpty());
        assertTrue(findings("""
                import static app.Outer.Flag;
                class Example {
                    @other.Flag String title;
                    @Flag String subtitle;
                }
                """, "QualifiedAnnotation").isEmpty());
        assertTrue(findings("""
                class Example {
                    String title;
                    @other.String String subtitle;
                }
                """, "QualifiedAnnotation").isEmpty());
    }

    /**
     * 核对手动导入样例分别覆盖分组顺序、组内排序和三处空行问题
     */
    @Test
    void imports_groupedExamples_coverOrderingAndSpacing() throws Exception {
        String ordering = Files.readString(BASE_EXAMPLES.resolve("ImportsOrder.java"));
        String alphabetical = Files.readString(BASE_EXAMPLES.resolve("ImportsAlphabetical.java"));
        String spacing = Files.readString(BASE_EXAMPLES.resolve("ImportsSpacing.java"));
        assertFalse(findings(ordering, "CustomImportOrder").isEmpty());
        assertEquals(4, findings(alphabetical, "CustomImportOrder").size());
        assertTrue(findings(alphabetical, "ImportGroupSpacing").isEmpty());
        assertTrue(findings(spacing, "CustomImportOrder").isEmpty());
        assertEquals(3, findings(spacing, "ImportGroupSpacing").size());
    }

    /**
     * 核对导入与注解的对照样例符合各自的检查
     */
    @Test
    void formatting_compliantExamples_passTargetChecks() throws Exception {
        String imports = Files.readString(BASE_EXAMPLES.resolve("ImportsSorted.java"));
        String annotations = Files.readString(BASE_EXAMPLES.resolve("AnnotationsFormatted.java"));
        for (String rule : List.of("CustomImportOrder", "ImportGroupSpacing", "UnusedImports", "RedundantImport")) {
            assertTrue(findings(imports, rule).isEmpty(), rule);
        }
        for (String rule : List.of("AnnotationLocation", "QualifiedAnnotation")) {
            assertTrue(findings(annotations, rule).isEmpty(), rule);
        }
    }

    /**
     * 提取目标规则报告警告的行号
     *
     * @param input 待扫描的源码
     * @param rule  要筛选的检查名称
     * @return 警告所在的行号
     * @throws Exception 样例写入或扫描失败时
     */
    private List<Integer> findings(String input, String rule) throws Exception {
        Path sourcePath = CheckTestSupport.write(directoryPath, "Example.java", input);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), sourcePath).stream()
                .filter(event -> CheckTestSupport.matches(event, rule)).toList();
        assertTrue(events.stream().allMatch(event -> event.getSeverityLevel() == SeverityLevel.WARNING));
        return events.stream().map(AuditEvent::getLine).toList();
    }
}
