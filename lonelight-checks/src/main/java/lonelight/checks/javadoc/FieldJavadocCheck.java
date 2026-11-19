package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 检查实例字段和 record 组件是否有说明
 *
 * @author lonelight
 */
public final class FieldJavadocCheck extends RuleCheck {
    private JavaSource source;

    /**
     * 监听变量声明和 record 组件
     *
     * @return 用于检查字段说明的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.VARIABLE_DEF, TokenTypes.RECORD_COMPONENT_DEF};
    }

    /**
     * 为当前文件准备文档注解解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 要求保留字段 Javadoc 所在的注释节点
     *
     * @return 表示需要注释节点的 true
     */
    @Override
    public boolean isCommentNodesRequired() {
        return true;
    }

    /**
     * 报告缺少说明且未显式隐藏的字段或组件
     *
     * @param node 待检查的变量声明或 record 组件
     */
    @Override
    public void visitToken(DetailAST node) {
        if (!JavaSource.isInstanceFieldOrComponent(node)) {
            return;
        }
        DetailAST name = JavaSource.declarationName(node);
        if (node.getType() == TokenTypes.RECORD_COMPONENT_DEF) {
            DetailAST owner = JavaSource.enclosingType(node);
            DetailAST typeDocumentation = owner == null ? null : JavadocUtil.getAttachedJavadocComment(owner);
            if (typeDocumentation != null && java.util.regex.Pattern.compile("(?m)@param\\s+"
                    + java.util.regex.Pattern.quote(name.getText()) + "\\s+[^\\s*]+")
                    .matcher(JavadocUtil.getJavadocCommentContent(typeDocumentation)).find()) {
                return;
            }
        }
        for (DetailAST annotation : JavaSource.annotations(node)) {
            if (source.isType(JavaSource.annotationName(annotation), "com.fasterxml.jackson.annotation.JsonIgnore")) {
                DetailAST value = JavaSource.attribute(annotation, "value");
                if (value == null || value.findFirstToken(TokenTypes.LITERAL_TRUE) != null) {
                    return;
                }
            }
            if (source.isType(JavaSource.annotationName(annotation), "io.swagger.v3.oas.annotations.media.Schema")) {
                DetailAST hidden = JavaSource.attribute(annotation, "hidden");
                DetailAST descriptionValue = JavaSource.attribute(annotation, "description");
                String description = JavaSource.literalText(descriptionValue);
                if (hidden != null && hidden.findFirstToken(TokenTypes.LITERAL_TRUE) != null
                        || descriptionValue != null && (description == null || !description.isBlank())) {
                    return;
                }
            }
        }
        var documentation = JavadocUtil.getAttachedJavadocComment(node);
        if (documentation == null || JavadocUtil.getJavadocCommentContent(documentation)
                .replaceAll("/\\*\\*|\\*/|(?m)^\\s*\\*", "").isBlank()) {
            log(name, "field.javadoc");
        }
    }
}
