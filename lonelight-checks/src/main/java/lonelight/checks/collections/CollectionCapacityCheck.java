package lonelight.checks.collections;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import lonelight.checks.JavaSource;
import lonelight.checks.RuleCheck;

import java.util.Set;

/**
 * 提示为支持预设容量的 JDK 集合按预计元素数初始化
 *
 * @author lonelight
 */
public final class CollectionCapacityCheck extends RuleCheck {
    private static final Set<String> TYPES = Set.of("java.util.ArrayList", "java.util.ArrayDeque",
            "java.util.HashMap", "java.util.HashSet", "java.util.LinkedHashMap", "java.util.LinkedHashSet",
            "java.util.PriorityQueue", "java.util.concurrent.ConcurrentHashMap");
    /**
     * 当前文件的名称索引
     */
    private JavaSource javaSource;

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
     * 索引当前文件的类型名称
     *
     * @param rootDetailAST 当前文件的语法树根节点
     */
    @Override
    public void beginTree(DetailAST rootDetailAST) {
        javaSource = new JavaSource(rootDetailAST);
    }

    /**
     * 报告直接创建目标集合时的无参构造调用
     *
     * @param creationDetailAST 当前对象创建节点
     */
    @Override
    public void visitToken(DetailAST creationDetailAST) {
        DetailAST argumentsDetailAST = creationDetailAST.findFirstToken(TokenTypes.ELIST);
        if (argumentsDetailAST == null || argumentsDetailAST.hasChildren()
                || creationDetailAST.findFirstToken(TokenTypes.OBJBLOCK) != null) {
            return;
        }
        String name = JavaSource.typeName(creationDetailAST);
        if (TYPES.stream().anyMatch(type -> javaSource.isKnownType(name, type))) {
            log(creationDetailAST, "collection.capacity", JavaSource.simpleName(name));
        }
    }
}
