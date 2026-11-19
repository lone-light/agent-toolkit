package lonelight.checks.numeric;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.JavaVariables;
import lonelight.checks.RuleCheck;

import java.util.Set;

/**
 * 报告能从源码确认使用浮点参数的 BigDecimal 构造
 *
 * @author lonelight
 */
public final class BigDecimalFloatingConstructorCheck extends RuleCheck {
    private static final String BIG_DECIMAL = "java.math.BigDecimal";
    private static final Set<String> INTEGRAL_WRAPPERS = Set.of(
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Character");

    /**
     * 当前文件的类型名称索引
     */
    private JavaSource javaSource;
    /**
     * 当前文件的变量作用域索引
     */
    private JavaVariables javaVariables;

    /**
     * 监听对象创建表达式
     *
     * @return 对象创建节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.LITERAL_NEW};
    }

    /**
     * 为当前文件建立类型和变量索引
     *
     * @param rootDetailAST 当前语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        javaSource = new JavaSource(rootDetailAST);
        javaVariables = new JavaVariables(rootDetailAST);
    }

    /**
     * 检查 BigDecimal 构造器首个参数的数值类别
     *
     * @param creationDetailAST 对象创建节点
     */
    @Override
    public void visitToken(DetailAST creationDetailAST) {
        var arguments = creationDetailAST.findFirstToken(TokenTypes.ELIST);
        if (arguments != null && arguments.hasChildren()
                && javaSource.isKnownType(JavaSource.typeName(creationDetailAST), BIG_DECIMAL)
                && numberKind(arguments.getFirstChild()) == NumberKind.FLOATING) {
            log(creationDetailAST, "decimal.floating.constructor");
        }
    }

    /**
     * 识别可由源代码明确判断的数值表达式
     *
     * @param valueDetailAST 参数表达式或运算数
     * @return 所属数值类别或无法判断
     */
    private NumberKind numberKind(DetailAST valueDetailAST) {
        return switch (valueDetailAST.getType()) {
            case TokenTypes.NUM_FLOAT, TokenTypes.NUM_DOUBLE -> NumberKind.FLOATING;
            case TokenTypes.NUM_INT, TokenTypes.NUM_LONG, TokenTypes.CHAR_LITERAL -> NumberKind.INTEGRAL;
            case TokenTypes.EXPR, TokenTypes.UNARY_PLUS, TokenTypes.UNARY_MINUS -> numberKind(valueDetailAST.getFirstChild());
            case TokenTypes.TYPECAST -> declaredKind(valueDetailAST.findFirstToken(TokenTypes.TYPE));
            case TokenTypes.PLUS, TokenTypes.MINUS, TokenTypes.STAR, TokenTypes.DIV, TokenTypes.MOD -> {
                var left = numberKind(valueDetailAST.getFirstChild());
                var right = numberKind(valueDetailAST.getLastChild());
                yield numericPromotion(left, right);
            }
            case TokenTypes.IDENT, TokenTypes.DOT -> variableKind(valueDetailAST);
            default -> NumberKind.UNKNOWN;
        };
    }

    /**
     * 根据直接可见的变量声明判断数值类别
     *
     * @param valueDetailAST 变量引用节点
     * @return 已确认的数值类别或无法判断
     */
    private NumberKind variableKind(DetailAST valueDetailAST) {
        boolean explicitThis = valueDetailAST.getType() == TokenTypes.DOT;
        if (explicitThis && (valueDetailAST.getFirstChild().getType() != TokenTypes.LITERAL_THIS
                || valueDetailAST.getLastChild().getType() != TokenTypes.IDENT)) {
            return NumberKind.UNKNOWN;
        }
        String name = explicitThis ? valueDetailAST.getLastChild().getText() : valueDetailAST.getText();
        var declaration = javaVariables.find(valueDetailAST, name, explicitThis);
        return declaration == null || declaration.getType() == TokenTypes.PATTERN_VARIABLE_DEF
                ? NumberKind.UNKNOWN : declaredKind(declaration.findFirstToken(TokenTypes.TYPE));
    }

    /**
     * 识别显式声明的原始数值类型和标准包装类型
     *
     * @param typeDetailAST 类型节点，可为空
     * @return 已确认的数值类别或无法判断
     */
    private NumberKind declaredKind(DetailAST typeDetailAST) {
        if (typeDetailAST == null || !typeDetailAST.hasChildren()
                || typeDetailAST.findFirstToken(TokenTypes.ARRAY_DECLARATOR) != null) {
            return NumberKind.UNKNOWN;
        }
        var primitive = switch (typeDetailAST.getLastChild().getType()) {
            case TokenTypes.LITERAL_FLOAT, TokenTypes.LITERAL_DOUBLE -> NumberKind.FLOATING;
            case TokenTypes.LITERAL_BYTE, TokenTypes.LITERAL_SHORT, TokenTypes.LITERAL_CHAR,
                 TokenTypes.LITERAL_INT, TokenTypes.LITERAL_LONG -> NumberKind.INTEGRAL;
            default -> NumberKind.UNKNOWN;
        };
        if (primitive != NumberKind.UNKNOWN) {
            return primitive;
        }
        String name = JavaSource.typeName(typeDetailAST);
        if (javaSource.isType(name, "java.lang.Float") || javaSource.isType(name, "java.lang.Double")) {
            return NumberKind.FLOATING;
        }
        return INTEGRAL_WRAPPERS.stream().anyMatch(type -> javaSource.isType(name, type))
                ? NumberKind.INTEGRAL : NumberKind.UNKNOWN;
    }

    /**
     * 仅在两边均为已识别数值时应用二元数值提升
     *
     * @param leftNumberKind  左操作数类别
     * @param rightNumberKind 右操作数类别
     * @return 运算结果类别或无法判断
     */
    private static NumberKind numericPromotion(NumberKind leftNumberKind, NumberKind rightNumberKind) {
        if (leftNumberKind == NumberKind.UNKNOWN || rightNumberKind == NumberKind.UNKNOWN) {
            return NumberKind.UNKNOWN;
        }
        return leftNumberKind == NumberKind.FLOATING || rightNumberKind == NumberKind.FLOATING
                ? NumberKind.FLOATING : NumberKind.INTEGRAL;
    }

    /**
     * 区分浮点、整数和未解析的类型
     *
     * @author lonelight
     */
    private enum NumberKind {
        FLOATING, INTEGRAL, UNKNOWN
    }
}
