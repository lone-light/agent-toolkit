package lonelight.checks;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按源码作用域定位字段、局部变量和参数声明
 *
 * @author lonelight
 */
public final class JavaVariables {
    /**
     * 按变量短名索引当前文件中的声明
     */
    private final Map<String, List<DetailAST>> declarations = new HashMap<>();

    /**
     * 从当前语法树建立变量索引
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    public JavaVariables(DetailAST rootDetailAST) {
        for (var nodeDetailAST = rootDetailAST; nodeDetailAST != null; nodeDetailAST = nodeDetailAST.getNextSibling()) {
            index(nodeDetailAST);
        }
    }

    /**
     * 收集能声明变量名或遮蔽同名变量的节点
     *
     * @param nodeDetailAST 当前语法树节点
     */
    private void index(DetailAST nodeDetailAST) {
        var nameDetailAST = switch (nodeDetailAST.getType()) {
            case TokenTypes.VARIABLE_DEF, TokenTypes.PARAMETER_DEF, TokenTypes.RECORD_COMPONENT_DEF,
                 TokenTypes.PATTERN_VARIABLE_DEF, TokenTypes.LAMBDA -> nodeDetailAST.findFirstToken(TokenTypes.IDENT);
            default -> null;
        };
        if (nameDetailAST != null) {
            declarations.computeIfAbsent(nameDetailAST.getText(), name -> new ArrayList<>(1)).add(nodeDetailAST);
        }
        for (var childDetailAST = nodeDetailAST.getFirstChild(); childDetailAST != null;
             childDetailAST = childDetailAST.getNextSibling()) {
            index(childDetailAST);
        }
    }

    /**
     * 按作用域深度定位接收者的声明
     *
     * @param usageDetailAST 引用节点
     * @param name           接收者名称
     * @param explicitThis   是否只查找显式访问的实例字段
     * @return 可见的匹配声明，找不到时为 null
     */
    public DetailAST find(DetailAST usageDetailAST, String name, boolean explicitThis) {
        DetailAST selectedDetailAST = null;
        int selectedDepth = -1;
        for (var declarationDetailAST : declarations.getOrDefault(name, List.of())) {
            int container = declarationDetailAST.getParent().getType();
            boolean field = container == TokenTypes.OBJBLOCK || container == TokenTypes.RECORD_COMPONENTS;
            if (explicitThis && !field) {
                continue;
            }
            int depth = enclosingDepth(usageDetailAST, scope(declarationDetailAST));
            boolean beforeUse = declarationDetailAST.getLineNo() < usageDetailAST.getLineNo()
                    || declarationDetailAST.getLineNo() == usageDetailAST.getLineNo() && declarationDetailAST.getColumnNo() < usageDetailAST.getColumnNo();
            if (depth > selectedDepth && (field || beforeUse)) {
                selectedDetailAST = declarationDetailAST;
                selectedDepth = depth;
            }
        }
        return selectedDetailAST;
    }

    /**
     * 取得普通声明的词法范围和模式变量可能产生遮蔽的代码块
     *
     * @param declarationDetailAST 变量声明节点
     * @return 用于确认名称能否解析的语法节点
     */
    private static DetailAST scope(DetailAST declarationDetailAST) {
        var parentDetailAST = declarationDetailAST.getParent();
        if (parentDetailAST == null) {
            return null;
        }
        if (declarationDetailAST.getType() == TokenTypes.LAMBDA) {
            return declarationDetailAST;
        }
        if (declarationDetailAST.getType() == TokenTypes.PATTERN_VARIABLE_DEF) {
            for (var scopeDetailAST = parentDetailAST; scopeDetailAST != null;
                 scopeDetailAST = scopeDetailAST.getParent()) {
                if (scopeDetailAST.getType() == TokenTypes.SLIST) {
                    return scopeDetailAST;
                }
            }
        }
        return switch (parentDetailAST.getType()) {
            case TokenTypes.OBJBLOCK, TokenTypes.RECORD_COMPONENTS, TokenTypes.PARAMETERS,
                 TokenTypes.FOR_EACH_CLAUSE, TokenTypes.FOR_INIT -> parentDetailAST.getParent();
            default -> parentDetailAST;
        };
    }

    /**
     * 计算引用所在作用域在语法树中的深度
     *
     * @param usageDetailAST 引用节点
     * @param scopeDetailAST 候选作用域节点
     * @return 作用域深度，引用不在其中时为 -1
     */
    private static int enclosingDepth(DetailAST usageDetailAST, DetailAST scopeDetailAST) {
        for (DetailAST parentDetailAST = usageDetailAST; parentDetailAST != null; parentDetailAST = parentDetailAST.getParent()) {
            if (parentDetailAST == scopeDetailAST) {
                int depth = 0;
                for (DetailAST ownerDetailAST = scopeDetailAST; ownerDetailAST != null; ownerDetailAST = ownerDetailAST.getParent()) {
                    depth++;
                }
                return depth;
            }
        }
        return -1;
    }
}
