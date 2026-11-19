package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 Javadoc 的展开格式检查
 *
 * @author lonelight
 */
class MultilineJavadocCheckTest {
    @TempDir
    Path directoryPath;

    /**
     * 验证紧凑 Javadoc 会报告且普通注释保持合规
     */
    @Test
    void scan_compactJavadoc_reportsOnlyDocumentation() throws Exception {
        Path filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                /** 类型说明 */
                interface Example {
                    /** 最大容量 */
                    int CAPACITY = 1;
                    /** 回调说明 */
                    void onCreated();
                    /**
                     * 关闭说明 */
                    void close();
                    /**
                     * 执行说明
                     */
                    void run();
                    /* 普通块注释 */
                    // 普通行注释
                    void stop();
                    /**
                     */
                    void reset();
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), filePath).stream()
                .filter(event -> CheckTestSupport.matches(event, "MultilineJavadoc")).toList();
        assertEquals(List.of(1, 3, 5, 7, 17), events.stream().map(AuditEvent::getLine).toList());
        assertTrue(events.stream().allMatch(event -> event.getSeverityLevel() == SeverityLevel.WARNING));
    }
}
