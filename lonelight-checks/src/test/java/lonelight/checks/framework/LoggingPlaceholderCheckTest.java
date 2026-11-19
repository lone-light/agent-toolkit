package lonelight.checks.framework;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证 SLF4J 日志接收者和动态拼接的识别
 *
 * @author lonelight
 */
class LoggingPlaceholderCheckTest extends CustomCheckTest {
    /**
     * 验证 SLF4J 动态拼接会提示且纯字面量拼接可通过
     */
    @Test
    void detectsSlf4jDynamicMessagesAndLeavesLiteralConcatenation() throws Exception {
        verify(LoggingPlaceholderCheck.class, "import org.slf4j.Logger; class Example { Logger logger; void run() { logger.info(\"name={}\", name); logger.info(\"a\" + \"b\"); logger.warn(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "@lombok.extern.slf4j.Slf4j class Example { void run() { log.info(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "import other.Logger; class Example { Logger logger; void run() { logger.info(\"name=\" + name); } }", 0);
    }

    /**
     * 验证变量遮蔽和显式 this 对日志接收者识别的影响
     */
    @Test
    void respectsFieldShadowingDifferentClassesAndExplicitThis() throws Exception {
        verify(LoggingPlaceholderCheck.class, "import org.slf4j.Logger; class Example { Logger logger; void run(Other logger) { logger.info(\"name=\" + name); this.logger.info(\"name=\" + name); } class Nested { Other logger; void run() { logger.info(\"name=\" + name); } } }", 1);
        verify(LoggingPlaceholderCheck.class, "class Example { void run(org.slf4j.Logger logger) { logger.info(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "class Example { void run(org.slf4j.Logger logger, org.slf4j.Marker marker) { logger.info(template, \"name=\" + name); logger.info(marker, \"name=\" + name); } }", 1);
    }
}
