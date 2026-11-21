package lonelight.checks.naming;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 按配置检查类型名后缀
 *
 * @author lonelight
 */
public final class TypeSuffixCheck extends RuleCheck {
    private static final Pattern SUFFIX = Pattern.compile("[A-Za-z0-9]+");

    /**
     * 全限定类型名模式与允许的后缀
     */
    private final Map<Pattern, String[]> mappings = new LinkedHashMap<>();

    /**
     * 当前文件的名称索引
     */
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
            String suffixList = parts.length == 2 ? parts[1] : "";
            String[] suffixes = suffixList.split("\\|", -1);
            if (parts[0].isBlank() || Arrays.stream(suffixes).anyMatch(suffix -> !SUFFIX.matcher(suffix).matches())) {
                throw new IllegalArgumentException("mappings entries must use regex=Suffix|OtherSuffix");
            }
            mappings.put(Pattern.compile(parts[0]), suffixes);
        }
    }

    /**
     * 监听支持后缀检查的类型声明
     *
     * @return 类、接口、record 和枚举的节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.RECORD_DEF, TokenTypes.ENUM_DEF};
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
        if (mappings.isEmpty()) {
            return;
        }
        String declaredName = source.declaredTypeName(node);
        if (declaredName.isEmpty()) {
            return;
        }
        DetailAST name = JavaSource.declarationName(node);
        for (Map.Entry<Pattern, String[]> mapping : mappings.entrySet()) {
            if (mapping.getKey().matcher(declaredName).matches()
                    && Arrays.stream(mapping.getValue()).noneMatch(name.getText()::endsWith)) {
                log(name, "type.suffix", String.join(" / ", mapping.getValue()));
            }
        }
    }
}
