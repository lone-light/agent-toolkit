package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 检查布尔字段和 record 组件的 is 前缀
 *
 * @author lonelight
 */
public final class BooleanFieldNameCheck extends RuleCheck {
    private JavaSource source;

    /**
     * 监听变量声明和 record 组件
     *
     * @return 用于检查布尔属性命名的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.VARIABLE_DEF, TokenTypes.RECORD_COMPONENT_DEF};
    }

    /**
     * 为当前文件准备布尔类型识别
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 报告带有 is 前缀的布尔属性名
     *
     * @param node 待检查的变量声明或 record 组件
     */
    @Override
    public void visitToken(DetailAST node) {
        if (!JavaSource.isInstanceFieldOrComponent(node)) {
            return;
        }
        DetailAST type = node.findFirstToken(TokenTypes.TYPE);
        String written = JavaSource.typeName(type);
        DetailAST name = JavaSource.declarationName(node);
        if (type != null && type.findFirstToken(TokenTypes.ARRAY_DECLARATOR) == null
                && (type.findFirstToken(TokenTypes.LITERAL_BOOLEAN) != null || source.isType(written, "java.lang.Boolean"))
                && name.getText().matches("is[A-Z].*")) {
            log(name, "boolean.field.name");
        }
    }
}
