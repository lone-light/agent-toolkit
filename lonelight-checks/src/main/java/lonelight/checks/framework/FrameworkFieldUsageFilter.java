package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.TreeWalkerAuditEvent;
import com.puppycrawl.tools.checkstyle.TreeWalkerFilter;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;

import java.util.List;
import java.util.Set;

/**
 * 按框架注解和 unused 标记过滤未使用私有字段的诊断
 *
 * @author lonelight
 */
public final class FrameworkFieldUsageFilter extends AbstractAutomaticBean implements TreeWalkerFilter {
    private static final Set<String> LOMBOK_TYPE_ANNOTATIONS = Set.of(
            "lombok.Getter", "lombok.Setter", "lombok.Data", "lombok.Value");

    /**
     * 可豁免未使用检查的字段注解
     */
    private Set<String> fieldAnnotations = Set.of();

    /**
     * 可豁免未使用检查的所属类型注解
     */
    private Set<String> typeAnnotations = Set.of();

    /**
     * 当前文件的源码索引
     */
    private JavaSource source;
    /**
     * 当前文件的候选字段声明
     */
    private List<DetailAST> fields = List.of();

    /**
     * 判断声明是否由框架使用或显式豁免
     *
     * @param field       检查的字段
     * @param declaration 字段或类型声明节点
     * @param allowed     允许豁免的注解全限定名
     * @param source      当前文件的源码索引
     * @return 存在生效的豁免注解时为 true
     */
    private static boolean hasExemption(DetailAST field, DetailAST declaration, Set<String> allowed, JavaSource source) {
        for (DetailAST annotation : JavaSource.annotations(declaration)) {
            for (String name : allowed) {
                if (source.isKnownAnnotationType(annotation, name)
                        && appliesToField(annotation, name, field, declaration, source)) {
                    return true;
                }
            }
        }
        return hasUnusedSuppression(declaration, source);
    }

    /**
     * 按字段修饰符和字段级覆盖判断 Lombok 注解是否生效
     *
     * @param annotation  当前注解
     * @param name        注解全限定名
     * @param field       检查的字段
     * @param declaration 注解所属字段或类型
     * @param source      当前文件的源码索引
     * @return 注解可用于此字段时为 true
     */
    private static boolean appliesToField(DetailAST annotation, String name, DetailAST field,
                                          DetailAST declaration, JavaSource source) {
        if (field != declaration && LOMBOK_TYPE_ANNOTATIONS.contains(name) && JavaSource.isExplicitlyStatic(field)) {
            return false;
        }
        if (!name.equals("lombok.Getter") && !name.equals("lombok.Setter")) {
            return true;
        }
        boolean overridden = field != declaration && source.hasKnownAnnotation(field, name);
        boolean finalField = field.findFirstToken(TokenTypes.MODIFIERS).findFirstToken(TokenTypes.FINAL) != null;
        if (overridden || name.equals("lombok.Setter") && finalField) {
            return false;
        }
        DetailAST value = JavaSource.attribute(annotation, "value");
        DetailAST level = value == null ? null : JavaSource.firstOperand(value);
        if (level != null && level.getType() == TokenTypes.DOT) {
            level = level.getLastChild();
        }
        return level == null || !level.getText().equals("NONE");
    }

    /**
     * 识别显式声明的 unused 豁免
     *
     * @param declaration 字段或类型声明节点
     * @param source      当前文件的源码索引
     * @return 明确抑制 unused 警告时为 true
     */
    private static boolean hasUnusedSuppression(DetailAST declaration, JavaSource source) {
        return JavaSource.annotations(declaration).stream()
                .filter(annotation -> source.isKnownAnnotationType(annotation, "java.lang.SuppressWarnings"))
                .flatMap(annotation -> JavaSource.descendants(annotation, TokenTypes.STRING_LITERAL).stream())
                .anyMatch(literal -> literal.getText().equals("\"unused\""));
    }

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
    protected void finishLocalSetup() {
        // 配置注解列表时已完成初始化
    }

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
        JavaSource current = JavaSource.forEvent(event, source);
        if (source != current) {
            fields = JavaSource.descendants(event.rootAst(), TokenTypes.VARIABLE_DEF);
            source = current;
        }
        for (DetailAST field : fields) {
            DetailAST name = JavaSource.declarationName(field);
            if (name.getLineNo() != event.getLine() || name.getColumnNo() != event.getColumnCharIndex()) {
                continue;
            }
            if (hasExemption(field, field, fieldAnnotations, source)) {
                return false;
            }
            DetailAST type = JavaSource.enclosingType(field);
            return type == null || !hasExemption(field, type, typeAnnotations, source);
        }
        return true;
    }
}
