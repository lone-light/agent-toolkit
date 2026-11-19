package lonelight.checks.javadoc;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证类型注释的摘要和作者检查
 *
 * @author lonelight
 */
class TypeJavadocCheckTest extends CustomCheckTest {
    /**
     * 验证已有文档的单行摘要、空作者和多个作者处理
     */
    @Test
    void scan_typeDocumentation_reportsSummaryAndAuthorIndependently() throws Exception {
        verify(TypeJavadocCheck.class, """
                /**
                 * 处理订单
                 * 并计算总额
                 *
                 * @author
                 */
                class Bad {}
                /**
                 * 处理订单并计算总额
                 *
                 * @author lonelight
                 * @author alice
                 */
                record Good(int total) {}
                /**
                 * 查询 {@link Good} 的金额
                 *
                 * @author lonelight
                 */
                interface Query {}
                """, 2);
    }

    /**
     * 验证项目规则只报告缺少注释的非公开类型
     */
    @Test
    void scan_missingTypeDocumentation_reportsNonpublicTypes() throws Exception {
        verify(TypeJavadocCheck.class, """
                public class Example {}
                class Helper {}
                """, 1);
    }

    /**
     * 验证 HTML 标记和注释不算正文，行内代码占用正文行
     */
    @Test
    void scan_inlineTagsAndHtml_countsOnlyVisibleTextLines() throws Exception {
        verify(TypeJavadocCheck.class, """
                /**
                 * <p>
                 * 查询 {@code total} 的金额
                 * </p>
                 * <!-- 内部说明 -->
                 *
                 * @author <b>lonelight</b>
                 */
                class Example {}
                """, 0);
        verify(TypeJavadocCheck.class, """
                /**
                 * <!-- 内部说明 -->
                 *
                 * @author lonelight
                 */
                class Example {}
                """, 1);
    }
}
