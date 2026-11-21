package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 提示在接口入口启用可识别的请求模型校验
 *
 * @author lonelight
 */
public final class RequestValidationCheck extends RuleCheck {
    private static final Set<String> OTHER_BINDINGS = Set.of("PathVariable", "RequestParam", "RequestHeader",
            "CookieValue", "MatrixVariable");
    private static final Set<String> CONSTRAINTS = Set.of("AssertFalse", "AssertTrue", "DecimalMax", "DecimalMin",
            "Digits", "Email", "Future", "FutureOrPresent", "Max", "Min", "Negative", "NegativeOrZero", "NotBlank",
            "NotEmpty", "NotNull", "Null", "Past", "PastOrPresent", "Pattern", "Positive", "PositiveOrZero", "Size");
    /**
     * 当前文件的语法树根节点
     */
    private DetailAST rootDetailAST;
    /**
     * 当前文件的名称索引
     */
    private JavaSource javaSource;
    /**
     * 已解析的源码模型
     */
    private ApiSourceModels apiSourceModels;

    /**
     * 判断成员是否为 JavaBean 访问器
     *
     * @param memberDetailAST 待检查的成员节点
     * @return 为无参的非静态 getter 时为 true
     */
    private static boolean isGetter(DetailAST memberDetailAST) {
        var parameters = memberDetailAST.findFirstToken(TokenTypes.PARAMETERS);
        return memberDetailAST.getType() == TokenTypes.METHOD_DEF && !JavaSource.isExplicitlyStatic(memberDetailAST)
                && parameters != null && !parameters.hasChildren()
                && JavaSource.declarationName(memberDetailAST).getText().matches("(?:get|is)[A-Z].*");
    }

    /**
     * 检查声明是否启用了级联校验
     *
     * @param nodeDetailAST       待检查的声明
     * @param declaringJavaSource 声明所在文件的名称索引
     * @return 标注标准 Valid 时为 true
     */
    private static boolean hasCascade(DetailAST nodeDetailAST, JavaSource declaringJavaSource) {
        return JavaSource.annotations(nodeDetailAST).stream()
                .anyMatch(annotationDetailAST -> isValid(annotationDetailAST, declaringJavaSource));
    }

    /**
     * 匹配标准级联校验注解
     *
     * @param annotationDetailAST 待匹配的注解节点
     * @param declaringJavaSource 名称所在文件的解析信息
     * @return 匹配 Jakarta 或 javax Valid 时为 true
     */
    private static boolean isValid(DetailAST annotationDetailAST, JavaSource declaringJavaSource) {
        return declaringJavaSource.isKnownAnnotationType(annotationDetailAST, "jakarta.validation.Valid")
                || declaringJavaSource.isKnownAnnotationType(annotationDetailAST, "javax.validation.Valid");
    }

    /**
     * 监听候选接口方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_DEF};
    }

    /**
     * 重置当前文件的模型索引
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        this.rootDetailAST = rootDetailAST;
        javaSource = new JavaSource(rootDetailAST);
        apiSourceModels = null;
    }

    /**
     * 检查接口中未标注校验的请求对象
     *
     * @param methodDetailAST 待检查的接口方法
     */
    @Override
    public void visitToken(DetailAST methodDetailAST) {
        if (!FrameworkMethods.isRestEndpoint(javaSource, methodDetailAST)) {
            return;
        }
        for (var parameterDetailAST : JavaSource.children(methodDetailAST.findFirstToken(TokenTypes.PARAMETERS))) {
            if (parameterDetailAST.getType() == TokenTypes.PARAMETER_DEF && isCandidate(parameterDetailAST)) {
                checkParameter(parameterDetailAST);
            }
        }
    }

    /**
     * 排除已启用校验或按标量和框架参数绑定的参数
     *
     * @param parameterDetailAST 接口参数声明
     * @return 可能绑定为请求对象时为 true
     */
    private boolean isCandidate(DetailAST parameterDetailAST) {
        return !FrameworkMethods.isInternalParameter(parameterDetailAST, javaSource)
                && !javaSource.hasKnownAnnotation(parameterDetailAST, "org.springframework.beans.factory.annotation.Value")
                && !javaSource.hasKnownAnnotation(parameterDetailAST, "org.springframework.validation.annotation.Validated")
                && !hasCascade(parameterDetailAST, javaSource)
                && OTHER_BINDINGS.stream().noneMatch(binding ->
                javaSource.hasKnownAnnotation(parameterDetailAST, FrameworkMethods.WEB + binding));
    }

    /**
     * 按参数的直接类型查找请求对象及其约束
     *
     * @param parameterDetailAST 待定位的接口参数声明
     */
    private void checkParameter(DetailAST parameterDetailAST) {
        DetailAST typeDetailAST = parameterDetailAST.findFirstToken(TokenTypes.TYPE);
        String name = JavaSource.typeName(typeDetailAST);
        if (typeDetailAST == null || name.isEmpty() || typeDetailAST.findFirstToken(TokenTypes.ARRAY_DECLARATOR) != null
                || parameterDetailAST.findFirstToken(TokenTypes.ELLIPSIS) != null
                || ApiSourceModels.isTypeParameter(parameterDetailAST, name)) {
            return;
        }
        if (apiSourceModels == null) {
            apiSourceModels = new ApiSourceModels(Path.of(getFilePath()).toAbsolutePath().normalize(), rootDetailAST, javaSource);
        }
        var model = apiSourceModels.find(name, javaSource, parameterDetailAST);
        if (model != null && hasConstraints(model, new HashSet<>())) {
            log(JavaSource.declarationName(parameterDetailAST), "request.validation", model.name());
        }
    }

    /**
     * 查找类型、属性及父类上的约束或级联校验
     *
     * @param model   待检查的模型
     * @param visited 已检查的模型名称
     * @return 找到校验声明时为 true
     */
    private boolean hasConstraints(ApiSourceModels.Model model, Set<String> visited) {
        int kind = model.tree().getType();
        if ((kind != TokenTypes.CLASS_DEF && kind != TokenTypes.RECORD_DEF && kind != TokenTypes.INTERFACE_DEF)
                || !visited.add(model.name())) {
            return false;
        }
        if (hasConstraint(model.tree(), model.source())) {
            return true;
        }
        for (var memberDetailAST : ApiSourceModels.members(model.tree())) {
            if ((JavaSource.isInstanceFieldOrComponent(memberDetailAST) || isGetter(memberDetailAST))
                    && hasConstrainedMember(memberDetailAST, model.source())) {
                return true;
            }
        }
        return hasConstrainedParents(model, TokenTypes.EXTENDS_CLAUSE, visited)
                || hasConstrainedParents(model, TokenTypes.IMPLEMENTS_CLAUSE, visited);
    }

    /**
     * 检查继承或实现的直接父类型
     *
     * @param model   待查找父类型的模型
     * @param clause  父类型子句类别
     * @param visited 已检查的模型名称
     * @return 父类型中存在约束时为 true
     */
    private boolean hasConstrainedParents(ApiSourceModels.Model model, int clause, Set<String> visited) {
        for (var parentDetailAST : JavaSource.children(model.tree().findFirstToken(clause))) {
            if (parentDetailAST.getType() == TokenTypes.IDENT || parentDetailAST.getType() == TokenTypes.DOT) {
                String name = FullIdent.createFullIdent(parentDetailAST).getText();
                var parentModel = apiSourceModels.find(name, model.source(), model.tree());
                if (parentModel != null && hasConstraints(parentModel, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查成员声明和成员类型上的校验注解
     *
     * @param memberDetailAST     字段、record 组件或 getter 声明
     * @param declaringJavaSource 成员所在文件的名称索引
     * @return 存在直接约束或级联标记时为 true
     */
    private boolean hasConstrainedMember(DetailAST memberDetailAST, JavaSource declaringJavaSource) {
        return hasCascade(memberDetailAST, declaringJavaSource) || hasConstraint(memberDetailAST, declaringJavaSource)
                || JavaSource.descendants(memberDetailAST.findFirstToken(TokenTypes.TYPE), TokenTypes.ANNOTATION).stream()
                .anyMatch(annotationDetailAST -> isConstraint(annotationDetailAST, memberDetailAST, declaringJavaSource)
                        || isValid(annotationDetailAST, declaringJavaSource));
    }

    /**
     * 判断声明是否标注了校验约束
     *
     * @param declarationDetailAST 待检查的声明
     * @param declaringJavaSource  声明所在文件的名称索引
     * @return 至少存在一个可识别约束时为 true
     */
    private boolean hasConstraint(DetailAST declarationDetailAST, JavaSource declaringJavaSource) {
        return JavaSource.annotations(declarationDetailAST).stream()
                .anyMatch(annotationDetailAST -> isConstraint(annotationDetailAST, declarationDetailAST, declaringJavaSource));
    }

    /**
     * 识别标准约束和带 Constraint 元注解的本地自定义约束
     *
     * @param annotationDetailAST 待检查的注解节点
     * @param contextDetailAST    注解所在声明
     * @param declaringJavaSource 声明所在文件的名称索引
     * @return 可确认是 Bean Validation 约束时为 true
     */
    private boolean isConstraint(DetailAST annotationDetailAST, DetailAST contextDetailAST, JavaSource declaringJavaSource) {
        String name = JavaSource.annotationName(annotationDetailAST);
        String simple = JavaSource.simpleName(name);
        if (CONSTRAINTS.contains(simple)
                && (declaringJavaSource.isKnownAnnotationType(annotationDetailAST, "jakarta.validation.constraints." + simple)
                || declaringJavaSource.isKnownAnnotationType(annotationDetailAST, "javax.validation.constraints." + simple))) {
            return true;
        }
        var model = apiSourceModels.find(name, declaringJavaSource, contextDetailAST);
        return model != null && model.tree().getType() == TokenTypes.ANNOTATION_DEF
                && (model.source().hasKnownAnnotation(model.tree(), "jakarta.validation.Constraint")
                || model.source().hasKnownAnnotation(model.tree(), "javax.validation.Constraint"));
    }
}
