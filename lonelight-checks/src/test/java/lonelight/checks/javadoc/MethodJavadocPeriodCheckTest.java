package lonelight.checks.javadoc;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

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
    void checksProseAndSkipsCodeWithoutLosingLineNumbers() throws Exception {
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
}
