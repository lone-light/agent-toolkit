package lonelight.checks.imports;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.RuleCheck;

/**
 * 检查 IDEA 默认导入分组的空行布局
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

    /**
     * 上一条导入所属的分组
     */
    private Group previousGroup;

    /**
     * 上一条导入结束的位置
     */
    private int previousEnd;

    /**
     * 监听普通导入和静态导入
     *
     * @return 导入节点类别
     */
    @Override
    public int[] getDefaultTokens() {
        return new int[]{TokenTypes.IMPORT, TokenTypes.STATIC_IMPORT};
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
     * 按 IDEA 默认布局检查相邻导入组之间的空行
     *
     * @param node 当前导入声明
     */
    @Override
    public void visitToken(DetailAST node) {
        Group group = group(node);
        if (previousGroup != null && previousGroup.compareTo(group) < 0) {
            boolean betweenJdkGroups = previousGroup == Group.JAVAX && group == Group.JAVA;
            int linesBetween = node.getLineNo() - previousEnd - 1;
            if (linesBetween != (betweenJdkGroups ? 0 : 1) && hasOnlyBlankLinesBefore(node.getLineNo())) {
                log(node, betweenJdkGroups ? "import.spacing.jdk" : "import.spacing.boundary");
            }
        }
        previousGroup = group;
        previousEnd = node.getLastChild().getLineNo();
    }

    /**
     * 判断上一条导入到当前导入之间是否只有空行
     *
     * @param importLine 当前导入的行号
     * @return 组边界没有夹杂注释或代码时为 true
     */
    private boolean hasOnlyBlankLinesBefore(int importLine) {
        for (int line = previousEnd; line < importLine - 1; line++) {
            if (!getLine(line).isBlank()) {
                return false;
            }
        }
        return true;
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
