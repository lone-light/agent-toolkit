package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.beans.Introspector;
import java.util.Set;

/**
 * 检查对象变量名是否与其显式类型匹配
 *
 * @author lonelight
 */
public final class VariableTypeNameCheck extends RuleCheck {
    /**
     * 跳过变量命名检查的类型
     */
    private Set<String> ignoredTypes = Set.of();

    /**
     * 配置中允许的类型与变量名组合
     */
    private Set<String> aliases = Set.of();

    /**
     * 是否接受以类型名结尾的带前缀变量名
     */
    private boolean allowPrefixes = true;

    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 供 Checkstyle 从 XML 配置跳过变量命名检查的类型
     *
     * @param values 免查类型的名称
     */
    @SuppressWarnings("unused")
    public void setIgnoredTypes(String... values) {
        ignoredTypes = Set.of(values);
    }

    /**
     * 设置允许代替类型名的变量别名
     *
     * @param values 采用「类型全限定名=变量名」格式的配置项
     * @throws IllegalArgumentException 别名配置格式不正确时
     */
    public void setAliases(String... values) {
        for (String value : values) {
            if (!value.matches("[^=]+=[a-zA-Z_$][a-zA-Z0-9_$]*")) {
                throw new IllegalArgumentException("aliases entries must use Type=variableName");
            }
        }
        aliases = Set.of(values);
    }

    /**
     * 供 Checkstyle 从 XML 配置变量是否允许上下文前缀
     *
     * @param value 是否允许前缀
     */
    @SuppressWarnings("unused")
    public void setAllowPrefixes(boolean value) {
        allowPrefixes = value;
    }

    /**
     * 监听需要检查类型命名的变量声明
     *
     * @return 变量、参数和 record 组件的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.VARIABLE_DEF, TokenTypes.PARAMETER_DEF, TokenTypes.RECORD_COMPONENT_DEF};
    }

    /**
     * 为当前文件准备变量类型名称解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 检查显式类型变量的名称是否与类型匹配
     *
     * @param node 待检查的变量、参数或 record 组件
     */
    @Override
    public void visitToken(DetailAST node) {
        if (JavaSource.isExplicitlyStatic(node)) {
            return;
        }
        DetailAST type = node.findFirstToken(TokenTypes.TYPE);
        if (type == null || type.findFirstToken(TokenTypes.ARRAY_DECLARATOR) != null) {
            return;
        }
        String written = JavaSource.typeName(type);
        if (written.isEmpty() || written.equals("var") || isTypeParameter(node, written)) {
            return;
        }
        if (ignoredTypes.stream().anyMatch(name -> source.isType(written, name))) {
            return;
        }
        var identifier = JavaSource.declarationName(node);
        String name = identifier.getText();
        String simple = JavaSource.simpleName(written);
        String expected = Introspector.decapitalize(simple);
        // 将开头的缩写转为小写，保留后续单词的大小写
        if (expected.equals(simple)) {
            int end = 1;
            while (end < simple.length() && Character.isUpperCase(simple.charAt(end))
                    && (end + 1 == simple.length() || Character.isUpperCase(simple.charAt(end + 1)))) {
                end++;
            }
            expected = simple.substring(0, end).toLowerCase(java.util.Locale.ROOT) + simple.substring(end);
        }
        boolean alias = aliases.stream().anyMatch(entry -> {
            String[] parts = entry.split("=", 2);
            return source.isType(written, parts[0]) && name.equals(parts[1]);
        });
        if (!name.equals(expected) && !alias
                && !(allowPrefixes && name.matches("[a-z][a-zA-Z0-9]*" + java.util.regex.Pattern.quote(simple)))) {
            log(identifier, "variable.type.name", expected);
        }
    }

    /**
     * 识别声明从外层作用域引用的泛型参数
     *
     * @param declaration 待检查的变量声明
     * @param name        需要解析的类型名
     * @return 匹配外层泛型参数时为 true
     */
    private static boolean isTypeParameter(DetailAST declaration, String name) {
        for (DetailAST owner = declaration.getParent(); owner != null; owner = owner.getParent()) {
            DetailAST parameters = owner.findFirstToken(TokenTypes.TYPE_PARAMETERS);
            if (parameters != null && JavaSource.descendants(parameters, TokenTypes.TYPE_PARAMETER).stream()
                    .anyMatch(parameter -> JavaSource.declarationName(parameter).getText().equals(name))) {
                return true;
            }
        }
        return false;
    }
}
