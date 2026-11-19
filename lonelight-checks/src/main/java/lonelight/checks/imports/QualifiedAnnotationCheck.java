package lonelight.checks.imports;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

/**
 * 报告可安全改用短名的全限定注解
 *
 * @author lonelight
 */
public final class QualifiedAnnotationCheck extends RuleCheck {
    private JavaSource source;
    private final Map<String, Set<String>> annotationsByName = new HashMap<>();
    private final Set<String> shortTypeNames = new HashSet<>();

    /**
     * 监听需要检查的注解
     *
     * @return 注解节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.ANNOTATION};
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
            for (DetailAST ident : JavaSource.descendants(node, TokenTypes.IDENT)) {
                if (isShortType(ident)) {
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

    /**
     * 判断标识符是否处于简单类型引用位置
     *
     * @param ident 待检查的标识符节点
     * @return 属于规则识别的类型位置时为 true
     */
    private static boolean isShortType(DetailAST ident) {
        int parent = ident.getParent().getType();
        return parent == TokenTypes.TYPE || parent == TokenTypes.TYPE_ARGUMENT || parent == TokenTypes.ARRAY_DECLARATOR
                || parent == TokenTypes.EXTENDS_CLAUSE || parent == TokenTypes.IMPLEMENTS_CLAUSE
                || parent == TokenTypes.LITERAL_NEW;
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
}
