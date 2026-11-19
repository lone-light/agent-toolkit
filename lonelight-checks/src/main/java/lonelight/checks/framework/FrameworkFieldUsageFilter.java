package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.TreeWalkerAuditEvent;
import com.puppycrawl.tools.checkstyle.TreeWalkerFilter;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.Set;
import lonelight.checks.JavaSource;

/**
 * 按框架注解和 unused 标记过滤未使用私有字段的诊断
 *
 * @author lonelight
 */
public final class FrameworkFieldUsageFilter extends AbstractAutomaticBean implements TreeWalkerFilter {
    private Set<String> fieldAnnotations = Set.of();
    private Set<String> typeAnnotations = Set.of();

    /**
     * 供 Checkstyle 从 XML 配置可豁免未使用检查的字段注解
     *
     * @param values 字段注解的全限定名
     */
    @SuppressWarnings("unused")
    public void setFieldAnnotations(String... values) {
        fieldAnnotations = Set.of(values);
    }

    /**
     * 供 Checkstyle 从 XML 配置可豁免未使用检查的类型注解
     *
     * @param values 类型注解的全限定名
     */
    @SuppressWarnings("unused")
    public void setTypeAnnotations(String... values) {
        typeAnnotations = Set.of(values);
    }

    /**
     * 完成配置阶段，注解列表由配置项直接提供
     */
    @Override
    protected void finishLocalSetup() { }

    /**
     * 过滤符合豁免条件的未使用字段诊断
     *
     * @param event 待处理的扫描事件
     * @return 应保留诊断时为 true
     */
    @Override
    public boolean accept(TreeWalkerAuditEvent event) {
        if (!event.getSourceName().endsWith(".UnusedPrivateFieldCheck")) {
            return true;
        }
        JavaSource source = new JavaSource(event.rootAst());
        for (DetailAST field : JavaSource.descendants(event.rootAst(), TokenTypes.VARIABLE_DEF)) {
            DetailAST name = JavaSource.declarationName(field);
            if (name.getLineNo() != event.getLine() || name.getColumnNo() != event.getColumnCharIndex()) {
                continue;
            }
            if (hasExemption(field, fieldAnnotations, source)) {
                return false;
            }
            DetailAST type = JavaSource.enclosingType(field);
            return type == null || !hasExemption(type, typeAnnotations, source);
        }
        return true;
    }

    /**
     * 判断声明是否由框架使用或显式豁免
     *
     * @param declaration 字段或类型声明节点
     * @param allowed 允许豁免的注解全限定名
     * @param source 当前文件的名称索引
     * @return 存在生效的豁免注解时为 true
     */
    private static boolean hasExemption(DetailAST declaration, Set<String> allowed, JavaSource source) {
        for (DetailAST annotation : JavaSource.annotations(declaration)) {
            if (allowed.stream().anyMatch(name -> source.isType(JavaSource.annotationName(annotation), name))) {
                DetailAST value = JavaSource.attribute(annotation, "value");
                boolean disabled = value != null && JavaSource.descendants(value, TokenTypes.IDENT).stream()
                        .anyMatch(identifier -> identifier.getText().equals("NONE"));
                if (!disabled) {
                    return true;
                }
            }
        }
        return hasUnusedSuppression(declaration, source);
    }

    /**
     * 识别显式声明的 unused 豁免
     *
     * @param declaration 字段或类型声明节点
     * @param source 当前文件的名称索引
     * @return 明确抑制 unused 警告时为 true
     */
    private static boolean hasUnusedSuppression(DetailAST declaration, JavaSource source) {
        return JavaSource.annotations(declaration).stream()
                .filter(annotation -> source.isType(JavaSource.annotationName(annotation), "java.lang.SuppressWarnings"))
                .flatMap(annotation -> JavaSource.descendants(annotation, TokenTypes.STRING_LITERAL).stream())
                .anyMatch(literal -> literal.getText().equals("\"unused\""));
    }
}
