package lonelight.checks.concurrent;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.JavaVariables;
import lonelight.checks.RuleCheck;

import java.util.Set;

/**
 * 报告队列或线程数缺少明确上限的传统线程池工厂
 *
 * @author lonelight
 */
public final class ExecutorFactoryCheck extends RuleCheck {
    private static final String OWNER = "java.util.concurrent.Executors";
    private static final Set<String> FACTORIES = Set.of("newFixedThreadPool", "newSingleThreadExecutor",
            "newCachedThreadPool", "newScheduledThreadPool", "newSingleThreadScheduledExecutor");

    /**
     * 当前文件的导入及类型名称
     */
    private JavaSource javaSource;
    /**
     * 当前文件的变量作用域索引
     */
    private JavaVariables javaVariables;

    /**
     * 检查外层类中是否声明了同名方法
     *
     * @param usageDetailAST 调用节点
     * @param name           目标方法名
     * @return 存在本地声明时为 true
     */
    private static boolean hasLocalMethod(DetailAST usageDetailAST, String name) {
        for (var ownerDetailAST = JavaSource.enclosingType(usageDetailAST); ownerDetailAST != null;
             ownerDetailAST = JavaSource.enclosingType(ownerDetailAST)) {
            var members = JavaSource.children(ownerDetailAST.findFirstToken(TokenTypes.OBJBLOCK));
            if (members.stream().anyMatch(member -> member.getType() == TokenTypes.METHOD_DEF
                    && JavaSource.declarationName(member).getText().equals(name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 监听调用及方法引用
     *
     * @return 对应的 Java 节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_CALL, TokenTypes.METHOD_REF};
    }

    /**
     * 为当前文件建立名称索引
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        javaSource = new JavaSource(rootDetailAST);
        javaVariables = new JavaVariables(rootDetailAST);
    }

    /**
     * 检查能定位到 JDK Executors 的工厂调用或引用
     *
     * @param callDetailAST 待检查的调用或方法引用
     */
    @Override
    public void visitToken(DetailAST callDetailAST) {
        var targetDetailAST = callDetailAST.getType() == TokenTypes.METHOD_REF
                ? callDetailAST : callDetailAST.getFirstChild();
        var nameDetailAST = targetDetailAST.getType() == TokenTypes.IDENT
                ? targetDetailAST : targetDetailAST.getLastChild();
        if (nameDetailAST == null || !FACTORIES.contains(nameDetailAST.getText())) {
            return;
        }
        String method = nameDetailAST.getText();
        if (targetDetailAST.getType() == TokenTypes.IDENT
                ? javaSource.hasStaticImport(OWNER, method) && !hasLocalMethod(callDetailAST, method)
                : isExecutors(targetDetailAST.getFirstChild(), callDetailAST)) {
            log(nameDetailAST, "executor.factory", method);
        }
    }

    /**
     * 识别限定器是 Executors 类型而不是同名变量
     *
     * @param qualifierDetailAST 方法访问的限定器
     * @param usageDetailAST     调用节点
     * @return 能识别为 JDK Executors 时为 true
     */
    private boolean isExecutors(DetailAST qualifierDetailAST, DetailAST usageDetailAST) {
        int kind = qualifierDetailAST.getType();
        if (kind != TokenTypes.IDENT && kind != TokenTypes.DOT) {
            return false;
        }
        String name = FullIdent.createFullIdent(qualifierDetailAST).getText();
        return javaSource.isKnownType(name, OWNER)
                && (kind != TokenTypes.IDENT || javaVariables.find(usageDetailAST, name, false) == null);
    }
}
