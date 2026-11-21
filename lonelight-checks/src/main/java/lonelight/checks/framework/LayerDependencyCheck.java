package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 检查 Controller 和 Service 声明中的禁止依赖
 *
 * @author lonelight
 */
public final class LayerDependencyCheck extends RuleCheck {
    private static final Set<Integer> DECLARATIONS = Set.of(TokenTypes.METHOD_DEF, TokenTypes.VARIABLE_DEF,
            TokenTypes.PARAMETER_DEF, TokenTypes.RECORD_COMPONENT_DEF);

    /**
     * 控制器中受限类型的全限定名模式
     */
    private Pattern controllerForbidden = Pattern.compile(".*\\.mapper\\..*");

    /**
     * 服务中受限类型的全限定名模式
     */
    private Pattern serviceForbidden = Pattern.compile("(?:jakarta|javax)\\.servlet\\..*|org\\.springframework\\.http\\.ResponseEntity");

    /**
     * 当前文件的名称索引
     */
    private JavaSource source;

    /**
     * 供 Checkstyle 从 XML 配置 Controller 的禁止依赖
     *
     * @param value 匹配完整类型名的正则表达式
     */
    @SuppressWarnings("unused")
    public void setControllerForbidden(String value) {
        controllerForbidden = Pattern.compile(value);
    }

    /**
     * 供 Checkstyle 从 XML 配置 Service 的禁止依赖
     *
     * @param value 匹配完整类型名的正则表达式
     */
    @SuppressWarnings("unused")
    public void setServiceForbidden(String value) {
        serviceForbidden = Pattern.compile(value);
    }

    /**
     * 监听源码中的类型节点
     *
     * @return 类型节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.TYPE};
    }

    /**
     * 为当前文件准备依赖类型解析
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        source = new JavaSource(root);
    }

    /**
     * 报告 Controller 或 Service 声明中的禁止依赖
     *
     * @param node 待检查的类型节点
     */
    @Override
    public void visitToken(DetailAST node) {
        if (!DECLARATIONS.contains(node.getParent().getType())) {
            return;
        }
        DetailAST owner = JavaSource.enclosingType(node);
        if (owner == null) {
            return;
        }
        Pattern forbidden = null;
        if (source.hasKnownAnnotation(owner, FrameworkMethods.WEB + "RestController")
                || source.hasKnownAnnotation(owner, "org.springframework.stereotype.Controller")) {
            forbidden = controllerForbidden;
        } else if (source.hasKnownAnnotation(owner, "org.springframework.stereotype.Service")) {
            forbidden = serviceForbidden;
        }
        if (forbidden != null) {
            var match = source.firstMatchingType(node, forbidden);
            if (match != null) {
                log(match.token(), "layer.dependency", match.canonicalName());
            }
        }
    }
}
