package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证方法 Javadoc 的句号检查和定位
 *
 * @author lonelight
 */
class MethodJavadocPeriodCheckTest extends CustomCheckTest {
    /**
     * 验证跳过代码片段后仍准确报告句号所在行
     */
    @Test
    void scan_proseAndCode_reportsProseLineNumbers() throws Exception {
        verify(MethodJavadocPeriodCheck.class, """
                class Example {
                    /**
                     * 加载数据。
                     * <pre>
                     * 示例.
                     * </pre>
                     * <!-- 内部说明. -->
                     * {@code 示例.}
                     *
                     * @return 返回结果.
                     */
                    int run() {
                        return 0;
                    }
                }
                """, 2);
        verify(MethodJavadocPeriodCheck.class, "class Example { /** 返回结果 */ int run() { return 0; } }", 0);
        verify(MethodJavadocPeriodCheck.class, "class Example { /** 返回结果。 */ int run() { return 0; } }", 1);
        verify(MethodJavadocPeriodCheck.class, "class Example { /** {@code value.} */ int run() { return 0; } }", 0);
    }

    /**
     * 验证长代码片段可跳过且不吞掉片段后的正文
     */
    @Test
    void scan_longInlineCode_checksFollowingProse() throws Exception {
        String inlineCode = "{@code " + "{value}".repeat(10_000) + ".}";
        verify(MethodJavadocPeriodCheck.class, "class Example { /** " + inlineCode + " */ void run() {} }", 0);
        verify(MethodJavadocPeriodCheck.class, "class Example { /** " + inlineCode + " 正文。 */ void run() {} }", 1);
    }

    /**
     * 验证不同类型的代码片段不会影响周围正文
     */
    @Test
    void scan_mixedCodeMarkup_checksProseOnEitherSide() throws Exception {
        var filePath = CheckTestSupport.write(directoryPath, "Example.java", """
                class Example {
                    /**
                     * 说明。 <PRE class="sample">
                     * <code>示例.</code>
                     * </PRE>
                     * {@literal 示例.}
                     * <code>示例.</code> 结束。
                     */
                    void run() {}
                }
                """);
        var rule = CheckTestSupport.rule(MethodJavadocPeriodCheck.class.getName());
        var events = CheckTestSupport.scan(CheckTestSupport.isolate(rule, true), filePath);
        assertEquals(List.of(3, 7), events.stream().map(AuditEvent::getLine).toList());
    }

    /**
     * 验证代码片段和 HTML 标记均不会掩盖后续正文或产生额外诊断
     */
    @Test
    void scan_snippetAndHtml_checksVisibleProseOnly() throws Exception {
        String source = """
                class Example {
                    /**
                     * {@snippet lang="java" :
                     * // Syntax.
                     * 1. get()
                     * }
                     * {@code 示例.} <b>2. 正文。</b>
                     */
                    void run() {}
                }
                """;
        var file = CheckTestSupport.write(directoryPath, "Example.java", source);
        for (Class<?> type : List.of(MethodJavadocPeriodCheck.class, MethodJavadocNumberingCheck.class)) {
            var configuration = CheckTestSupport.isolate(CheckTestSupport.rule(type.getName()), true);
            var events = CheckTestSupport.scan(configuration, file);
            assertEquals(List.of(7), events.stream().map(AuditEvent::getLine).toList());
        }
    }
}
