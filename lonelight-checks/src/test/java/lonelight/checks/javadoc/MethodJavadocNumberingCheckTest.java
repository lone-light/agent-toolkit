package lonelight.checks.javadoc;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证方法 Javadoc 的步骤编号检查
 *
 * @author lonelight
 */
class MethodJavadocNumberingCheckTest extends CustomCheckTest {
    /**
     * 验证步骤编号可检出且代码片段可通过
     */
    @Test
    void checksNumberedProseAndIgnoresCode() throws Exception {
        verify(MethodJavadocNumberingCheck.class, """
                class Example {
                    /**
                     * 1. 查询数据
                     * 二、转换数据
                     * <pre>
                     * 1. 示例
                     * </pre>
                     * <!-- 1. 内部说明 -->
                     * {@code 1. 示例}
                     */
                    void run() { }
                }
                """, 2);
        verify(MethodJavadocNumberingCheck.class, "class Example { /** 1. 获取数据 */ void run() {} }", 1);
        verify(MethodJavadocNumberingCheck.class, "class Example { /** <code>1. sample</code> */ void run() {} }", 0);
        verify(MethodJavadocNumberingCheck.class, "class Example { /** @param count 1.5 倍 */ void run(int count) {} }", 0);
        verify(MethodJavadocNumberingCheck.class, "class Example { /** 1.5 倍的缩放系数 */ void run() {} }", 0);
        verify(MethodJavadocNumberingCheck.class, "class Example { /** 1.获取数据 */ void run() {} }", 1);
    }
}
