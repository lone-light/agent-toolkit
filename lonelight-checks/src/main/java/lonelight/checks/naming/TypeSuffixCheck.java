package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 按配置检查类型名后缀
 *
 * @author lonelight
 */
public final class TypeSuffixCheck extends RuleCheck {
    private final Map<Pattern, String[]> mappings = new LinkedHashMap<>();
    private JavaSource source;

    /**
     * 设置各类名称模式允许使用的后缀
     *
     * @param values 采用 regex=Suffix 格式的配置项，多个后缀用竖线分隔
     * @throws IllegalArgumentException 后缀映射格式或正则不正确时
     */
    public void setMappings(String... values) {
        mappings.clear();
        for (String value : values) {
            String[] parts = value.split("=", 2);
            if (parts.length != 2 || parts[0].isBlank() || !parts[1].matches("[A-Za-z0-9]+(\\|[A-Za-z0-9]+)*")) {
                throw new IllegalArgumentException("mappings entries must use regex=Suffix|OtherSuffix");
            }
            mappings.put(Pattern.compile(parts[0]), parts[1].split("\\|"));
        }
    }

    /**
     * 监听支持后缀检查的类型声明
     *
     * @return 类、接口、record 和枚举的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.RECORD_DEF, TokenTypes.ENUM_DEF};
    }

    /**
     * 为当前文件准备类型名称解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 检查命中配置的类型名是否使用允许的后缀
     *
     * @param node 待检查的类型声明
     */
    @Override
    public void visitToken(DetailAST node) {
        DetailAST name = JavaSource.declarationName(node);
        for (Map.Entry<Pattern, String[]> mapping : mappings.entrySet()) {
            if (mapping.getKey().matcher(source.resolve(name.getText())).matches()
                    && java.util.Arrays.stream(mapping.getValue()).noneMatch(name.getText()::endsWith)) {
                log(name, "type.suffix", String.join(" / ", mapping.getValue()));
            }
        }
    }
}
