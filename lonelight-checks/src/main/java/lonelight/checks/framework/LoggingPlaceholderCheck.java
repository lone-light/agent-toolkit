package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 报告 SLF4J 日志消息中的动态字符串拼接
 *
 * @author lonelight
 */
public final class LoggingPlaceholderCheck extends RuleCheck {
    private static final Set<String> LEVELS = Set.of("trace", "debug", "info", "warn", "error");
    private JavaSource source;
    private List<DetailAST> declarations = List.of();

    /**
     * 监听日志检查所需的方法调用
     *
     * @return 方法调用节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.METHOD_CALL};
    }

    /**
     * 收集当前文件中的变量和参数声明
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
        declarations = new ArrayList<>(JavaSource.descendants(root, TokenTypes.VARIABLE_DEF));
        declarations.addAll(JavaSource.descendants(root, TokenTypes.PARAMETER_DEF));
    }

    /**
     * 报告 SLF4J 日志消息中的动态字符串拼接
     *
     * @param call 待检查的方法调用
     */
    @Override
    public void visitToken(DetailAST call) {
        DetailAST dot = call.findFirstToken(TokenTypes.DOT);
        if (dot == null || !LEVELS.contains(dot.getLastChild().getText())) {
            return;
        }
        DetailAST receiver = dot.getFirstChild();
        String receiverName = receiver.getType() == TokenTypes.IDENT ? receiver.getText()
                : receiver.getType() == TokenTypes.DOT && receiver.getFirstChild().getType() == TokenTypes.LITERAL_THIS
                ? receiver.getLastChild().getText() : "";
        if (!isLogger(call, receiverName, receiver.getType() == TokenTypes.DOT)) {
            return;
        }
        DetailAST arguments = call.findFirstToken(TokenTypes.ELIST);
        if (arguments == null) {
            return;
        }
        DetailAST expression = arguments.getFirstChild();
        if (expression != null && expression.getFirstChild().getType() == TokenTypes.IDENT) {
            DetailAST marker = findDeclaration(call, expression.getFirstChild().getText(), false);
            if (marker != null && source.isType(JavaSource.typeName(marker.findFirstToken(TokenTypes.TYPE)), "org.slf4j.Marker")) {
                do {
                    expression = expression.getNextSibling();
                } while (expression != null && expression.getType() != TokenTypes.EXPR);
            }
        }
        if (expression != null && !JavaSource.descendants(expression, TokenTypes.PLUS).isEmpty()
                && !JavaSource.descendants(expression, TokenTypes.STRING_LITERAL).isEmpty()
                && !JavaSource.descendants(expression, TokenTypes.IDENT).isEmpty()) {
            log(expression, "logging.placeholder");
        }
    }

    /**
     * 判断调用的接收者是否为 SLF4J Logger
     *
     * @param call 待检查的调用节点
     * @param name 调用接收者的名称
     * @param explicitThis 是否显式通过 this 访问字段
     * @return 能识别为 SLF4J Logger 时为 true
     */
    private boolean isLogger(DetailAST call, String name, boolean explicitThis) {
        DetailAST selected = findDeclaration(call, name, explicitThis);
        if (selected != null) {
            DetailAST type = selected.findFirstToken(TokenTypes.TYPE);
            return type != null && source.isType(JavaSource.typeName(type), "org.slf4j.Logger");
        }
        DetailAST owner = JavaSource.enclosingType(call);
        return name.equals("log") && owner != null && source.hasAnnotation(owner, "lombok.extern.slf4j.Slf4j");
    }

    /**
     * 按作用域深度定位接收者的声明
     *
     * @param call 调用节点
     * @param name 接收者名称
     * @param explicitThis 是否只查找显式访问的实例字段
     * @return 可见的匹配声明，找不到时为 null
     */
    private DetailAST findDeclaration(DetailAST call, String name, boolean explicitThis) {
        DetailAST selected = null;
        int selectedDepth = -1;
        for (DetailAST declaration : declarations) {
            if (!JavaSource.declarationName(declaration).getText().equals(name)) {
                continue;
            }
            DetailAST scope = declaration.getParent();
            boolean field = scope.getType() == TokenTypes.OBJBLOCK;
            if (explicitThis && !field) {
                continue;
            }
            if (field || scope.getType() == TokenTypes.PARAMETERS || scope.getType() == TokenTypes.FOR_EACH_CLAUSE
                    || scope.getType() == TokenTypes.FOR_INIT) {
                scope = scope.getParent();
            }
            int depth = enclosingDepth(call, scope);
            boolean beforeUse = declaration.getLineNo() < call.getLineNo()
                    || declaration.getLineNo() == call.getLineNo() && declaration.getColumnNo() < call.getColumnNo();
            if (depth > selectedDepth && (field || beforeUse)) {
                selected = declaration;
                selectedDepth = depth;
            }
        }
        return selected;
    }

    /**
     * 计算调用所在作用域在语法树中的深度
     *
     * @param call 调用节点
     * @param scope 候选作用域节点
     * @return 作用域深度，调用不在其中时为 -1
     */
    private static int enclosingDepth(DetailAST call, DetailAST scope) {
        for (DetailAST parent = call; parent != null; parent = parent.getParent()) {
            if (parent == scope) {
                int depth = 0;
                for (DetailAST owner = scope; owner != null; owner = owner.getParent()) {
                    depth++;
                }
                return depth;
            }
        }
        return -1;
    }
}
