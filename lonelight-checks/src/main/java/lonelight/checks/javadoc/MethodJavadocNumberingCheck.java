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
    private static final Pattern STEP_PREFIX = Pattern.compile(
            "\\d++(?:[、)）]|\\.(?!\\d))|[（(]\\d++[)）]|[一二三四五六七八九十]++、");

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
            String text = prose.startsWith("<p>") ? prose.substring("<p>".length()).stripLeading() : prose;
            var prefix = STEP_PREFIX.matcher(text);
            if (prefix.lookingAt() && prefix.end() < text.length()) {
                log(lineNumber, "javadoc.numbering");
            }
        });
    }
}
