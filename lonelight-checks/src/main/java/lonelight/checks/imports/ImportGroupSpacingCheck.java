package lonelight.checks.imports;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.RuleCheck;

/**
 * 检查 IDEA 默认导入布局的组间空行
 *
 * @author lonelight
 */
public final class ImportGroupSpacingCheck extends RuleCheck {
    /**
     * 标识 IDEA 默认布局的四类导入组
     *
     * @author lonelight
     */
    private enum Group {
        OTHER, JAVAX, JAVA, STATIC
    }

    private Group previousGroup;
    private int previousEnd;

    /**
     * 监听普通导入和静态导入
     *
     * @return 导入节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[] {TokenTypes.IMPORT, TokenTypes.STATIC_IMPORT};
    }

    /**
     * 重置当前文件的导入组位置
     *
     * @param root 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST root) {
        previousGroup = null;
        previousEnd = 0;
    }

    /**
     * 按 IDEA 默认布局检查导入组间空行
     *
     * @param node 当前导入声明
     */
    @Override
    public void visitToken(DetailAST node) {
        Group group = group(node);
        if (previousGroup != null && previousGroup.compareTo(group) < 0) {
            boolean betweenJdkGroups = previousGroup == Group.JAVAX && group == Group.JAVA;
            int linesBetween = node.getLineNo() - previousEnd - 1;
            // 空行检查只适用于组间全为空白的情况
            boolean onlyWhitespace = true;
            for (int line = previousEnd; line < node.getLineNo() - 1; line++) {
                if (!getLine(line).isBlank()) {
                    onlyWhitespace = false;
                    break;
                }
            }
            if (onlyWhitespace && linesBetween != (betweenJdkGroups ? 0 : 1)) {
                log(node, betweenJdkGroups ? "import.spacing.jdk" : "import.spacing.boundary");
            }
        }
        previousGroup = group;
        previousEnd = node.getLastChild().getLineNo();
    }

    /**
     * 确定导入声明所属的组
     *
     * @param node 待归类的导入声明
     * @return 导入分组
     */
    private static Group group(DetailAST node) {
        if (node.getType() == TokenTypes.STATIC_IMPORT) {
            return Group.STATIC;
        }
        String name = FullIdent.createFullIdentBelow(node).getText();
        if (name.startsWith("javax.")) {
            return Group.JAVAX;
        }
        return name.startsWith("java.") ? Group.JAVA : Group.OTHER;
    }
}
