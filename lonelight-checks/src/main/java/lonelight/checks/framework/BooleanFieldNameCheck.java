package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 报告 REST 请求和响应模型中带 is 前缀的布尔属性
 *
 * @author lonelight
 */
public final class BooleanFieldNameCheck extends RuleCheck {
    /**
     * 已检查的模型名称
     */
    private final Set<String> visited = new HashSet<>();
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
     * 监听可能声明 REST 映射的方法
     *
     * @return 方法声明节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.METHOD_DEF};
    }

    /**
     * 重置当前文件的模型索引和去重状态
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        this.rootDetailAST = rootDetailAST;
        javaSource = indexSource(rootDetailAST);
        apiSourceModels = null;
        visited.clear();
    }

    /**
     * 从可识别的 REST 方法检查入参和返回模型的字段名
     *
     * @param methodDetailAST 待检查的方法声明
     */
    @Override
    public void visitToken(DetailAST methodDetailAST) {
        if (!FrameworkMethods.isRestEndpoint(javaSource, methodDetailAST)
                || ApiAnnotations.isHiddenEndpoint(methodDetailAST, javaSource)) {
            return;
        }
        if (apiSourceModels == null) {
            apiSourceModels = new ApiSourceModels(Path.of(getFilePath()).toAbsolutePath().normalize(), rootDetailAST, javaSource);
        }
        var parameters = JavaSource.children(methodDetailAST.findFirstToken(TokenTypes.PARAMETERS));
        for (var parameter : parameters) {
            if (FrameworkMethods.isValueParameter(parameter)
                    && !FrameworkMethods.isInternalParameter(parameter, javaSource)
                    && ApiAnnotations.isVisibleParameter(parameter, javaSource)) {
                checkReferences(parameter.findFirstToken(TokenTypes.TYPE), javaSource, parameter, JavaSource.declarationName(parameter));
            }
        }
        checkReferences(methodDetailAST.findFirstToken(TokenTypes.TYPE), javaSource, methodDetailAST, JavaSource.declarationName(methodDetailAST));
    }

    /**
     * 沿引用的声明、父类和字段类型检查可见的模型属性
     *
     * @param typeDetailAST       类型表达式节点
     * @param declaringJavaSource 引用所在文件的名称索引
     * @param contextDetailAST    引用位置所在的声明
     * @param targetDetailAST     Controller 中的报告位置
     */
    private void checkReferences(DetailAST typeDetailAST, JavaSource declaringJavaSource, DetailAST contextDetailAST, DetailAST targetDetailAST) {
        for (var model : apiSourceModels.referenced(typeDetailAST, declaringJavaSource, contextDetailAST)) {
            if (!visited.add(model.name()) || PersistenceModels.isEntity(model)
                    || ApiAnnotations.hidden(model.tree(), model.source())) {
                continue;
            }
            for (var fieldDetailAST : ApiSourceModels.members(model.tree())) {
                if (JavaSource.isInstanceFieldOrComponent(fieldDetailAST)
                        && !ApiAnnotations.hidden(fieldDetailAST, model.source())) {
                    checkField(model, fieldDetailAST, targetDetailAST);
                }
            }
            checkReferences(model.tree().findFirstToken(TokenTypes.EXTENDS_CLAUSE), model.source(), model.tree(), targetDetailAST);
        }
    }

    /**
     * 报告布尔字段并继续检查字段引用的模型
     *
     * @param model           字段所属源码模型
     * @param fieldDetailAST  待检查的字段或组件
     * @param targetDetailAST Controller 中的报告位置
     */
    private void checkField(ApiSourceModels.Model model, DetailAST fieldDetailAST, DetailAST targetDetailAST) {
        DetailAST typeDetailAST = fieldDetailAST.findFirstToken(TokenTypes.TYPE);
        DetailAST nameDetailAST = JavaSource.declarationName(fieldDetailAST);
        if (typeDetailAST != null && typeDetailAST.findFirstToken(TokenTypes.ARRAY_DECLARATOR) == null
                && nameDetailAST.getText().matches("is[A-Z].*")
                && (typeDetailAST.findFirstToken(TokenTypes.LITERAL_BOOLEAN) != null
                || model.source().isType(JavaSource.typeName(typeDetailAST), "java.lang.Boolean"))) {
            log(targetDetailAST, "boolean.field.name", model.name(), nameDetailAST.getText(), nameDetailAST.getLineNo());
        }
        checkReferences(typeDetailAST, model.source(), fieldDetailAST, targetDetailAST);
    }
}
