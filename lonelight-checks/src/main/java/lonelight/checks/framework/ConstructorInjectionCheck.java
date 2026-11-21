package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 检查 Spring 组件通过构造器注入依赖并保存到私有 final 字段
 *
 * @author lonelight
 */
public final class ConstructorInjectionCheck extends RuleCheck {
    private static final Set<String> INJECT = Set.of("org.springframework.beans.factory.annotation.Autowired",
            "jakarta.inject.Inject", "javax.inject.Inject");
    private static final Set<String> COMPONENTS = Set.of("org.springframework.stereotype.Component",
            "org.springframework.stereotype.Controller", "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository", "org.springframework.web.bind.annotation.RestController",
            "org.springframework.context.annotation.Configuration", "org.springframework.boot.autoconfigure.SpringBootApplication");
    private static final String REQUIRED_ARGS = "lombok.RequiredArgsConstructor";
    private static final String CONFIG_VALUE = "org.springframework.beans.factory.annotation.Value";
    /**
     * 当前文件的名称索引
     */
    private JavaSource javaSource;

    /**
     * 识别对当前对象字段的直接赋值目标
     *
     * @param nodeDetailAST  赋值左侧节点
     * @param shadowingNames 当前作用域中的参数和局部变量
     * @return 可确认的字段名，无法确认时为空字符串
     */
    private static String fieldName(DetailAST nodeDetailAST, Set<String> shadowingNames) {
        if (nodeDetailAST.getType() == TokenTypes.DOT && nodeDetailAST.getFirstChild().getType() == TokenTypes.LITERAL_THIS
                && nodeDetailAST.getLastChild().getType() == TokenTypes.IDENT) {
            return nodeDetailAST.getLastChild().getText();
        }
        return nodeDetailAST.getType() == TokenTypes.IDENT && !shadowingNames.contains(nodeDetailAST.getText()) ? nodeDetailAST.getText() : "";
    }

    /**
     * 判断是否声明指定字段修饰符
     *
     * @param fieldDetailAST 待检查的字段
     * @param token          目标修饰符节点类别
     * @return 修饰符直接存在时为 true
     */
    private static boolean hasModifier(DetailAST fieldDetailAST, int token) {
        var modifiers = fieldDetailAST.findFirstToken(TokenTypes.MODIFIERS);
        return modifiers != null && modifiers.findFirstToken(token) != null;
    }

    /**
     * 监听可能包含注入成员的类
     *
     * @return 类声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.CLASS_DEF};
    }

    /**
     * 索引当前文件的注解名称
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        javaSource = indexSource(rootDetailAST);
    }

    /**
     * 检查字段注入和可识别的显式或 Lombok 构造器注入
     *
     * @param typeDetailAST 待检查的类声明
     */
    @Override
    public void visitToken(DetailAST typeDetailAST) {
        var members = ApiSourceModels.members(typeDetailAST);
        Map<String, DetailAST> fields = members.stream().filter(JavaSource::isInstanceFieldOrComponent)
                .collect(Collectors.toMap(fieldDetailAST -> JavaSource.declarationName(fieldDetailAST).getText(), Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
        var constructors = members.stream().filter(member -> member.getType() == TokenTypes.CTOR_DEF).toList();
        boolean component = COMPONENTS.stream().anyMatch(annotation -> javaSource.hasKnownAnnotation(typeDetailAST, annotation));
        boolean lombok = javaSource.hasKnownAnnotation(typeDetailAST, REQUIRED_ARGS);
        Set<DetailAST> injected = new LinkedHashSet<>();
        if (component && lombok) {
            fields.values().stream().filter(fieldDetailAST -> fieldDetailAST.findFirstToken(TokenTypes.ASSIGN) == null
                            && (hasModifier(fieldDetailAST, TokenTypes.FINAL) || javaSource.hasKnownAnnotation(fieldDetailAST, "lombok.NonNull")))
                    .forEach(injected::add);
        }
        boolean soleConstructor = component && constructors.size() == 1 && !lombok
                && !javaSource.hasKnownAnnotation(typeDetailAST, "lombok.AllArgsConstructor")
                && !javaSource.hasKnownAnnotation(typeDetailAST, "lombok.NoArgsConstructor");
        for (var constructorDetailAST : constructors) {
            if (soleConstructor || usesInject(constructorDetailAST)) {
                collectAssignedFields(constructorDetailAST, fields, injected);
            }
        }
        for (var fieldDetailAST : fields.values()) {
            checkInjectedField(fieldDetailAST, injected.contains(fieldDetailAST));
        }
    }

    /**
     * 检查注入写法和依赖字段的修饰符
     *
     * @param fieldDetailAST         待检查的实例字段
     * @param injectedViaConstructor 字段是否由注入构造器赋值
     */
    private void checkInjectedField(DetailAST fieldDetailAST, boolean injectedViaConstructor) {
        if (usesInject(fieldDetailAST) || javaSource.hasKnownAnnotation(fieldDetailAST, "jakarta.annotation.Resource")
                || javaSource.hasKnownAnnotation(fieldDetailAST, "javax.annotation.Resource")) {
            log(JavaSource.declarationName(fieldDetailAST), "injection.field");
        } else if (injectedViaConstructor && !javaSource.hasKnownAnnotation(fieldDetailAST, CONFIG_VALUE)
                && (!hasModifier(fieldDetailAST, TokenTypes.LITERAL_PRIVATE)
                || !hasModifier(fieldDetailAST, TokenTypes.FINAL))) {
            log(JavaSource.declarationName(fieldDetailAST), "injection.final");
        }
    }

    /**
     * 收集由构造器参数直接赋值的当前类字段
     *
     * @param constructorDetailAST 已识别的注入构造器
     * @param fields               按名称索引的实例字段
     * @param injected             待更新的注入字段集合
     */
    private void collectAssignedFields(DetailAST constructorDetailAST, Map<String, DetailAST> fields, Set<DetailAST> injected) {
        var parameters = JavaSource.children(constructorDetailAST.findFirstToken(TokenTypes.PARAMETERS)).stream()
                .filter(FrameworkMethods::isValueParameter).toList();
        Set<String> names = parameters.stream().map(JavaSource::declarationName).map(DetailAST::getText)
                .collect(Collectors.toSet());
        Set<String> beanParameters = parameters.stream().filter(parameter -> !javaSource.hasKnownAnnotation(parameter, CONFIG_VALUE))
                .map(JavaSource::declarationName).map(DetailAST::getText).collect(Collectors.toSet());
        Set<String> localNames = new HashSet<>(names);
        for (var statement : JavaSource.children(constructorDetailAST.findFirstToken(TokenTypes.SLIST))) {
            if (statement.getType() == TokenTypes.VARIABLE_DEF) {
                localNames.add(JavaSource.declarationName(statement).getText());
            } else if (statement.getType() == TokenTypes.EXPR && statement.getFirstChild().getType() == TokenTypes.ASSIGN) {
                var assignment = statement.getFirstChild();
                var value = assignment.getLastChild();
                if (value.getType() == TokenTypes.IDENT && beanParameters.contains(value.getText())) {
                    var fieldDetailAST = fields.get(fieldName(assignment.getFirstChild(), localNames));
                    if (fieldDetailAST != null) {
                        injected.add(fieldDetailAST);
                    }
                }
            }
        }
    }

    /**
     * 判断是否标有 Spring 或标准依赖注入注解
     *
     * @param nodeDetailAST 待检查的声明
     * @return 匹配注解时为 true
     */
    private boolean usesInject(DetailAST nodeDetailAST) {
        return INJECT.stream().anyMatch(annotation -> javaSource.hasKnownAnnotation(nodeDetailAST, annotation));
    }
}
