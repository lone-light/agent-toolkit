package lonelight.checks.imports;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.JavaVariables;
import lonelight.checks.RuleCheck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 报告可安全改用短名的全限定注解
 *
 * @author lonelight
 */
public final class QualifiedAnnotationCheck extends RuleCheck {
    /**
     * 按短名索引的注解类型
     */
    private final Map<String, Set<String>> annotationsByName = new HashMap<>();
    /**
     * 当前文件中已使用的类型短名
     */
    private final Set<String> shortTypeNames = new HashSet<>();
    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 判断标识符是否处于简单类型引用位置
     *
     * @param identDetailAST 待检查的标识符节点
     * @param javaVariables  当前文件的变量索引
     * @return 属于类型位置或限定器无法解析为变量时为 true
     */
    private static boolean isShortType(DetailAST identDetailAST, JavaVariables javaVariables) {
        var qualifier = identDetailAST;
        var parent = qualifier.getParent();
        while (parent.getType() == TokenTypes.DOT && parent.getFirstChild() == qualifier) {
            if (parent.getLastChild().getType() == TokenTypes.LITERAL_CLASS) {
                return true;
            }
            qualifier = parent;
            parent = qualifier.getParent();
        }
        int kind = parent.getType();
        if (kind == TokenTypes.TYPE || kind == TokenTypes.TYPE_ARGUMENT || kind == TokenTypes.ARRAY_DECLARATOR
                || kind == TokenTypes.EXTENDS_CLAUSE || kind == TokenTypes.IMPLEMENTS_CLAUSE
                || kind == TokenTypes.LITERAL_NEW || kind == TokenTypes.PERMITS_CLAUSE || kind == TokenTypes.ANNOTATION) {
            return true;
        }
        boolean methodReference = kind == TokenTypes.METHOD_REF && parent.getFirstChild() == qualifier;
        if (methodReference && parent.getLastChild().getType() == TokenTypes.LITERAL_NEW) {
            return true;
        }
        return (qualifier != identDetailAST || methodReference)
                && javaVariables.find(identDetailAST, identDetailAST.getText(), false) == null;
    }

    /**
     * 判断注解名是否符合包限定名的形式
     *
     * @param name 源码中的注解名称
     * @return 前缀符合包名形式时为 true
     */
    private static boolean isPackageQualified(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 && name.substring(0, dot).codePoints()
                .allMatch(character -> character == '.' || character == '_' || Character.isLowerCase(character)
                        || Character.isDigit(character));
    }

    /**
     * 监听需要检查的注解
     *
     * @return 注解节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.ANNOTATION};
    }

    /**
     * 收集可能与注解短名冲突的名称
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
        annotationsByName.clear();
        shortTypeNames.clear();
        for (DetailAST node = root; node != null; node = node.getNextSibling()) {
            for (DetailAST annotation : JavaSource.descendants(node, TokenTypes.ANNOTATION)) {
                String written = JavaSource.annotationName(annotation);
                annotationsByName.computeIfAbsent(JavaSource.simpleName(written), ignored -> new HashSet<>()).add(written);
            }
        }
        if (annotationsByName.values().stream().flatMap(Set::stream).noneMatch(QualifiedAnnotationCheck::isPackageQualified)) {
            return;
        }
        var javaVariables = new JavaVariables(root);
        for (DetailAST node = root; node != null; node = node.getNextSibling()) {
            for (DetailAST ident : JavaSource.descendants(node, TokenTypes.IDENT)) {
                if (annotationsByName.containsKey(ident.getText()) && isShortType(ident, javaVariables)) {
                    shortTypeNames.add(ident.getText());
                }
            }
        }
    }

    /**
     * 报告能安全改用短名的全限定注解
     *
     * @param annotation 待检查的注解节点
     */
    @Override
    public void visitToken(DetailAST annotation) {
        String written = JavaSource.annotationName(annotation);
        String simple = JavaSource.simpleName(written);
        if (!isPackageQualified(written) || !source.canShortenQualifiedName(written)
                || shortTypeNames.contains(simple) && !source.isType(simple, written)
                || annotationsByName.get(simple).stream().anyMatch(other ->
                !other.equals(written) && (!other.equals(simple) || !source.isType(simple, written)))) {
            return;
        }
        log(annotation, "annotation.qualified", simple);
    }
}
