package lonelight.checks.configuration;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证目录排除和局部豁免的作用范围
 *
 * @author lonelight
 */
class FiltersTest {
    @TempDir
    Path directoryPath;

    /**
     * 验证单行枚举的内侧空格豁免保留其他空格检查
     */
    @Test
    void suppress_compactEnum_keepsOtherBraceDiagnostics() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    @Deprecated
                    enum Compact {ON, OFF}
                    enum MissingSpace{ON, OFF}
                    enum Multiline {ON,
                        OFF}
                    void run() {run();}
                    enum Trailing {ON, OFF;}
                    enum WithField {ON, OFF; int count;}
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        var lines = events.stream()
                .filter(event -> CheckTestSupport.matches(event, "WhitespaceAround")).map(AuditEvent::getLine).toList();
        assertEquals(List.of(4, 5, 6, 7, 7, 9, 9), lines);
        assertTrue(events.stream().noneMatch(event -> CheckTestSupport.matches(event, "LeftCurly")
                && (event.getLine() == 3 || event.getLine() == 4)));
    }

    /**
     * 验证独占一行的段落标签可以代替段落之间的空行
     */
    @Test
    void suppress_standaloneParagraphTag_keepsInlineTagDiagnostics() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    /**
                     * 第一段
                     * <p>
                     * 第二段
                     */
                    void formatted() {}

                    /**
                     * 第一段
                     * <p>第二段
                     */
                    void inline() {}
                }
                """);
        var lines = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath).stream()
                .filter(event -> CheckTestSupport.matches(event, "JavadocParagraph")).map(AuditEvent::getLine).toList();
        assertEquals(List.of(11), lines);
    }

    /**
     * 验证规则跳过构建产物且仍检查真实源码
     */
    @Test
    void scan_generatedDirectories_keepsSourceRoots() throws Exception {
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            Path generatedPath = CheckTestSupport.write(directoryPath.resolve("target/generated"), "Example.java", "invalid Java");
            assertTrue(CheckTestSupport.scan(CheckTestSupport.load(profile), generatedPath).isEmpty());
        }
        Path realSourcePath = CheckTestSupport.write(directoryPath.resolve("build/src/main/java"), "Example.java", "public class Example {}\n");
        assertFalse(CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), realSourcePath).isEmpty());
    }

    /**
     * 验证成对注释只屏蔽指定规则
     */
    @Test
    void suppress_pairedComments_skipsNamedCheck() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    void run() {
                        // CHECKSTYLE.OFF: NeedBraces
                        if (true) run();
                        // CHECKSTYLE.ON: NeedBraces
                        if (true) run();
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "NeedBraces")).count());
    }

    /**
     * 规则名称和标识须完整匹配，拼错的标记保留诊断
     */
    @Test
    void suppress_similarNamesAndMalformedMarkers_keepsViolations() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    // CHECKSTYLE.OFF: Method
                    void UpperOne() {}
                    // CHECKSTYLE.ON: Method
                    // CHECKSTYLE.OFF: productionMethod
                    void UpperTwo() {}
                    // CHECKSTYLE.ON: productionMethod
                    // CHECKSTYLE-OFF: MethodName
                    void UpperThree() {}
                    // CHECKSTYLE-ON: MethodName
                    // CHECKSTYLE.OFF: MethodName|
                    void UpperFour() {}
                    // CHECKSTYLE.ON: MethodName|
                    // CHECKSTYLE.OFF: MethodNameCheck|NeedBraces
                    void UpperFive() { if (true) UpperOne(); }
                    // CHECKSTYLE.ON: MethodNameCheck|NeedBraces
                    // CHECKSTYLE.OFF: productionMethodName
                    void UpperSix() {}
                    // CHECKSTYLE.ON: productionMethodName
                    void UpperSeven() { if (true) UpperOne(); }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(List.of(3, 6, 9, 12, 20), events.stream()
                .filter(event -> CheckTestSupport.matches(event, "MethodName")).map(AuditEvent::getLine).toList());
        assertEquals(List.of(20), events.stream()
                .filter(event -> CheckTestSupport.matches(event, "NeedBraces")).map(AuditEvent::getLine).toList());
    }

    /**
     * 源码解析失败时报告错误，同批次的后续源码仍按规则检查
     */
    @Test
    void scan_unparseableFile_reportsErrorAndChecksNextFile() throws Exception {
        Path brokenPath = CheckTestSupport.write(directoryPath, "Broken.java", "class Broken {");
        Path validPath = CheckTestSupport.write(directoryPath, "Example.java", """
                public class Example {
                    void run(int count) { if (count > 42) run(count); }
                }
                """);
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            var events = CheckTestSupport.scan(CheckTestSupport.load(profile), List.of(brokenPath, validPath),
                    getClass().getClassLoader());
            var failures = events.stream().filter(event -> event.getFileName().equals(brokenPath.toString())
                    && event.getSourceName().endsWith(".TreeWalker")).toList();
            assertEquals(1, failures.size(), profile);
            assertEquals("error", failures.getFirst().getSeverityLevel().getName());
            String expectedRule = profile.equals("lonelight-base") ? "NeedBraces" : "MagicNumber";
            assertTrue(events.stream().anyMatch(event -> event.getFileName().equals(validPath.toString())
                    && CheckTestSupport.matches(event, expectedRule)), profile);
        }
    }

    /**
     * 验证行宽检查使用独立的成对豁免标记
     */
    @Test
    void suppress_lineLengthMarkers_skipsMarkedLines() throws Exception {
        String longLine = "// " + "x".repeat(130) + "\n";
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", "// CHECKSTYLE.OFF: LineLength\n"
                + longLine + "// CHECKSTYLE.ON: LineLength\n" + longLine + "class Example {}\n");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), filePath);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "LineLength")).count());
    }

    /**
     * 验证测试目录适用指定的检查豁免
     */
    @Test
    void scan_testSources_appliesConfiguredExemptions() throws Exception {
        String source = """
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                class Example {
                    private int count = 42;

                    @GetMapping
                    String status() { return "repeated"; }

                    String fallback() { return "repeated"; }
                }
                """;
        Path mainPath = CheckTestSupport.write(directoryPath.resolve("src/main/java"), "Example.java", source);
        Path testPath = CheckTestSupport.write(directoryPath.resolve("src/test/java"), "Example.java", source);
        var configuration = CheckTestSupport.load("lonelight-project");
        var mainEvents = CheckTestSupport.scan(configuration, mainPath);
        var testEvents = CheckTestSupport.scan(configuration, testPath);
        assertTrue(testEvents.stream().anyMatch(event -> CheckTestSupport.matches(event, "TypeJavadoc")));
        for (String name : List.of("MagicNumber", "MultipleStringLiterals", "FieldJavadoc", "ControllerApiDocumentation")) {
            assertTrue(mainEvents.stream().anyMatch(event -> CheckTestSupport.matches(event, name)), name);
            assertTrue(testEvents.stream().noneMatch(event -> CheckTestSupport.matches(event, name)), name);
        }
    }
}
