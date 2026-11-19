package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import java.nio.file.Path;
import java.util.List;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Javadoc 的展开格式检查
 *
 * @author lonelight
 */
class MultilineJavadocCheckTest {
    @TempDir
    Path directory;

    /**
     * 验证紧凑 Javadoc 会报告且普通注释保持合规
     */
    @Test
    void detectsCompactDocumentationButLeavesOtherCommentsAlone() throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
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
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, "MultilineJavadoc")).toList();
        assertEquals(List.of(1, 3, 5, 7, 17), events.stream().map(AuditEvent::getLine).toList());
        assertTrue(events.stream().allMatch(event -> event.getSeverityLevel() == SeverityLevel.WARNING));
    }
}
