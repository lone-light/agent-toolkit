package lonelight.checks.configuration;

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
        Path filePath = CheckTestSupport.write(directoryPath.resolve("src/test/java"), "Example.java",
                "class Example { private int count = 42; void run() { use(\"repeated\"); use(\"repeated\"); } }");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), filePath);
        for (String name : List.of("MagicNumber", "MultipleStringLiterals", "lonelight.checks.javadoc.FieldJavadocCheck")) {
            assertTrue(events.stream().noneMatch(event -> CheckTestSupport.matches(event, name)), name);
        }
    }
}
