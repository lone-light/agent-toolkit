package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.JavaVariables;
import lonelight.checks.RuleCheck;

import java.util.Set;

/**
 * 报告 SLF4J 日志消息中的动态字符串拼接
 *
 * @author lonelight
 */
public final class LoggingPlaceholderCheck extends RuleCheck {
    private static final Set<String> LEVELS = Set.of("trace", "debug", "info", "warn", "error");

    /**
     * 当前文件的名称索引
     */
    private JavaSource source;
    /**
     * 当前文件的变量作用域索引
     */
    private JavaVariables javaVariables;

    /**
     * 读取变量名或通过 this 访问的字段名
     *
     * @param receiverDetailAST 调用接收者节点
     * @return 接收者名称，无法识别时为空字符串
     */
    private static String receiverName(DetailAST receiverDetailAST) {
        if (receiverDetailAST.getType() == TokenTypes.IDENT) {
            return receiverDetailAST.getText();
        }
        if (receiverDetailAST.getType() == TokenTypes.DOT
                && receiverDetailAST.getFirstChild().getType() == TokenTypes.LITERAL_THIS) {
            return receiverDetailAST.getLastChild().getText();
        }
        return "";
    }

    /**
     * 监听日志检查所需的方法调用
     *
     * @return 方法调用节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_CALL};
    }

    /**
     * 收集当前文件中的变量和参数声明
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = indexSource(root);
        javaVariables = new JavaVariables(root);
    }

    /**
     * 报告 SLF4J 日志消息中的动态字符串拼接
     *
     * @param callDetailAST 待检查的方法调用
     */
    @Override
    public void visitToken(DetailAST callDetailAST) {
        var dot = callDetailAST.findFirstToken(TokenTypes.DOT);
        if (dot == null || !LEVELS.contains(dot.getLastChild().getText())) {
            return;
        }
        var receiver = dot.getFirstChild();
        String name = receiverName(receiver);
        if (name.isEmpty() || !isLogger(callDetailAST, name, receiver.getType() == TokenTypes.DOT)) {
            return;
        }
        var message = messageArgument(callDetailAST);
        if (JavaSource.descendants(message, TokenTypes.PLUS).stream().anyMatch(plus ->
                hasStringOperand(plus) && hasValueIdentifier(plus))) {
            log(message, "logging.placeholder");
        }
    }

    /**
     * 识别加法链中的字面量字符串操作数
     *
     * @param expression 加法表达式或其操作数
     * @return 加法链直接连接字符串字面量时为 true
     */
    private boolean hasStringOperand(DetailAST expression) {
        return switch (expression.getType()) {
            case TokenTypes.STRING_LITERAL, TokenTypes.TEXT_BLOCK_LITERAL_BEGIN -> true;
            case TokenTypes.PLUS -> JavaSource.children(expression).stream().anyMatch(this::hasStringOperand);
            case TokenTypes.QUESTION -> {
                var condition = JavaSource.firstOperand(expression);
                yield JavaSource.children(expression).stream()
                        .anyMatch(child -> child != condition && hasStringOperand(child));
            }
            case TokenTypes.TYPECAST -> source.isType(JavaSource.typeName(expression.findFirstToken(TokenTypes.TYPE)), "java.lang.String")
                    && hasStringOperand(JavaSource.lastOperand(expression));
            default -> false;
        };
    }

    /**
     * 判断表达式是否使用值或调用名称，跳过类型转换的类型名
     *
     * @param expression 待检查的表达式
     * @return 表达式包含值或调用名称时为 true
     */
    private static boolean hasValueIdentifier(DetailAST expression) {
        if (expression.getType() == TokenTypes.TYPE) {
            return false;
        }
        return expression.getType() == TokenTypes.IDENT
                || JavaSource.children(expression).stream().anyMatch(LoggingPlaceholderCheck::hasValueIdentifier);
    }

    /**
     * 读取日志消息参数并跳过开头的 SLF4J Marker
     *
     * @param callDetailAST 日志方法调用
     * @return 消息表达式，缺失时为 null
     */
    private DetailAST messageArgument(DetailAST callDetailAST) {
        var arguments = callDetailAST.findFirstToken(TokenTypes.ELIST);
        var first = arguments == null ? null : arguments.getFirstChild();
        if (first == null) {
            return null;
        }
        var argument = JavaSource.firstOperand(first);
        String name = receiverName(argument);
        if (name.isEmpty()) {
            return first;
        }
        var marker = javaVariables.find(callDetailAST, name, argument.getType() == TokenTypes.DOT);
        if (marker == null || marker.getType() == TokenTypes.PATTERN_VARIABLE_DEF
                || !source.isType(JavaSource.typeName(marker.findFirstToken(TokenTypes.TYPE)), "org.slf4j.Marker")) {
            return first;
        }
        var next = first.getNextSibling();
        while (next != null && next.getType() != TokenTypes.EXPR) {
            next = next.getNextSibling();
        }
        return next;
    }

    /**
     * 判断调用的接收者是否为 SLF4J Logger
     *
     * @param call         待检查的调用节点
     * @param name         调用接收者的名称
     * @param explicitThis 是否显式通过 this 访问字段
     * @return 能识别为 SLF4J Logger 时为 true
     */
    private boolean isLogger(DetailAST call, String name, boolean explicitThis) {
        DetailAST selected = javaVariables.find(call, name, explicitThis);
        if (selected != null) {
            DetailAST type = selected.findFirstToken(TokenTypes.TYPE);
            return selected.getType() != TokenTypes.PATTERN_VARIABLE_DEF
                    && type != null && source.isType(JavaSource.typeName(type), "org.slf4j.Logger");
        }
        DetailAST owner = JavaSource.enclosingType(call);
        return name.equals("log") && owner != null && source.hasKnownAnnotation(owner, "lombok.extern.slf4j.Slf4j");
    }
}
