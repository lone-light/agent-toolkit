package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 从 REST 接口签名检查接口及请求响应模型的说明注解
 *
 * @author lonelight
 */
public final class ControllerApiDocumentationCheck extends RuleCheck {
    private static final String OAS = "io.swagger.v3.oas.annotations.";
    private static final String WEB = FrameworkMethods.WEB;
    private static final String SCHEMA = OAS + "media.Schema";
    private static final String JSON = "com.fasterxml.jackson.annotation.";
    private static final Set<String> PARAMETER_BINDINGS = Set.of(
            "PathVariable", "RequestParam", "RequestHeader", "CookieValue", "MatrixVariable");
    private static final Set<String> INTERNAL_ANNOTATIONS = Set.of(WEB + "RequestAttribute", WEB + "SessionAttribute",
            "org.springframework.security.core.annotation.AuthenticationPrincipal",
            "org.springframework.security.core.annotation.CurrentSecurityContext");
    private static final Set<String> INTERNAL_TYPES = Set.of(
            "jakarta.servlet.ServletRequest", "jakarta.servlet.ServletResponse",
            "jakarta.servlet.http.HttpServletRequest", "jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpSession", "javax.servlet.ServletRequest", "javax.servlet.ServletResponse",
            "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpSession", "java.security.Principal",
            "org.springframework.security.core.Authentication", "org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.web.server.ServerWebExchange", "org.springframework.validation.BindingResult",
            "org.springframework.validation.Errors", "org.springframework.ui.Model", "org.springframework.ui.ModelMap");
    private static final Set<String> SCALAR_TYPES = Set.of(
            "java.lang.String", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Character", "java.util.UUID",
            "java.math.BigInteger", "java.math.BigDecimal", "java.time.LocalDate", "java.time.LocalDateTime",
            "java.time.Instant", "java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.util.Date");
    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char");
    private static final Set<String> SCALAR_CONTAINERS = Set.of("java.util.Optional", "java.util.List",
            "java.util.Set", "java.util.Collection");

    private final Set<FieldLocation> reportedFields = new HashSet<>();
    private JavaSource source;
    private ApiSourceModels models;
    private DetailAST root;

    /**
     * 监听可能声明 REST 接口的方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.METHOD_DEF};
    }

    /**
     * 重置本文件的接口文档检查状态
     *
     * @param tree 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST tree) {
        root = tree;
        source = null;
        models = null;
        reportedFields.clear();
    }

    /**
     * 释放本文件的语法树及模型索引
     *
     * @param tree 当前文件的语法树根节点
     */
    @Override
    public void finishTree(DetailAST tree) {
        root = null;
        source = null;
        models = null;
    }

    /**
     * 检查 REST 方法及其请求响应的文档注解
     *
     * @param method 待检查的方法声明
     */
    @Override
    public void visitToken(DetailAST method) {
        if (JavaSource.annotations(method).isEmpty()) {
            return;
        }
        if (source == null) {
            source = new JavaSource(root);
        }
        if (!FrameworkMethods.isRestEndpoint(source, method)) {
            return;
        }
        DetailAST operation = annotation(method, source, OAS + "Operation");
        if (hidden(method, source) || hidden(JavaSource.enclosingType(method), source) || flag(operation, "hidden")) {
            return;
        }
        DetailAST methodName = JavaSource.declarationName(method);
        if (!text(operation, "summary")) {
            log(methodName, "api.source.operation");
        }
        if (models == null) {
            models = new ApiSourceModels(Path.of(getFilePath()).toAbsolutePath().normalize(), root, source);
        }

        Set<String> visited = new HashSet<>();
        List<DetailAST> parameterDocs = new ArrayList<>(annotations(method, source, OAS + "Parameter"));
        parameterDocs.addAll(nested(annotation(method, source, OAS + "Parameters"), source, "value", OAS + "Parameter"));
        parameterDocs.addAll(nested(operation, source, "parameters", OAS + "Parameter"));
        DetailAST parameters = method.findFirstToken(TokenTypes.PARAMETERS);
        for (DetailAST parameter = parameters == null ? null : parameters.getFirstChild();
                parameter != null; parameter = parameter.getNextSibling()) {
            if (parameter.getType() == TokenTypes.PARAMETER_DEF) {
                checkParameter(method, operation, parameter, parameterDocs, visited);
            }
        }
        checkModels(method, methodName, source, new HashSet<>(), Direction.RESPONSE);
    }

    /**
     * 按参数绑定方式检查参数说明或模型字段说明
     *
     * @param method 接口方法声明
     * @param operation 接口的 Operation 注解，可为空
     * @param parameter 待检查的参数声明
     * @param parameterDocs 方法级参数说明
     * @param visited 本次请求已遍历的模型名称
     */
    private void checkParameter(DetailAST method, DetailAST operation, DetailAST parameter,
                                List<DetailAST> parameterDocs, Set<String> visited) {
        DetailAST name = JavaSource.declarationName(parameter);
        DetailAST directDoc = annotation(parameter, source, OAS + "Parameter");
        if (hidden(parameter, source) || flag(directDoc, "hidden") || isInternal(parameter)) {
            return;
        }

        DetailAST binding = PARAMETER_BINDINGS.stream().map(key -> annotation(parameter, source, WEB + key))
                .filter(Objects::nonNull).findFirst().orElse(null);
        DetailAST part = annotation(parameter, source, WEB + "RequestPart");
        boolean body = source.hasKnownAnnotation(parameter, WEB + "RequestBody");
        boolean form = source.hasKnownAnnotation(parameter, WEB + "ModelAttribute");
        if (binding != null || !body && !form && part == null && isImplicitScalar(parameter)) {
            requireParameterDescription(name, directDoc, binding, parameterDocs);
            return;
        }

        if (!checkModels(parameter, name, source, visited, Direction.REQUEST)) {
            if (body && !description(annotation(parameter, source, OAS + "parameters.RequestBody"))
                    && !description(annotation(method, source, OAS + "parameters.RequestBody"))
                    && nested(operation, source, "requestBody", OAS + "parameters.RequestBody").stream()
                        .noneMatch(ControllerApiDocumentationCheck::description)) {
                log(name, "api.source.body");
            } else if (part != null || !body && !form && directDoc != null) {
                requireParameterDescription(name, directDoc, part, parameterDocs);
            }
        }
    }

    /**
     * 报告缺少直接或方法级说明的参数
     *
     * @param name 参数名节点
     * @param documentation 直接标注的 Parameter 注解，可为空
     * @param binding 参数绑定注解，可为空
     * @param parameterDocs 方法级参数说明
     */
    private void requireParameterDescription(DetailAST name, DetailAST documentation, DetailAST binding,
                                             List<DetailAST> parameterDocs) {
        if (!description(documentation) && !matchingParameterDoc(parameterDocs, binding, name.getText())) {
            log(name, "api.source.parameter");
        }
    }

    /**
     * 按绑定名称和位置匹配方法级参数说明
     *
     * @param docs 候选 Parameter 注解
     * @param binding 参数绑定注解，可为空
     * @param javaName Java 参数名
     * @return 存在有效说明或隐藏标记时为 true
     */
    private boolean matchingParameterDoc(List<DetailAST> docs, DetailAST binding, String javaName) {
        String name = boundName(binding, javaName);
        if (name == null) {
            return false;
        }
        String location = binding == null ? "QUERY" : switch (JavaSource.simpleName(JavaSource.annotationName(binding))) {
            case "PathVariable", "MatrixVariable" -> "PATH";
            case "RequestHeader" -> "HEADER";
            case "CookieValue" -> "COOKIE";
            default -> "QUERY";
        };
        return docs.stream().anyMatch(doc -> {
            String candidate = JavaSource.literalText(JavaSource.attribute(doc, "name"));
            String documentedLocation = enumConstant(doc, "in");
            boolean sameName = location.equals("HEADER") ? name.equalsIgnoreCase(candidate) : name.equals(candidate);
            return sameName && (documentedLocation == null || documentedLocation.equals("DEFAULT")
                    || documentedLocation.equals(location))
                    && (flag(doc, "hidden") || description(doc));
        });
    }

    /**
     * 读取绑定名称并在省略时采用参数名
     *
     * @param binding 参数绑定注解，可为空
     * @param fallback 默认参数名
     * @return 绑定名称，无法静态解析时为 null
     */
    private static String boundName(DetailAST binding, String fallback) {
        if (binding != null) {
            for (String key : List.of("name", "value")) {
                DetailAST value = JavaSource.attribute(binding, key);
                if (value != null) {
                    String literal = JavaSource.literalText(value);
                    if (literal == null || !literal.isEmpty()) {
                        return literal;
                    }
                }
            }
        }
        return fallback;
    }

    /**
     * 识别不需要对外说明的框架参数
     *
     * @param parameter 待检查的参数声明
     * @return 参数由框架提供时为 true
     */
    private boolean isInternal(DetailAST parameter) {
        String name = JavaSource.typeName(parameter.findFirstToken(TokenTypes.TYPE));
        return INTERNAL_ANNOTATIONS.stream().anyMatch(key -> source.hasKnownAnnotation(parameter, key))
                || INTERNAL_TYPES.stream().anyMatch(key -> source.isKnownType(name, key));
    }

    /**
     * 识别未显式绑定的简单请求参数
     *
     * @param parameter 待检查的参数声明
     * @return 参数类型可按标量处理时为 true
     */
    private boolean isImplicitScalar(DetailAST parameter) {
        var type = parameter.findFirstToken(TokenTypes.TYPE);
        if (type == null) {
            return false;
        }
        DetailAST base = type;
        for (var array = type.findFirstToken(TokenTypes.ARRAY_DECLARATOR); array != null;
                array = array.findFirstToken(TokenTypes.ARRAY_DECLARATOR)) {
            base = array;
        }
        String name = JavaSource.typeName(base);
        if (isScalar(name, parameter) || PRIMITIVES.contains(base.getFirstChild().getText())) {
            return true;
        }
        if (SCALAR_CONTAINERS.stream().noneMatch(key -> source.isKnownType(name, key))) {
            return false;
        }
        List<String> types = new ArrayList<>();
        ApiSourceModels.typeNames(type, types::add);
        return types.size() == 2 && isScalar(types.getLast(), parameter);
    }

    /**
     * 识别内置标量或源码中的枚举
     *
     * @param name 参数的类型名
     * @param declaration 参数声明节点
     * @return 类型可按标量处理时为 true
     */
    private boolean isScalar(String name, DetailAST declaration) {
        if (SCALAR_TYPES.stream().anyMatch(key -> source.isKnownType(name, key))) {
            return true;
        }
        if (name.isEmpty() || typeParameter(declaration, name)) {
            return false;
        }
        ApiSourceModels.Model model = models.find(name, source, declaration);
        return model != null && model.tree().getType() == TokenTypes.ENUM_DEF;
    }

    /**
     * 查找并检查声明类型所引用的源码模型
     *
     * @param declaration 带有类型的声明节点
     * @param target 报告诊断的位置
     * @param declaringSource 声明所在文件的名称索引
     * @param visited 当前方向已遍历的模型名称
     * @param direction 请求或响应方向
     * @return 找到至少一个源码模型时为 true
     */
    private boolean checkModels(DetailAST declaration, DetailAST target, JavaSource declaringSource, Set<String> visited,
                                Direction direction) {
        Set<ApiSourceModels.Model> referenced = new LinkedHashSet<>();
        ApiSourceModels.typeNames(declaration.findFirstToken(TokenTypes.TYPE), name -> {
            if (!typeParameter(declaration, name)) {
                ApiSourceModels.Model model = models.find(name, declaringSource, declaration);
                if (model != null) {
                    referenced.add(model);
                }
            }
        });
        for (ApiSourceModels.Model model : referenced) {
            checkModel(model, target, visited, direction);
        }
        return !referenced.isEmpty();
    }

    /**
     * 检查模型及其父类的对外字段说明
     *
     * @param model 待检查的模型
     * @param target 报告诊断的位置
     * @param visited 当前方向已遍历的模型名称
     * @param direction 请求或响应方向
     */
    private void checkModel(ApiSourceModels.Model model, DetailAST target, Set<String> visited, Direction direction) {
        if (!visited.add(model.name()) || hidden(model.tree(), model.source())) {
            return;
        }
        DetailAST fields = model.tree().findFirstToken(model.tree().getType() == TokenTypes.RECORD_DEF
                ? TokenTypes.RECORD_COMPONENTS : TokenTypes.OBJBLOCK);
        for (DetailAST field = fields == null ? null : fields.getFirstChild();
                field != null; field = field.getNextSibling()) {
            if (JavaSource.isInstanceFieldOrComponent(field)) {
                checkField(model, field, target, visited, direction);
            }
        }
        DetailAST parent = model.tree().findFirstToken(TokenTypes.EXTENDS_CLAUSE);
        ApiSourceModels.typeNames(parent, name -> {
            if (!typeParameter(model.tree(), name)) {
                ApiSourceModels.Model base = models.find(name, model.source(), model.tree());
                if (base != null) {
                    checkModel(base, target, visited, direction);
                }
            }
        });
    }

    /**
     * 检查可见字段的说明及其引用模型
     *
     * @param model 字段所属的模型
     * @param field 字段或 record 组件
     * @param target 报告诊断的位置
     * @param visited 当前方向已遍历的模型名称
     * @param direction 请求或响应方向
     */
    private void checkField(ApiSourceModels.Model model, DetailAST field, DetailAST target, Set<String> visited,
                            Direction direction) {
        JavaSource declaringSource = model.source();
        if (hidden(field, declaringSource)) {
            return;
        }
        DetailAST schema = annotation(field, declaringSource, SCHEMA);
        String schemaAccess = enumConstant(schema, "accessMode");
        String jsonAccess = enumConstant(annotation(field, declaringSource, JSON + "JsonProperty"), "access");
        if (direction.excludedAccess.equals(schemaAccess) || direction.excludedAccess.equals(jsonAccess)) {
            return;
        }
        var arraySchema = annotation(field, declaringSource, OAS + "media.ArraySchema");
        boolean described = text(schema, "description")
                || nested(arraySchema, declaringSource, "arraySchema", SCHEMA).stream()
                    .anyMatch(value -> text(value, "description"));
        if (!described) {
            DetailAST name = JavaSource.declarationName(field);
            if (reportedFields.add(new FieldLocation(model.name(), name.getText()))) {
                log(target, "api.source.field", model.name(), name.getText(), name.getLineNo());
            }
        }
        checkModels(field, target, declaringSource, visited, direction);
    }

    /**
     * 判断名称是否为当前作用域中的泛型参数
     *
     * @param context 类型引用所在节点
     * @param name 待匹配的泛型参数名
     * @return 由当前或外层声明提供时为 true
     */
    private static boolean typeParameter(DetailAST context, String name) {
        for (DetailAST owner = context; owner != null; owner = owner.getParent()) {
            DetailAST parameters = owner.findFirstToken(TokenTypes.TYPE_PARAMETERS);
            if (parameters != null && JavaSource.descendants(parameters, TokenTypes.TYPE_PARAMETER).stream()
                    .anyMatch(parameter -> JavaSource.declarationName(parameter).getText().equals(name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 识别显式从接口文档或序列化中隐藏的声明
     *
     * @param node 待检查的声明节点
     * @param source 声明所在文件的名称索引
     * @return 存在有效隐藏或忽略注解时为 true
     */
    private static boolean hidden(DetailAST node, JavaSource source) {
        DetailAST ignore = annotation(node, source, JSON + "JsonIgnore");
        DetailAST typeIgnore = annotation(node, source, JSON + "JsonIgnoreType");
        return source.hasKnownAnnotation(node, OAS + "Hidden") || flag(annotation(node, source, SCHEMA), "hidden")
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
     * @param key 布尔属性名
     * @return 属性值为 true 字面量时为 true
     */
    private static boolean flag(DetailAST annotation, String key) {
        DetailAST value = annotation == null ? null : JavaSource.attribute(annotation, key);
        return value != null && value.getFirstChild().getType() == TokenTypes.LITERAL_TRUE;
    }

    /**
     * 读取注解属性中写出的枚举常量名
     *
     * @param annotation 待检查的注解，可为空
     * @param key 属性名
     * @return 常量的短名，属性缺失时为 null
     */
    private static String enumConstant(DetailAST annotation, String key) {
        DetailAST expression = annotation == null ? null : JavaSource.attribute(annotation, key);
        if (expression == null) {
            return null;
        }
        DetailAST value = expression.getFirstChild();
        return value.getType() == TokenTypes.DOT ? value.getLastChild().getText() : value.getText();
    }

    /**
     * 检查注解是否提供说明或引用
     *
     * @param annotation 待检查的注解，可为空
     * @return 说明或引用属性有效时为 true
     */
    private static boolean description(DetailAST annotation) {
        return text(annotation, "description") || text(annotation, "ref");
    }

    /**
     * 检查注解属性是否存在且未明确为空白
     *
     * @param annotation 待检查的注解，可为空
     * @param key 文本属性名
     * @return 属性无法静态判空或包含非空白文本时为 true
     */
    private static boolean text(DetailAST annotation, String key) {
        DetailAST value = annotation == null ? null : JavaSource.attribute(annotation, key);
        String literal = JavaSource.literalText(value);
        return value != null && (literal == null || !literal.isBlank());
    }

    /**
     * 筛选注解属性中指定类型的嵌套注解
     *
     * @param annotation 外层注解，可为空
     * @param source 注解所在文件的名称索引
     * @param attribute 属性名
     * @param type 注解类型的全限定名
     * @return 匹配的嵌套注解
     */
    private static List<DetailAST> nested(DetailAST annotation, JavaSource source, String attribute, String type) {
        return annotation == null ? List.of() : JavaSource.annotationValues(annotation, attribute).stream()
                .filter(node -> source.isKnownType(JavaSource.annotationName(node), type)).toList();
    }

    /**
     * 取得声明上首个匹配类型的注解
     *
     * @param node 待检查的声明节点
     * @param source 声明所在文件的名称索引
     * @param type 注解类型的全限定名
     * @return 首个匹配注解，缺失时为 null
     */
    private static DetailAST annotation(DetailAST node, JavaSource source, String type) {
        return annotations(node, source, type).stream().findFirst().orElse(null);
    }

    /**
     * 提取声明上匹配指定类型的注解
     *
     * @param node 待检查的声明节点
     * @param source 声明所在文件的名称索引
     * @param type 注解类型的全限定名
     * @return 匹配的注解
     */
    private static List<DetailAST> annotations(DetailAST node, JavaSource source, String type) {
        return JavaSource.annotations(node).stream().filter(nodeAnnotation ->
                source.isKnownType(JavaSource.annotationName(nodeAnnotation), type)).toList();
    }

    /**
     * 区分请求和响应方向的字段可见性
     *
     * @author lonelight
     */
    private enum Direction {
        REQUEST("READ_ONLY"),
        RESPONSE("WRITE_ONLY");

        private final String excludedAccess;

        /**
         * 指定当前方向不暴露的字段访问模式
         *
         * @param excludedAccess 需要排除的访问模式
         */
        Direction(String excludedAccess) {
            this.excludedAccess = excludedAccess;
        }
    }

    /**
     * 标识已报告的模型字段
     *
     * @param model 模型的完整类型名称
     * @param name 字段名称
     * @author lonelight
     */
    private record FieldLocation(String model, String name) { }
}
