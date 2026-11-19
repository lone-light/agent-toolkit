package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.nio.file.Path;
import java.util.*;

import static lonelight.checks.framework.ApiAnnotations.*;

/**
 * 从 REST 接口签名检查接口及请求响应模型的说明注解
 *
 * @author lonelight
 */
public final class ControllerApiDocumentationCheck extends RuleCheck {
    private static final String WEB = FrameworkMethods.WEB;
    private static final String REQUEST_BODY_DOC = OAS + "parameters.RequestBody";
    private static final String DESCRIPTION_ATTRIBUTE = "description";
    private static final Set<String> PARAMETER_BINDINGS = Set.of(
            "PathVariable", "RequestParam", "RequestHeader", "CookieValue", "MatrixVariable");
    private static final Set<String> SCALAR_TYPES = Set.of(
            "java.lang.String", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
            "java.lang.Long", "java.lang.Float", "java.lang.Double", "java.lang.Character", "java.util.UUID",
            "java.math.BigInteger", "java.math.BigDecimal", "java.time.LocalDate", "java.time.LocalDateTime",
            "java.time.Instant", "java.time.OffsetDateTime", "java.time.ZonedDateTime", "java.util.Date");
    private static final Set<String> PRIMITIVES = Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char");
    private static final Set<String> SCALAR_CONTAINERS = Set.of("java.util.Optional", "java.util.List",
            "java.util.Set", "java.util.Collection");

    /**
     * 本次扫描已报告的接口模型字段
     */
    private final Set<FieldLocation> reportedFields = new HashSet<>();

    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 当前文件可查找的接口模型
     */
    private ApiSourceModels models;

    /**
     * 当前文件的语法树根节点
     */
    private DetailAST root;

    /**
     * 监听可能声明 REST 接口的方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_DEF};
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
        if (!FrameworkMethods.isRestEndpoint(source, method) || isHiddenEndpoint(method, source)) {
            return;
        }
        DetailAST operation = annotation(method, source, OAS + "Operation");
        DetailAST methodName = JavaSource.declarationName(method);
        if (!text(operation, "summary")) {
            log(methodName, "api.source.operation");
        }
        if (models == null) {
            models = new ApiSourceModels(Path.of(getFilePath()).toAbsolutePath().normalize(), root, source);
        }

        Set<String> visited = new HashSet<>();
        List<DetailAST> parameterDocs = new ArrayList<>(annotations(method, source, PARAMETER));
        parameterDocs.addAll(nested(annotation(method, source, OAS + "Parameters"), source, "value", PARAMETER));
        parameterDocs.addAll(nested(operation, source, "parameters", PARAMETER));
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
     * @param method        接口方法声明
     * @param operation     接口的 Operation 注解，可为空
     * @param parameter     待检查的参数声明
     * @param parameterDocs 方法级参数说明
     * @param visited       本次请求已遍历的模型名称
     */
    private void checkParameter(DetailAST method, DetailAST operation, DetailAST parameter,
                                List<DetailAST> parameterDocs, Set<String> visited) {
        DetailAST name = JavaSource.declarationName(parameter);
        DetailAST directDoc = annotation(parameter, source, PARAMETER);
        if (!isVisibleParameter(parameter, source) || FrameworkMethods.isInternalParameter(parameter, source)) {
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
            if (body && !description(annotation(parameter, source, REQUEST_BODY_DOC))
                    && !description(annotation(method, source, REQUEST_BODY_DOC))
                    && nested(operation, source, "requestBody", REQUEST_BODY_DOC).stream()
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
     * @param name          参数名节点
     * @param documentation 直接标注的 Parameter 注解，可为空
     * @param binding       参数绑定注解，可为空
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
     * @param docs     候选 Parameter 注解
     * @param binding  参数绑定注解，可为空
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
                    && (flag(doc, HIDDEN_ATTRIBUTE) || description(doc));
        });
    }

    /**
     * 读取绑定名称并在省略时采用参数名
     *
     * @param binding  参数绑定注解，可为空
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
     * @param name        参数的类型名
     * @param declaration 参数声明节点
     * @return 类型可按标量处理时为 true
     */
    private boolean isScalar(String name, DetailAST declaration) {
        if (SCALAR_TYPES.stream().anyMatch(key -> source.isKnownType(name, key))) {
            return true;
        }
        if (name.isEmpty() || ApiSourceModels.isTypeParameter(declaration, name)) {
            return false;
        }
        ApiSourceModels.Model model = models.find(name, source, declaration);
        return model != null && model.tree().getType() == TokenTypes.ENUM_DEF;
    }

    /**
     * 查找并检查声明类型所引用的源码模型
     *
     * @param declaration     带有类型的声明节点
     * @param target          报告诊断的位置
     * @param declaringSource 声明所在文件的名称索引
     * @param visited         当前方向已遍历的模型名称
     * @param direction       请求或响应方向
     * @return 找到至少一个源码模型时为 true
     */
    private boolean checkModels(DetailAST declaration, DetailAST target, JavaSource declaringSource, Set<String> visited,
                                Direction direction) {
        var referenced = models.referenced(declaration.findFirstToken(TokenTypes.TYPE), declaringSource, declaration);
        for (ApiSourceModels.Model model : referenced) {
            checkModel(model, target, visited, direction);
        }
        return !referenced.isEmpty();
    }

    /**
     * 检查模型及其父类的对外字段说明
     *
     * @param model     待检查的模型
     * @param target    报告诊断的位置
     * @param visited   当前方向已遍历的模型名称
     * @param direction 请求或响应方向
     */
    private void checkModel(ApiSourceModels.Model model, DetailAST target, Set<String> visited, Direction direction) {
        if (!visited.add(model.name()) || hidden(model.tree(), model.source())) {
            return;
        }
        for (var field : ApiSourceModels.members(model.tree())) {
            if (JavaSource.isInstanceFieldOrComponent(field)) {
                checkField(model, field, target, visited, direction);
            }
        }
        var parent = model.tree().findFirstToken(TokenTypes.EXTENDS_CLAUSE);
        models.referenced(parent, model.source(), model.tree())
                .forEach(base -> checkModel(base, target, visited, direction));
    }

    /**
     * 检查可见字段的说明及其引用模型
     *
     * @param model     字段所属的模型
     * @param field     字段或 record 组件
     * @param target    报告诊断的位置
     * @param visited   当前方向已遍历的模型名称
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
        boolean described = text(schema, DESCRIPTION_ATTRIBUTE)
                || nested(arraySchema, declaringSource, "arraySchema", SCHEMA).stream()
                .anyMatch(value -> text(value, DESCRIPTION_ATTRIBUTE));
        if (!described) {
            DetailAST name = JavaSource.declarationName(field);
            if (reportedFields.add(new FieldLocation(model.name(), name.getText()))) {
                log(target, "api.source.field", model.name(), name.getText(), name.getLineNo());
            }
        }
        checkModels(field, target, declaringSource, visited, direction);
    }

    /**
     * 读取注解属性中写出的枚举常量名
     *
     * @param annotation 待检查的注解，可为空
     * @param key        属性名
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
        return text(annotation, DESCRIPTION_ATTRIBUTE) || text(annotation, "ref");
    }

    /**
     * 检查注解属性是否存在且未明确为空白
     *
     * @param annotation 待检查的注解，可为空
     * @param key        文本属性名
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
     * @param source     注解所在文件的名称索引
     * @param attribute  属性名
     * @param type       注解类型的全限定名
     * @return 匹配的嵌套注解
     */
    private static List<DetailAST> nested(DetailAST annotation, JavaSource source, String attribute, String type) {
        return annotation == null ? List.of() : JavaSource.annotationValues(annotation, attribute).stream()
                .filter(node -> source.isKnownType(JavaSource.annotationName(node), type)).toList();
    }

    /**
     * 区分请求和响应方向的字段可见性
     *
     * @author lonelight
     */
    private enum Direction {
        REQUEST("READ_ONLY"),
        RESPONSE("WRITE_ONLY");

        /**
         * 当前请求或响应方向跳过的字段访问模式
         */
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
     * @param name  字段名称
     * @author lonelight
     */
    private record FieldLocation(String model, String name) {
    }
}
