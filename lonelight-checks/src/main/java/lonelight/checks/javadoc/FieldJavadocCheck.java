package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.regex.Pattern;

/**
 * 检查实例字段和 record 组件是否有说明
 *
 * @author lonelight
 */
public final class FieldJavadocCheck extends RuleCheck {
    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 检查 record 类型的 Javadoc 是否说明了当前组件
     *
     * @param nodeDetailAST 待检查的声明节点
     * @return 存在同名有效参数说明时为 true
     */
    private static boolean hasRecordDescription(DetailAST nodeDetailAST) {
        if (nodeDetailAST.getType() != TokenTypes.RECORD_COMPONENT_DEF) {
            return false;
        }
        var owner = JavaSource.enclosingType(nodeDetailAST);
        var documentation = owner == null ? null : JavadocUtil.getAttachedJavadocComment(owner);
        if (documentation == null) {
            return false;
        }
        String name = JavaSource.declarationName(nodeDetailAST).getText();
        return Pattern.compile("@param\\s+" + Pattern.quote(name) + "\\s+[^\\s*]+")
                .matcher(JavadocUtil.getJavadocCommentContent(documentation)).find();
    }

    /**
     * 判断字段是否有非空 Javadoc
     *
     * @param nodeDetailAST 待检查的字段
     * @return Javadoc 中包含内容时为 true
     */
    private static boolean hasFieldJavadoc(DetailAST nodeDetailAST) {
        var documentation = JavadocUtil.getAttachedJavadocComment(nodeDetailAST);
        return documentation != null && !JavadocUtil.getJavadocCommentContent(documentation)
                .replaceAll("/\\*\\*|\\*/|(?m)^\\s*\\*", "").isBlank();
    }

    /**
     * 监听变量声明和 record 组件
     *
     * @return 用于检查字段说明的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.VARIABLE_DEF, TokenTypes.RECORD_COMPONENT_DEF};
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
     * @param nodeDetailAST 待检查的变量声明或 record 组件
     */
    @Override
    public void visitToken(DetailAST nodeDetailAST) {
        if (JavaSource.isInstanceFieldOrComponent(nodeDetailAST)
                && !hasRecordDescription(nodeDetailAST)
                && JavaSource.annotations(nodeDetailAST).stream().noneMatch(this::documentsOrHides)
                && !hasFieldJavadoc(nodeDetailAST)) {
            log(JavaSource.declarationName(nodeDetailAST), "field.javadoc");
        }
    }

    /**
     * 判断字段注解是否提供说明或将字段隐藏
     *
     * @param annotationDetailAST 字段注解
     * @return 注解提供有效说明或隐藏字段时为 true
     */
    private boolean documentsOrHides(DetailAST annotationDetailAST) {
        String name = JavaSource.annotationName(annotationDetailAST);
        if (source.isType(name, "com.fasterxml.jackson.annotation.JsonIgnore")) {
            var value = JavaSource.attribute(annotationDetailAST, "value");
            return value == null || value.findFirstToken(TokenTypes.LITERAL_TRUE) != null;
        }
        if (!source.isType(name, "io.swagger.v3.oas.annotations.media.Schema")) {
            return false;
        }
        var hidden = JavaSource.attribute(annotationDetailAST, "hidden");
        var descriptionValue = JavaSource.attribute(annotationDetailAST, "description");
        String description = JavaSource.literalText(descriptionValue);
        return hidden != null && hidden.findFirstToken(TokenTypes.LITERAL_TRUE) != null
                || descriptionValue != null && (description == null || !description.isBlank());
    }
}
