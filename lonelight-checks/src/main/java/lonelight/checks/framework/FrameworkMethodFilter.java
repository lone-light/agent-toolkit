package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.TreeWalkerAuditEvent;
import com.puppycrawl.tools.checkstyle.TreeWalkerFilter;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import lonelight.checks.JavaSource;

/**
 * 按框架注解过滤方法命名和文档诊断
 *
 * @author lonelight
 */
public final class FrameworkMethodFilter extends AbstractAutomaticBean implements TreeWalkerFilter {
    /**
     * 完成无需附加配置的过滤器初始化
     */
    @Override
    protected void finishLocalSetup() { }

    /**
     * 按方法所用的框架注解过滤对应诊断
     *
     * @param event 待处理的扫描事件
     * @return 应保留诊断时为 true
     */
    @Override
    public boolean accept(TreeWalkerAuditEvent event) {
        String check = event.getSourceName();
        boolean naming = check.endsWith(".MethodNameCheck");
        boolean missing = check.endsWith(".MissingJavadocMethodCheck");
        boolean documentation = check.endsWith(".JavadocMethodCheck");
        if (!naming && !missing && !documentation) {
            return true;
        }
        DetailAST target = null;
        for (DetailAST method : JavaSource.descendants(event.rootAst(), TokenTypes.METHOD_DEF)) {
            DetailAST comment = naming ? null : JavadocUtil.getAttachedJavadocComment(method);
            if (contains(event, comment == null ? method : comment, lastToken(method))) {
                // 先访问外层方法，因此后匹配的局部类方法优先
                target = method;
            }
        }
        return target == null || !exempt(target, event.rootAst(), naming, missing);
    }

    /**
     * 判断诊断位置是否落在两个节点之间
     *
     * @param event 待定位的扫描事件
     * @param start 范围起点，包含该位置
     * @param end 范围终点，包含该位置
     * @return 位置在范围内时为 true
     */
    private static boolean contains(TreeWalkerAuditEvent event, DetailAST start, DetailAST end) {
        int line = event.getLine();
        int column = event.getColumnCharIndex();
        return (line > start.getLineNo() || line == start.getLineNo() && column >= start.getColumnNo())
                && (line < end.getLineNo() || line == end.getLineNo() && column <= end.getColumnNo());
    }

    /**
     * 判断框架方法是否适用当前规则的豁免
     *
     * @param method 待判断的方法声明
     * @param root 当前文件的语法树根节点
     * @param naming 当前诊断是否来自方法命名检查
     * @param missing 当前诊断是否来自缺少 Javadoc 检查
     * @return 可以豁免时为 true
     */
    private static boolean exempt(DetailAST method, DetailAST root, boolean naming, boolean missing) {
        JavaSource source = new JavaSource(root);
        return FrameworkMethods.isJUnitTest(source, method)
                || !naming && source.hasAnnotation(method, "java.lang.Override")
                || missing && FrameworkMethods.hasHttpMapping(source, method);
    }

    /**
     * 沿最末分支定位语法树末端
     *
     * @param node 子树根节点
     * @return 最末分支的叶子节点
     */
    private static DetailAST lastToken(DetailAST node) {
        return node.getLastChild() == null ? node : lastToken(node.getLastChild());
    }
}
