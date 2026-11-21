package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.*;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import com.puppycrawl.tools.checkstyle.utils.ScopeUtil;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.HashSet;
import java.util.Set;

/**
 * 检查类型 Javadoc 的单行摘要和作者标签
 *
 * @author lonelight
 */
public final class TypeJavadocCheck extends RuleCheck {
    /**
     * 收集正文可见文字所在的源码行
     *
     * @param detailNode Javadoc 节点，可为空
     * @param lines      待更新的行号集合
     */
    private static void collectTextLines(DetailNode detailNode, Set<Integer> lines) {
        if (detailNode == null) {
            return;
        }
        int type = detailNode.getType();
        if (type == JavadocCommentsTokenTypes.JAVADOC_INLINE_TAG
                || type == JavadocCommentsTokenTypes.TEXT && !detailNode.getText().isBlank()) {
            lines.add(detailNode.getLineNumber());
            return;
        }
        if (type == JavadocCommentsTokenTypes.TEXT || type == JavadocCommentsTokenTypes.HTML_COMMENT
                || type == JavadocCommentsTokenTypes.HTML_TAG_START || type == JavadocCommentsTokenTypes.HTML_TAG_END) {
            return;
        }
        for (DetailNode childDetailNode = detailNode.getFirstChild(); childDetailNode != null;
             childDetailNode = childDetailNode.getNextSibling()) {
            collectTextLines(childDetailNode, lines);
        }
    }

    /**
     * 监听可附加类型 Javadoc 的声明
     *
     * @return 类型声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{
                TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.ENUM_DEF,
                TokenTypes.ANNOTATION_DEF, TokenTypes.RECORD_DEF,
        };
    }

    /**
     * 保留可附加到类型上的文档注释
     *
     * @return 需要注释节点时为 true
     */
    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    /**
     * 报告缺少单行摘要或非空作者的类型文档
     *
     * @param typeDetailAST 待检查的类型声明
     */
    @Override
    public void visitToken(DetailAST typeDetailAST) {
        if (JavaSource.annotations(typeDetailAST).stream().anyMatch(annotation ->
                JavaSource.simpleName(JavaSource.annotationName(annotation)).equals("Generated"))) {
            return;
        }
        var name = JavaSource.declarationName(typeDetailAST);
        var comment = JavadocUtil.getAttachedJavadocComment(typeDetailAST);
        if (comment == null) {
            // 公开类型的缺失诊断由通用配置报告
            if (ScopeUtil.getSurroundingScope(typeDetailAST).filter(scope -> !scope.isIn(Scope.PUBLIC)).isPresent()) {
                log(name, "javadoc.type.missing");
            }
            return;
        }
        DetailNode docDetailNode = new JavadocDetailNodeParser().parseJavadocComment(comment).getTree();
        if (docDetailNode == null) {
            return;
        }
        Set<Integer> summaryLines = new HashSet<>();
        for (DetailNode detailNode = docDetailNode.getFirstChild(); detailNode != null; detailNode = detailNode.getNextSibling()) {
            if (detailNode.getType() == JavadocCommentsTokenTypes.JAVADOC_BLOCK_TAG) {
                break;
            }
            collectTextLines(detailNode, summaryLines);
        }
        if (summaryLines.size() != 1) {
            log(name, "javadoc.type.summary");
        }
        var authors = JavadocUtil.getAllNodesOfType(docDetailNode, JavadocCommentsTokenTypes.JAVADOC_BLOCK_TAG).stream()
                .map(DetailNode::getFirstChild).filter(detailNode -> detailNode.getType() == JavadocCommentsTokenTypes.AUTHOR_BLOCK_TAG)
                .toList();
        if (authors.isEmpty() || authors.stream().anyMatch(author -> {
            var description = JavadocUtil.findFirstToken(author, JavadocCommentsTokenTypes.DESCRIPTION);
            Set<Integer> lines = new HashSet<>();
            collectTextLines(description, lines);
            return lines.isEmpty();
        })) {
            log(name, "javadoc.type.author");
        }
    }
}
