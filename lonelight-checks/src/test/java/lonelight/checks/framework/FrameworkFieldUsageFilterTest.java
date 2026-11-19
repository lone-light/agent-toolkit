package lonelight.checks.framework;

import java.nio.file.Path;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证未使用私有字段诊断的框架豁免
 *
 * @author lonelight
 */
class FrameworkFieldUsageFilterTest {
    @TempDir
    Path directory;

    /**
     * 统计按框架约定过滤后的未使用字段诊断
     *
     * @param source 待扫描的源码
     * @return 报告的未使用字段数量
     * @throws Exception 样例写入或扫描失败时
     */
    private int unused(String source) throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", source);
        return (int) CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, "UnusedPrivateField")).count();
    }

    /**
     * 验证只有明确的框架注解或 unused 标记触发豁免
     */
    @Test
    void respectsExplicitFrameworkIdentityAndExactUnusedSuppression() throws Exception {
        assertEquals(0, unused("@lombok.Data class Example { private String name; }"));
        assertEquals(0, unused("class Example { @org.springframework.beans.factory.annotation.Autowired private Object value; }"));
        assertEquals(0, unused("class Example { @SuppressWarnings(\"unused\") private Object value; }"));
        assertEquals(1, unused("class Example { @SuppressWarnings(\"unchecked\") private Object value; }"));
        assertEquals(1, unused("import other.Getter; class Example { @Getter private Object value; }"));
        assertEquals(1, unused("class Example { @lombok.Getter(lombok.AccessLevel.NONE) private Object value; }"));
        assertEquals(1, unused("class Example { private Object value; }"));
    }
}
