package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import lonelight.checks.RuleCheck;

/**
 * 报告方法 Javadoc 正文行末的中英文句号
 *
 * @author lonelight
 */
public final class MethodJavadocPeriodCheck extends RuleCheck {
    /**
     * 监听方法和构造器声明
     *
     * @return 检查方法文档所需的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return MethodDocumentation.tokens();
    }

    /**
     * 要求语法树保留注释
     *
     * @return 表示需要注释节点的 true
     */
    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    /**
     * 报告正文行末的中英文句号
     *
     * @param detailAST 待检查的方法或构造器声明
     */
    @Override
    public void visitToken(DetailAST detailAST) {
        MethodDocumentation.forEachProseLine(detailAST, (lineNumber, prose) -> {
            String visible = prose.replaceAll("<[^>]+>", "").strip();
            if (visible.matches(".*[.。]$")) {
                log(lineNumber, "javadoc.period");
            }
        });
    }
}
