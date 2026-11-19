package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.regex.Pattern;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 报告 REST 接口返回类型中的持久化实体
 *
 * @author lonelight
 */
public final class ControllerEntityReturnCheck extends RuleCheck {
    private Pattern entityPattern = Pattern.compile(".*\\.entity\\..*");
    private JavaSource source;

    /**
     * 供 Checkstyle 从 XML 配置持久化实体的识别规则
     *
     * @param value 匹配实体全限定名的正则表达式
     */
    @SuppressWarnings("unused")
    public void setEntityPattern(String value) {
        entityPattern = Pattern.compile(value);
    }

    /**
     * 监听接口候选方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.METHOD_DEF};
    }

    /**
     * 为当前文件准备返回类型解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 报告 REST 接口返回类型中可识别的实体
     *
     * @param method 待检查的方法声明
     */
    @Override
    public void visitToken(DetailAST method) {
        var returnType = method.findFirstToken(TokenTypes.TYPE);
        if (returnType != null && FrameworkMethods.isRestEndpoint(source, method)) {
            var match = source.firstMatchingType(returnType, entityPattern);
            if (match != null) {
                log(match.token(), "controller.entity.return");
            }
        }
    }
}
