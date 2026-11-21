package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.TreeWalkerAuditEvent;
import com.puppycrawl.tools.checkstyle.TreeWalkerFilter;
import com.puppycrawl.tools.checkstyle.checks.javadoc.JavadocParagraphCheck;

/**
 * 允许 Javadoc 用独占一行的段落标签分隔正文
 *
 * @author lonelight
 */
public final class JavadocParagraphSpacingFilter extends AbstractAutomaticBean implements TreeWalkerFilter {
    /**
     * 完成无需附加配置的过滤器初始化
     */
    @Override
    protected void finishLocalSetup() {
        // 段落标签从当前诊断所在行读取
    }

    /**
     * 保留段落标签格式以外的 Javadoc 诊断
     *
     * @param event 待过滤的检查结果
     * @return 应保留诊断时为 true
     */
    @Override
    public boolean accept(TreeWalkerAuditEvent event) {
        return !JavadocParagraphCheck.class.getName().equals(event.getSourceName())
                || !JavadocParagraphCheck.MSG_LINE_BEFORE.equals(event.violation().getKey())
                || !"* <p>".equals(event.fileContents().getLine(event.getLine() - 1).strip());
    }
}
