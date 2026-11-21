package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.nio.file.Path;

/**
 * 提示调用方使用持久化实体已声明的构建器
 *
 * @author lonelight
 */
public final class EntityBuilderCheck extends RuleCheck {
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
     * 判断实体是否公开标准 builder 方法或指定 Lombok 生成构建器
     *
     * @param model 待检查的持久化实体
     * @return 存在构建入口时为 true
     */
    private static boolean hasBuilder(ApiSourceModels.Model model) {
        var javaSource = model.source();
        if (hasLombokBuilder(model.tree(), javaSource)) {
            return true;
        }
        var body = model.tree().findFirstToken(TokenTypes.OBJBLOCK);
        for (var member : JavaSource.children(body)) {
            if (member.getType() == TokenTypes.CTOR_DEF && hasLombokBuilder(member, javaSource)
                    || member.getType() == TokenTypes.METHOD_DEF && isBuilderMethod(member)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 识别公开的无参静态构建方法
     *
     * @param methodDetailAST 方法声明
     * @return 方法公开标准构建入口时为 true
     */
    private static boolean isBuilderMethod(DetailAST methodDetailAST) {
        var modifiers = methodDetailAST.findFirstToken(TokenTypes.MODIFIERS);
        var parameters = methodDetailAST.findFirstToken(TokenTypes.PARAMETERS);
        return modifiers != null && modifiers.findFirstToken(TokenTypes.LITERAL_PUBLIC) != null
                && modifiers.findFirstToken(TokenTypes.LITERAL_STATIC) != null
                && parameters != null && !parameters.hasChildren()
                && JavaSource.declarationName(methodDetailAST).getText().equals("builder");
    }

    /**
     * 检查 Lombok 构建器入口是否对外生成
     *
     * @param nodeDetailAST 类型或构造器声明
     * @param javaSource    声明所在文件的名称索引
     * @return 声明公开构建入口时为 true
     */
    private static boolean hasLombokBuilder(DetailAST nodeDetailAST, JavaSource javaSource) {
        for (var annotation : JavaSource.annotations(nodeDetailAST)) {
            if (javaSource.isKnownAnnotationType(annotation, "lombok.Builder")
                    || javaSource.isKnownAnnotationType(annotation, "lombok.experimental.SuperBuilder")) {
                var access = JavaSource.attribute(annotation, "access");
                var methodName = JavaSource.attribute(annotation, "builderMethodName");
                var accessValue = access == null ? null : access.getFirstChild();
                var accessName = accessValue != null && accessValue.getType() == TokenTypes.DOT
                        ? accessValue.getLastChild() : accessValue;
                return (access == null || accessName != null && accessName.getText().equals("PUBLIC"))
                        && (methodName == null || "builder".equals(JavaSource.literalText(methodName)));
            }
        }
        return false;
    }

    /**
     * 判断对象创建是否位于实体自己的实现内部
     *
     * @param ownerDetailAST 实体声明节点
     * @param nodeDetailAST  对象创建节点
     * @return 实体是对象创建的外层类型时为 true
     */
    private static boolean encloses(DetailAST ownerDetailAST, DetailAST nodeDetailAST) {
        for (DetailAST outerDetailAST = JavaSource.enclosingType(nodeDetailAST); outerDetailAST != null; outerDetailAST = JavaSource.enclosingType(outerDetailAST)) {
            if (outerDetailAST == ownerDetailAST) {
                return true;
            }
        }
        return false;
    }

    /**
     * 监听对象创建表达式
     *
     * @return 对象创建节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.LITERAL_NEW};
    }

    /**
     * 重置当前源码文件的类型索引
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        this.rootDetailAST = rootDetailAST;
        javaSource = indexSource(rootDetailAST);
        apiSourceModels = null;
    }

    /**
     * 报告调用方直接 new 已公开构建器的实体
     *
     * @param creationDetailAST 待检查的对象创建节点
     */
    @Override
    public void visitToken(DetailAST creationDetailAST) {
        if (creationDetailAST.findFirstToken(TokenTypes.ELIST) == null
                || creationDetailAST.findFirstToken(TokenTypes.OBJBLOCK) != null) {
            return;
        }
        String name = JavaSource.typeName(creationDetailAST);
        if (name.isEmpty()) {
            return;
        }
        if (apiSourceModels == null) {
            apiSourceModels = new ApiSourceModels(Path.of(getFilePath()).toAbsolutePath().normalize(), rootDetailAST, javaSource);
        }
        var model = apiSourceModels.find(name, javaSource, creationDetailAST);
        if (model != null && PersistenceModels.isEntity(model) && hasBuilder(model)
                && !(model.source() == javaSource && encloses(model.tree(), creationDetailAST))) {
            log(creationDetailAST, "entity.builder", model.name());
        }
    }
}
