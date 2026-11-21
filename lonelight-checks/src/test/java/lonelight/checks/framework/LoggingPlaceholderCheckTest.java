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
    void scan_slf4jMessages_reportsDynamicConcatenation() throws Exception {
        verify(LoggingPlaceholderCheck.class, "import org.slf4j.Logger; class Example { Logger logger; void run() { logger.info(\"name={}\", name); logger.info(\"a\" + \"b\"); logger.warn(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "@lombok.extern.slf4j.Slf4j class Example { void run() { log.info(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "import other.Logger; class Example { Logger logger; void run() { logger.info(\"name=\" + name); } }", 0);
    }

    /**
     * 验证变量遮蔽和显式 this 对日志接收者识别的影响
     */
    @Test
    void scan_shadowedLogger_resolvesReceiver() throws Exception {
        verify(LoggingPlaceholderCheck.class, "import org.slf4j.Logger; class Example { Logger logger; void run(Other logger) { logger.info(\"name=\" + name); this.logger.info(\"name=\" + name); } class Nested { Other logger; void run() { logger.info(\"name=\" + name); } } }", 1);
        verify(LoggingPlaceholderCheck.class, "class Example { void run(org.slf4j.Logger logger) { logger.info(\"name=\" + name); } }", 1);
        verify(LoggingPlaceholderCheck.class, "class Example { void run(org.slf4j.Logger logger, org.slf4j.Marker marker) { logger.info(template, \"name=\" + name); logger.info(marker, \"name=\" + name); } }", 1);
    }

    /**
     * 验证消息字符串拼接与数值运算按各自的表达式判断
     */
    @Test
    void scan_arithmeticInMessage_doesNotReportStringConcatenation() throws Exception {
        verify(LoggingPlaceholderCheck.class, """
                class Example {
                    void run(org.slf4j.Logger log, int index) {
                        log.info(String.format("position %d", index + 1));
                        log.info(index + 1 > 0 ? "ready" : "empty");
                        log.info((String) "prefix" + "suffix");
                        log.info("key".length() + index > 0 ? "yes" : "no");
                    }
                }
                """, 0);
        verify(LoggingPlaceholderCheck.class, """
                class Example {
                    org.slf4j.Marker marker;
                    void run(org.slf4j.Logger log, int index) {
                        log.info((this.marker), ("index=" + index));
                        log.info(index == 0 ? "empty" : "index=" + index);
                        log.info("position " + 1 + index);
                        log.info(index + 1 + " items");
                        log.info((index == 0 ? "empty" : ("item ")) + index);
                        log.info((java.lang.String) "position " + index);
                    }
                }
                """, 6);
    }
}
