package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 检查指定注解属性是否缺失或为空白
 *
 * @author lonelight
 */
public final class AnnotationAttributeCheck extends RuleCheck {
    /**
     * 各注解必须填写的属性名
     */
    private final Map<String, Set<String>> attributes = new LinkedHashMap<>();

    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 当前文件的语法树根节点
     */
    private DetailAST rootDetailAST;

    /**
     * 用于区分同包声明和通配导入的模型索引
     */
    private ApiSourceModels apiSourceModels;

    /**
     * 设置必须填写的注解属性
     *
     * @param values 采用「注解全限定名=属性名」格式的配置项
     * @throws IllegalArgumentException 配置项的格式不正确时
     */
    public void setAttributes(String... values) {
        attributes.clear();
        for (String value : values) {
            String[] pair = value.split("=", 2);
            if (pair.length != 2 || !pair[0].contains(".") || pair[1].isBlank()) {
                throw new IllegalArgumentException("attributes entries must use canonical.Annotation=attribute");
            }
            attributes.computeIfAbsent(pair[0], ignored -> new LinkedHashSet<>()).add(pair[1]);
        }
    }

    /**
     * 监听源码中的注解
     *
     * @return 注解节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.ANNOTATION};
    }

    /**
     * 为当前文件准备注解名称解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        rootDetailAST = root;
        source = new JavaSource(root);
        apiSourceModels = null;
    }

    /**
     * 报告配置属性缺失或字面值为空白的注解
     *
     * @param annotation 待检查的注解节点
     */
    @Override
    public void visitToken(DetailAST annotation) {
        for (Map.Entry<String, Set<String>> entry : attributes.entrySet()) {
            if (source.isKnownAnnotationType(annotation, entry.getKey(), this::hasPackageType)) {
                for (String attribute : entry.getValue()) {
                    DetailAST value = JavaSource.attribute(annotation, attribute);
                    String literal = JavaSource.literalText(value);
                    if (value == null || literal != null && literal.isBlank()) {
                        log(annotation, "annotation.attribute", entry.getKey(), attribute);
                    }
                }
            }
        }
    }

    /**
     * 查找可能遮蔽通配导入的同包类型
     *
     * @param name 同包类型的短名
     * @return 找到同包类型时为 true
     */
    private boolean hasPackageType(String name) {
        if (apiSourceModels == null) {
            Path filePath = Path.of(getFilePath()).toAbsolutePath().normalize();
            apiSourceModels = new ApiSourceModels(filePath, rootDetailAST, source);
        }
        return apiSourceModels.findCanonical(source.resolve(name)) != null;
    }
}
