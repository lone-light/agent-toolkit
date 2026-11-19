package lonelight.checks.configuration;

import java.nio.file.Path;
import java.util.List;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证目录排除和局部豁免的作用范围
 *
 * @author lonelight
 */
class FiltersTest {
    @TempDir
    Path directory;

    /**
     * 验证规则跳过构建产物且仍检查真实源码
     */
    @Test
    void generatedOutputsAreSkippedAndSourceRootsRemainChecked() throws Exception {
        for (String profile : List.of("lonelight-base", "lonelight-project")) {
            Path generated = CheckTestSupport.write(directory.resolve("target/generated"), "Example.java", "invalid Java");
            assertTrue(CheckTestSupport.scan(CheckTestSupport.load(profile), generated).isEmpty());
        }
        Path realSource = CheckTestSupport.write(directory.resolve("build/src/main/java"), "Example.java", "public class Example {}\n");
        assertFalse(CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), realSource).isEmpty());
    }

    /**
     * 验证成对注释只屏蔽指定规则
     */
    @Test
    void pairedCommentsSuppressOnlyTheirNamedCheck() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
                class Example {
                    void run() {
                        // CHECKSTYLE.OFF: NeedBraces
                        if (true) run();
                        // CHECKSTYLE.ON: NeedBraces
                        if (true) run();
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "NeedBraces")).count());
    }

    /**
     * 验证行宽检查使用独立的成对豁免标记
     */
    @Test
    void lineLengthUsesItsOwnExplicitPairedMarkers() throws Exception {
        String longLine = "// " + "x".repeat(130) + "\n";
        Path file = CheckTestSupport.write(directory, "Example.java", "// CHECKSTYLE.OFF: LineLength\n"
                + longLine + "// CHECKSTYLE.ON: LineLength\n" + longLine + "class Example {}\n");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file);
        assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, "LineLength")).count());
    }

    /**
     * 验证测试目录适用指定的检查豁免
     */
    @Test
    void testPathsSuppressOnlyExplicitTestNoise() throws Exception {
        Path file = CheckTestSupport.write(directory.resolve("src/test/java"), "Example.java",
                "class Example { private int count = 42; void run() { use(\"repeated\"); use(\"repeated\"); } }");
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file);
        for (String name : List.of("MagicNumber", "MultipleStringLiterals", "lonelight.checks.javadoc.FieldJavadocCheck")) {
            assertTrue(events.stream().noneMatch(event -> CheckTestSupport.matches(event, name)), name);
        }
    }
}
