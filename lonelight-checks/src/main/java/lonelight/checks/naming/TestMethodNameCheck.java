package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;
import lonelight.checks.framework.FrameworkMethods;

/**
 * 检查 JUnit 方法名是否符合三段命名约定
 *
 * @author lonelight
 */
public final class TestMethodNameCheck extends RuleCheck {
    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 监听可能声明 JUnit 用例的方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_DEF};
    }

    /**
     * 为当前文件准备 JUnit 注解识别
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = indexSource(root);
    }

    /**
     * 报告不符合三段命名约定的 JUnit 方法
     *
     * @param node 待检查的方法声明
     */
    @Override
    public void visitToken(DetailAST node) {
        DetailAST name = JavaSource.declarationName(node);
        if (FrameworkMethods.isJUnitTest(source, node)
                && !name.getText().matches("[a-z][a-zA-Z0-9]*_[a-zA-Z0-9]+_[a-zA-Z0-9]+")) {
            log(name, "test.method.name");
        }
    }
}
