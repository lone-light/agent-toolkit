package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Set;
import lonelight.checks.RuleCheck;

/**
 * 报告通过全限定名调用 BeanUtils 的写法
 *
 * @author lonelight
 */
public final class BeanUtilsCallCheck extends RuleCheck {
    private static final Set<String> FORBIDDEN = Set.of("org.springframework.beans.BeanUtils",
            "org.apache.commons.beanutils.BeanUtils");

    /**
     * 监听方法调用
     *
     * @return 方法调用节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.METHOD_CALL};
    }

    /**
     * 报告通过全限定名调用 BeanUtils 的写法
     *
     * @param call 待检查的方法调用
     */
    @Override
    public void visitToken(DetailAST call) {
        DetailAST dot = call.findFirstToken(TokenTypes.DOT);
        if (dot != null) {
            String receiver = FullIdent.createFullIdent(dot.getFirstChild()).getText();
            if (FORBIDDEN.contains(receiver)) {
                log(call, "bean.utils.call");
            }
        }
    }
}
