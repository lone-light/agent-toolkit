package lonelight.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 解析单份 Java 源码中的类型和注解名称
 *
 * @author lonelight
 */
public final class JavaSource {
    private static final int UNICODE_ESCAPE_DIGITS = 4;
    private static final int HEX_RADIX = 16;
    private static final Pattern HEX_DIGITS = Pattern.compile("[0-9a-fA-F]+");

    /**
     * 显式导入的类型短名与全限定名
     */
    private final Map<String, String> imports = new HashMap<>();

    /**
     * 使用通配导入的包名
     */
    private final Set<String> wildcardImports = new HashSet<>();

    /**
     * 当前文件的类型声明，按短名索引
     */
    private final Map<String, List<DetailAST>> declaredTypes = new HashMap<>();

    /**
     * 当前文件声明的泛型类型参数名
     */
    private final Set<String> typeParameterNames = new HashSet<>();

    /**
     * 静态导入中使用的成员全限定名
     */
    private final Set<String> staticImports = new HashSet<>();

    /**
     * 当前文件声明的包名
     */
    private String packageName = "";

    /**
     * 为单个 Java 文件建立名称索引
     *
     * @param root 源码语法树根节点
     */
    public JavaSource(DetailAST root) {
        for (DetailAST node = root; node != null; node = node.getNextSibling()) {
            readDeclaredTypes(node);
            readHeader(node);
            if (node.getType() == TokenTypes.COMPILATION_UNIT) {
                for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    readHeader(child);
                }
            }
        }
    }

    /**
     * 判断同一语法块的类型声明是否已进入作用域
     *
     * @param candidate  候选类型声明
     * @param scope      候选作用域
     * @param annotation 注解节点
     * @return 声明在对应作用域内可见时为 true
     */
    private static boolean isDeclaredInScope(DetailAST candidate, DetailAST scope, DetailAST annotation) {
        if (candidate.getParent() != scope) {
            return false;
        }
        return scope.getType() != TokenTypes.SLIST || candidate.getLineNo() < annotation.getLineNo()
                || candidate.getLineNo() == annotation.getLineNo()
                && candidate.getColumnNo() <= annotation.getColumnNo();
    }

    /**
     * 提取声明上直接出现的注解
     *
     * @param declaration 声明节点
     * @return 按源码顺序排列的注解
     */
    public static List<DetailAST> annotations(DetailAST declaration) {
        DetailAST modifiers = declaration.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiers == null && declaration.getType() == TokenTypes.RECORD_COMPONENT_DEF) {
            modifiers = declaration.findFirstToken(TokenTypes.ANNOTATIONS);
        }
        List<DetailAST> result = new ArrayList<>();
        if (modifiers != null) {
            for (DetailAST node = modifiers.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getType() == TokenTypes.ANNOTATION) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    /**
     * 读取注解在源码中的名称
     *
     * @param annotation 注解节点
     * @return 注解的简单名或限定名
     */
    public static String annotationName(DetailAST annotation) {
        DetailAST name = annotation.findFirstToken(TokenTypes.DOT);
        return FullIdent.createFullIdent(name == null ? annotation.findFirstToken(TokenTypes.IDENT) : name).getText();
    }

    /**
     * 查找最近的外层类型声明
     *
     * @param node 起始节点
     * @return 最近的外层类型，找不到时为 null
     */
    public static DetailAST enclosingType(DetailAST node) {
        for (DetailAST parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getType() == TokenTypes.CLASS_DEF || parent.getType() == TokenTypes.RECORD_DEF
                    || parent.getType() == TokenTypes.ENUM_DEF || parent.getType() == TokenTypes.INTERFACE_DEF) {
                return parent;
            }
        }
        return null;
    }

    /**
     * 收集子树中指定类别的节点
     *
     * @param node  子树根节点，可为空
     * @param token 目标节点类别
     * @return 按遍历顺序收集的节点，包含匹配的根节点
     */
    public static List<DetailAST> descendants(DetailAST node, int token) {
        List<DetailAST> result = new ArrayList<>();
        if (node != null) {
            collect(node, token, result);
        }
        return result;
    }

    /**
     * 读取语法节点的直接子节点
     *
     * @param detailAST 待读取的节点，可为空
     * @return 按源码顺序排列的子节点
     */
    public static List<DetailAST> children(DetailAST detailAST) {
        List<DetailAST> children = new ArrayList<>();
        for (DetailAST childDetailAST = detailAST == null ? null : detailAST.getFirstChild(); childDetailAST != null; childDetailAST = childDetailAST.getNextSibling()) {
            children.add(childDetailAST);
        }
        return children;
    }

    /**
     * 递归收集符合类别的节点
     *
     * @param node   当前节点
     * @param token  目标节点类别
     * @param result 收集结果
     */
    private static void collect(DetailAST node, int token, List<DetailAST> result) {
        if (node.getType() == token) {
            result.add(node);
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            collect(child, token, result);
        }
    }

    /**
     * 获取具名声明中的标识符节点
     *
     * @param declarationDetailAST 按语法带有标识符的声明节点
     * @return 声明的标识符节点
     */
    public static DetailAST declarationName(DetailAST declarationDetailAST) {
        return Objects.requireNonNull(declarationDetailAST.findFirstToken(TokenTypes.IDENT),
                () -> "Missing declaration identifier: " + declarationDetailAST);
    }

    /**
     * 判断声明是否有显式 static 修饰符
     *
     * @param declarationDetailAST 待检查的声明节点
     * @return 含有 static 修饰符时为 true
     */
    public static boolean isExplicitlyStatic(DetailAST declarationDetailAST) {
        var modifiers = declarationDetailAST.findFirstToken(TokenTypes.MODIFIERS);
        return modifiers != null && modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null;
    }

    /**
     * 识别 record 组件和未显式标记为 static 的字段
     *
     * @param declarationDetailAST 待检查的声明节点
     * @return 属于规则检查的字段或组件时为 true
     */
    public static boolean isInstanceFieldOrComponent(DetailAST declarationDetailAST) {
        return declarationDetailAST.getType() == TokenTypes.RECORD_COMPONENT_DEF
                || declarationDetailAST.getType() == TokenTypes.VARIABLE_DEF
                && declarationDetailAST.getParent().getType() == TokenTypes.OBJBLOCK
                && !isExplicitlyStatic(declarationDetailAST);
    }

    /**
     * 提取类型节点中的名称
     *
     * @param type 类型节点，可为空
     * @return 读到的名称，没有节点或名称时为空字符串
     */
    public static String typeName(DetailAST type) {
        if (type == null) {
            return "";
        }
        for (DetailAST child = type.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.IDENT || child.getType() == TokenTypes.DOT) {
                return FullIdent.createFullIdent(child).getText();
            }
        }
        return "";
    }

    /**
     * 从限定名中提取最后一段
     *
     * @param name 类型名或限定名
     * @return 最后一段名称
     */
    public static String simpleName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * 查找注解属性对应的表达式
     *
     * @param annotation 注解节点
     * @param name       属性名
     * @return 属性表达式，缺失时为 null
     */
    public static DetailAST attribute(DetailAST annotation, String name) {
        for (DetailAST child = annotation.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR
                    && child.getFirstChild().getText().equals(name)) {
                return child.findFirstToken(TokenTypes.EXPR);
            }
        }
        return name.equals("value") ? annotation.findFirstToken(TokenTypes.EXPR) : null;
    }

    /**
     * 提取注解属性中的单个或成组注解
     *
     * @param annotation 外层注解节点
     * @param name       属性名，省略属性名时为 value
     * @return 按源码顺序排列的嵌套注解
     */
    public static List<DetailAST> annotationValues(DetailAST annotation, String name) {
        var value = findAnnotationValue(annotation, name);
        if (value == null) {
            return List.of();
        }
        if (value.getType() == TokenTypes.ANNOTATION) {
            return List.of(value);
        }
        List<DetailAST> result = new ArrayList<>();
        if (value.getType() == TokenTypes.ANNOTATION_ARRAY_INIT) {
            for (DetailAST child = value.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getType() == TokenTypes.ANNOTATION) {
                    result.add(child);
                }
            }
        }
        return result;
    }

    /**
     * 查找显式指定或省略属性名的注解值节点
     *
     * @param annotationDetailAST 外层注解
     * @param name                属性名
     * @return 匹配的值节点，找不到时为 null
     */
    private static DetailAST findAnnotationValue(DetailAST annotationDetailAST, String name) {
        boolean implicit = name.equals("value");
        for (var child = annotationDetailAST.getFirstChild(); child != null; child = child.getNextSibling()) {
            var value = switch (child.getType()) {
                case TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR -> {
                    String attributeName = child.getFirstChild().getText();
                    yield attributeName.equals(name) ? child.getLastChild() : null;
                }
                case TokenTypes.ANNOTATION, TokenTypes.ANNOTATION_ARRAY_INIT -> implicit ? child : null;
                default -> null;
            };
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 读取字符串字面量表达式并解码转义
     *
     * @param expression 待读取的表达式节点
     * @return 读出的文本，无法静态读取时为 null
     * @throws IllegalArgumentException 转义无法解码时
     */
    public static String literalText(DetailAST expression) {
        if (expression == null) {
            return null;
        }
        if (expression.getType() == TokenTypes.EXPR) {
            return literalText(expression.getFirstChild());
        }
        if (expression.getType() == TokenTypes.PLUS) {
            String left = literalText(expression.getFirstChild());
            String right = literalText(expression.getLastChild());
            return left == null || right == null ? null : left + right;
        }
        if (expression.getType() == TokenTypes.TEXT_BLOCK_LITERAL_BEGIN) {
            DetailAST content = expression.findFirstToken(TokenTypes.TEXT_BLOCK_CONTENT);
            return content == null ? "" : decodeUnicode(content.getText()).stripIndent().translateEscapes();
        }
        if (expression.getType() != TokenTypes.STRING_LITERAL) {
            return null;
        }
        String text = expression.getText();
        return decodeUnicode(text.substring(1, text.length() - 1)).translateEscapes();
    }

    /**
     * 按反斜杠的奇偶性解码 Unicode 转义
     *
     * @param text 不含外层引号的字面量内容
     * @return 解码后的文本
     */
    private static String decodeUnicode(String text) {
        StringBuilder result = new StringBuilder();
        int backslashes = 0;
        int index = 0;
        while (index < text.length()) {
            char character = text.charAt(index);
            int nextIndex = index + 1;
            int digits = character == '\\' && backslashes % 2 == 0 ? unicodeDigitsStart(text, nextIndex) : -1;
            if (digits >= 0) {
                nextIndex = digits + UNICODE_ESCAPE_DIGITS;
                character = (char) Integer.parseInt(text, digits, nextIndex, HEX_RADIX);
            }
            result.append(character);
            backslashes = character == '\\' ? backslashes + 1 : 0;
            index = nextIndex;
        }
        return result.toString();
    }

    /**
     * 定位 Unicode 转义的十六进制数字
     *
     * @param text  含转义的文本
     * @param start 反斜杠之后的位置
     * @return 数字起点，无有效转义时为 -1
     */
    private static int unicodeDigitsStart(String text, int start) {
        int digits = start;
        while (digits < text.length() && text.charAt(digits) == 'u') {
            digits++;
        }
        int end = digits + UNICODE_ESCAPE_DIGITS;
        return digits > start && end <= text.length() && HEX_DIGITS.matcher(text).region(digits, end).matches()
                ? digits : -1;
    }

    /**
     * 收集声明的类型名和泛型参数名
     *
     * @param node 当前语法树节点
     */
    private void readDeclaredTypes(DetailAST node) {
        switch (node.getType()) {
            case TokenTypes.CLASS_DEF, TokenTypes.INTERFACE_DEF, TokenTypes.RECORD_DEF,
                 TokenTypes.ENUM_DEF, TokenTypes.ANNOTATION_DEF -> declaredTypes
                    .computeIfAbsent(declarationName(node).getText(), ignored -> new ArrayList<>()).add(node);
            case TokenTypes.TYPE_PARAMETER -> typeParameterNames.add(declarationName(node).getText());
            default -> {
                // 其余节点只继续检查子节点
            }
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            readDeclaredTypes(child);
        }
    }

    /**
     * 记录包名及普通、静态和通配导入
     *
     * @param node 待识别的文件头节点
     */
    private void readHeader(DetailAST node) {
        if (node.getType() == TokenTypes.IMPORT) {
            String name = FullIdent.createFullIdentBelow(node).getText();
            if (name.endsWith(".*")) {
                wildcardImports.add(name.substring(0, name.length() - 2));
            } else {
                imports.put(simpleName(name), name);
            }
        } else if (node.getType() == TokenTypes.STATIC_IMPORT) {
            var qualifier = node.findFirstToken(TokenTypes.DOT);
            staticImports.add(FullIdent.createFullIdent(qualifier).getText());
        } else if (node.getType() == TokenTypes.PACKAGE_DEF) {
            var qualifier = node.findFirstToken(TokenTypes.DOT);
            var name = qualifier == null ? node.findFirstToken(TokenTypes.IDENT) : qualifier;
            packageName = FullIdent.createFullIdent(name).getText();
        }
    }

    /**
     * 根据显式导入和当前包名解析类型名称
     *
     * @param written 源码中的类型名
     * @return 解析后的类型名
     */
    public String resolve(String written) {
        int dot = written.indexOf('.');
        String first = dot < 0 ? written : written.substring(0, dot);
        String imported = imports.get(first);
        if (imported != null) {
            return imported + written.substring(first.length());
        }
        if (dot >= 0) {
            return written;
        }
        return packageName.isEmpty() ? written : packageName + "." + written;
    }

    /**
     * 取得源码声明的包名
     *
     * @return 包名，未声明时为空字符串
     */
    public String packageName() {
        return packageName;
    }

    /**
     * 判断类型能否安全地使用短名
     *
     * @param canonical 全限定类型名
     * @return 未发现同名冲突或导入歧义时为 true
     */
    public boolean canShortenQualifiedName(String canonical) {
        String simple = simpleName(canonical);
        if (declaredTypes.containsKey(simple) || typeParameterNames.contains(simple)
                || staticImports.stream().map(JavaSource::simpleName).anyMatch(name -> name.equals(simple) || name.equals("*"))) {
            return false;
        }
        String imported = imports.get(simple);
        if (imported != null) {
            return imported.equals(canonical);
        }
        String qualifier = canonical.substring(0, canonical.lastIndexOf('.'));
        return wildcardImports.isEmpty() || wildcardImports.size() == 1 && wildcardImports.contains(qualifier);
    }

    /**
     * 生成定位类型源码的候选全名
     *
     * @param written 源码中的类型名
     * @return 按查找顺序排列的候选名称
     */
    public List<String> sourceTypeNames(String written) {
        String resolved = resolve(written);
        String first = written.split("\\.", 2)[0];
        if (imports.containsKey(first)) {
            return List.of(resolved);
        }
        String local = packageName.isEmpty() ? written : packageName + "." + written;
        if (written.indexOf('.') >= 0 && resolved.equals(written)) {
            return local.equals(written) ? List.of(written) : List.of(written, local);
        }
        List<String> names = new ArrayList<>();
        names.add(local);
        if (!declaredTypes.containsKey(first)) {
            wildcardImports.stream().sorted().map(owner -> owner + "." + written).forEach(names::add);
        }
        return names;
    }

    /**
     * 按包名和导入判断类型是否匹配
     *
     * @param written   源码中的类型名
     * @param canonical 预期的全限定类型名
     * @return 可以确定匹配时为 true
     */
    public boolean isType(String written, String canonical) {
        return resolve(written).equals(canonical)
                || (!written.contains(".") && !imports.containsKey(written) && !declaredTypes.containsKey(written)
                && canonical.startsWith("java.lang.") && written.equals(simpleName(canonical)))
                || (!written.contains(".") && !imports.containsKey(written) && !declaredTypes.containsKey(written)
                && wildcardImports.size() == 1
                && wildcardImports.contains(canonical.substring(0, canonical.lastIndexOf('.')))
                && written.equals(simpleName(canonical)));
    }

    /**
     * 检查声明是否带有指定注解
     *
     * @param declaration 待检查的声明节点
     * @param canonical   注解的全限定名
     * @return 存在匹配注解时为 true
     */
    public boolean hasAnnotation(DetailAST declaration, String canonical) {
        return annotations(declaration).stream().anyMatch(annotation -> isType(annotationName(annotation), canonical));
    }

    /**
     * 允许通过通配导入匹配已知类型
     *
     * @param written   源码中的类型名
     * @param canonical 预期的全限定类型名
     * @return 可以匹配时为 true
     */
    public boolean isKnownType(String written, String canonical) {
        return isType(written, canonical)
                || !written.contains(".") && !imports.containsKey(written) && !declaredTypes.containsKey(written)
                && written.equals(simpleName(canonical))
                && wildcardImports.contains(canonical.substring(0, canonical.lastIndexOf('.')));
    }

    /**
     * 检查声明是否带有可识别的已知注解
     *
     * @param declaration 待检查的声明节点
     * @param canonical   注解的全限定名
     * @return 存在匹配注解时为 true
     */
    public boolean hasKnownAnnotation(DetailAST declaration, String canonical) {
        return annotations(declaration).stream().anyMatch(annotation -> isKnownAnnotationType(annotation, canonical));
    }

    /**
     * 按可见的同名声明和导入识别已知注解
     *
     * @param annotation 注解节点
     * @param canonical  注解的全限定名
     * @return 源码中的注解匹配已知类型时为 true
     */
    public boolean isKnownAnnotationType(DetailAST annotation, String canonical) {
        return isKnownAnnotationType(annotation, canonical, ignored -> false);
    }

    /**
     * 识别已知注解，并区分同包类型与按需导入
     *
     * @param annotationDetailAST  注解节点
     * @param canonical            注解的全限定名
     * @param packageTypePredicate 判断本包中是否存在传入短名的类型
     * @return 源码中的注解匹配已知类型时为 true
     */
    public boolean isKnownAnnotationType(DetailAST annotationDetailAST, String canonical,
                                         Predicate<String> packageTypePredicate) {
        String written = annotationName(annotationDetailAST);
        if (written.contains(".")) {
            return resolve(written).equals(canonical);
        }
        if (!written.equals(simpleName(canonical))) {
            return false;
        }
        var visible = visibleType(annotationDetailAST, written);
        if (visible != null) {
            return visible.getType() == TokenTypes.ANNOTATION_DEF && canonical.equals(declaredTypeName(visible));
        }
        String imported = imports.get(written);
        if (imported != null) {
            return imported.equals(canonical);
        }
        String qualifier = canonical.substring(0, canonical.lastIndexOf('.'));
        return qualifier.equals(packageName) || (qualifier.equals("java.lang") || wildcardImports.contains(qualifier))
                && !packageTypePredicate.test(written);
    }

    /**
     * 查找当前注解位置可见的同名类型
     *
     * @param annotation 注解节点
     * @param simple     要查找的类型短名
     * @return 最近的同名类型声明，找不到时为 null
     */
    private DetailAST visibleType(DetailAST annotation, String simple) {
        var candidates = declaredTypes.get(simple);
        if (candidates == null) {
            return null;
        }
        for (DetailAST scope = annotation.getParent(); scope != null; scope = scope.getParent()) {
            for (DetailAST candidate : candidates) {
                if (isDeclaredInScope(candidate, scope, annotation)) {
                    return candidate;
                }
            }
        }
        return candidates.stream().filter(candidate -> candidate.getParent() == null).findFirst().orElse(null);
    }

    /**
     * 读取顶层类型或成员类型的全限定名
     *
     * @param declaration 类型声明节点
     * @return 全限定名，局部类型或匿名类内的类型返回空字符串
     */
    public String declaredTypeName(DetailAST declaration) {
        Deque<String> names = new ArrayDeque<>();
        names.addFirst(declarationName(declaration).getText());
        DetailAST scope = declaration.getParent();
        while (scope != null && scope.getType() == TokenTypes.OBJBLOCK) {
            var owner = scope.getParent();
            var identifier = owner.findFirstToken(TokenTypes.IDENT);
            if (identifier == null) {
                return "";
            }
            names.addFirst(identifier.getText());
            scope = owner.getParent();
        }
        if (scope != null && scope.getType() != TokenTypes.COMPILATION_UNIT) {
            return "";
        }
        if (!packageName.isEmpty()) {
            names.addFirst(packageName);
        }
        return String.join(".", names);
    }

    /**
     * 判断未限定的静态成员名能否从导入定位到目标类
     *
     * @param owner  目标类全限定名
     * @param member 静态成员短名
     * @return 显式导入或无其他通配导入的目标成员时为 true
     */
    public boolean hasStaticImport(String owner, String member) {
        var explicit = staticImports.stream().filter(imported -> simpleName(imported).equals(member)).toList();
        if (!explicit.isEmpty()) {
            return explicit.size() == 1 && explicit.getFirst().equals(owner + "." + member);
        }
        return staticImports.contains(owner + ".*")
                && staticImports.stream().filter(imported -> imported.endsWith(".*")).count() == 1;
    }

    /**
     * 查找类型子树中首个匹配正则的类型
     *
     * @param node    待查找的子树根节点
     * @param pattern 匹配全限定类型名的正则表达式
     * @return 首个匹配的类型引用，找不到时为 null
     */
    public TypeReference firstMatchingType(DetailAST node, Pattern pattern) {
        if (node.getType() == TokenTypes.ANNOTATION) {
            return null;
        }
        if ((node.getType() == TokenTypes.IDENT || node.getType() == TokenTypes.DOT)
                && node.getParent().getType() != TokenTypes.DOT) {
            String written = node.getType() == TokenTypes.IDENT
                    ? node.getText() : FullIdent.createFullIdent(node).getText();
            int array = written.indexOf('[');
            String canonical = resolve(array < 0 ? written : written.substring(0, array));
            if (pattern.matcher(canonical).matches()) {
                return new TypeReference(node, canonical);
            }
        }
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            TypeReference match = firstMatchingType(child, pattern);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    /**
     * 保存匹配类型的节点和全限定名
     *
     * @param token         匹配到的类型语法树节点
     * @param canonicalName 解析后的完整类型名称
     * @author lonelight
     */
    public record TypeReference(DetailAST token, String canonicalName) {
    }
}
