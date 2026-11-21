package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;

import java.util.List;

/**
 * 识别源码中影响接口文档和序列化可见性的注解
 *
 * @author lonelight
 */
final class ApiAnnotations {
    static final String OAS = "io.swagger.v3.oas.annotations.";
    static final String SCHEMA = OAS + "media.Schema";
    static final String PARAMETER = OAS + "Parameter";
    static final String JSON = "com.fasterxml.jackson.annotation.";
    static final String HIDDEN_ATTRIBUTE = "hidden";

    /**
     * 限制工具类实例化
     */
    private ApiAnnotations() {
    }

    /**
     * 判断接口方法或其所属类型是否被隐藏
     *
     * @param methodDetailAST 已识别的 REST 方法声明
     * @param javaSource      声明所在文件的名称索引
     * @return 显式隐藏时为 true
     */
    static boolean isHiddenEndpoint(DetailAST methodDetailAST, JavaSource javaSource) {
        return hidden(methodDetailAST, javaSource) || hidden(JavaSource.enclosingType(methodDetailAST), javaSource)
                || flag(annotation(methodDetailAST, javaSource, OAS + "Operation"), HIDDEN_ATTRIBUTE);
    }

    /**
     * 判断接口参数是否被显式隐藏
     *
     * @param parameterDetailAST 接口参数声明
     * @param javaSource         声明所在文件的名称索引
     * @return 未显式隐藏时为 true
     */
    static boolean isVisibleParameter(DetailAST parameterDetailAST, JavaSource javaSource) {
        return !hidden(parameterDetailAST, javaSource)
                && !flag(annotation(parameterDetailAST, javaSource, PARAMETER), HIDDEN_ATTRIBUTE);
    }

    /**
     * 识别显式从接口文档或序列化中隐藏的声明
     *
     * @param detailAST  待检查的声明节点
     * @param javaSource 声明所在文件的名称索引
     * @return 存在有效隐藏或忽略注解时为 true
     */
    static boolean hidden(DetailAST detailAST, JavaSource javaSource) {
        var ignore = annotation(detailAST, javaSource, JSON + "JsonIgnore");
        var typeIgnore = annotation(detailAST, javaSource, JSON + "JsonIgnoreType");
        return javaSource.hasKnownAnnotation(detailAST, OAS + "Hidden")
                || flag(annotation(detailAST, javaSource, SCHEMA), HIDDEN_ATTRIBUTE)
                || ignored(ignore) || ignored(typeIgnore);
    }

    /**
     * 判断忽略注解是否生效
     *
     * @param annotation 待检查的注解，可为空
     * @return 注解存在且未显式关闭时为 true
     */
    private static boolean ignored(DetailAST annotation) {
        return annotation != null && (JavaSource.attribute(annotation, "value") == null || flag(annotation, "value"));
    }

    /**
     * 检查注解属性是否显式设为 true
     *
     * @param annotation 待检查的注解，可为空
     * @param key        布尔属性名
     * @return 属性值为 true 字面量时为 true
     */
    static boolean flag(DetailAST annotation, String key) {
        DetailAST value = annotation == null ? null : JavaSource.attribute(annotation, key);
        var expression = value == null ? null : JavaSource.firstOperand(value);
        return expression != null && expression.getType() == TokenTypes.LITERAL_TRUE;
    }

    /**
     * 取得声明上首个匹配类型的注解
     *
     * @param node   待检查的声明节点
     * @param source 声明所在文件的名称索引
     * @param type   注解类型的全限定名
     * @return 首个匹配注解，缺失时为 null
     */
    static DetailAST annotation(DetailAST node, JavaSource source, String type) {
        return annotations(node, source, type).stream().findFirst().orElse(null);
    }

    /**
     * 提取声明上匹配指定类型的注解
     *
     * @param node   待检查的声明节点
     * @param source 声明所在文件的名称索引
     * @param type   注解类型的全限定名
     * @return 匹配的注解
     */
    static List<DetailAST> annotations(DetailAST node, JavaSource source, String type) {
        return JavaSource.annotations(node).stream().filter(nodeAnnotation ->
                source.isKnownAnnotationType(nodeAnnotation, type)).toList();
    }
}
