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
     * 当前文件的类型与注解索引
     */
    private JavaSource source;

    /**
     * 判断两个节点限定的范围是否包含诊断位置
     *
     * @param event 待定位的扫描事件
     * @param start 范围起点，包含该位置
     * @param end   范围终点，包含该位置
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
     * @param method  待判断的方法声明
     * @param naming  当前诊断是否来自方法命名检查
     * @param missing 当前诊断是否来自缺少 Javadoc 检查
     * @return 可以豁免时为 true
     */
    private boolean exempt(DetailAST method, boolean naming, boolean missing) {
        return FrameworkMethods.isJUnitTest(source, method)
                || !naming && source.hasKnownAnnotation(method, "java.lang.Override")
                || missing && FrameworkMethods.hasHttpMapping(source, method);
    }

    /**
     * 完成无需附加配置的过滤器初始化
     */
    @Override
    protected void finishLocalSetup() {
        // 方法注解在扫描时读取，无需预先加载
    }

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
        for (DetailAST method : JavaSource.descendants(event.rootAst(), TokenTypes.METHOD_DEF)) {
            DetailAST comment = naming ? null : JavadocUtil.getAttachedJavadocComment(method);
            // 最后一个直接子节点指向方法体的左花括号或抽象方法的分号
            if (contains(event, comment == null ? method : comment, method.getLastChild())) {
                source = JavaSource.forEvent(event, source);
                return !exempt(method, naming, missing);
            }
        }
        return true;
    }
}
