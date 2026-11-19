package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import lonelight.checks.RuleCheck;

import java.util.regex.Pattern;

/**
 * 报告方法 Javadoc 正文开头的步骤编号
 *
 * @author lonelight
 */
public final class MethodJavadocNumberingCheck extends RuleCheck {
    private static final Pattern NUMBERED_STEP = Pattern.compile(
            "^(?:<p>)?\\s*(?:[0-9]+(?:[、)）]|\\.(?![0-9]))|[（(][0-9]+[)）]|[一二三四五六七八九十]+、)\\s*.+");

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
     * 报告正文开头的步骤编号
     *
     * @param detailAST 待检查的方法或构造器声明
     */
    @Override
    public void visitToken(DetailAST detailAST) {
        MethodDocumentation.forEachProseLine(detailAST, (lineNumber, prose) -> {
            if (NUMBERED_STEP.matcher(prose).matches()) {
                log(lineNumber, "javadoc.numbering");
            }
        });
    }
}
