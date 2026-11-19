package lonelight.checks;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 为自定义规则测试提供源码样例和诊断断言
 *
 * @author lonelight
 */
public abstract class CustomCheckTest {
    @TempDir
    protected Path directory;

    /**
     * 断言源码触发指定规则和数量的诊断
     *
     * @param type 自定义检查器类型
     * @param source 待扫描的源码
     * @param expected 预期的诊断数量
     * @param properties 交替排列的属性名和属性值
     * @throws Exception 样例写入或扫描失败时
     */
    protected void verify(Class<?> type, String source, int expected, String... properties) throws Exception {
        var check = CheckTestSupport.rule(type.getName(), properties);
        Path file = CheckTestSupport.write(directory, "Example.java", source);
        var events = CheckTestSupport.scan(CheckTestSupport.isolate(check, true), file);
        assertEquals(expected, events.size(), () -> source + "\n" + events.stream().map(AuditEvent::getMessage).toList());
        events.forEach(event -> assertEquals(type.getName(), event.getSourceName()));
    }

}
