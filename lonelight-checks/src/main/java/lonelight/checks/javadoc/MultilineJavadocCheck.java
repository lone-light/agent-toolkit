package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import lonelight.checks.RuleCheck;

/**
 * 检查 Javadoc 是否展开且结束标记独占一行
 *
 * @author lonelight
 */
public final class MultilineJavadocCheck extends RuleCheck {
    /**
     * 监听可能包含 Javadoc 的块注释
     *
     * @return 块注释开始节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.BLOCK_COMMENT_BEGIN};
    }

    /**
     * 要求语法树保留块注释
     *
     * @return 表示需要注释节点的 true
     */
    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    /**
     * 报告未展开或结束标记未独占一行的 Javadoc
     *
     * @param comment 待检查的块注释节点
     */
    @Override
    public void visitToken(DetailAST comment) {
        if (!JavadocUtil.isJavadocComment(comment)) {
            return;
        }
        String content = JavadocUtil.getBlockCommentContent(comment);
        int lastBreak = content.lastIndexOf('\n');
        if (comment.getLastChild().getLineNo() - comment.getLineNo() < 2
                || !content.substring(lastBreak + 1).isBlank()) {
            log(comment, "javadoc.multiline");
        }
    }
}
