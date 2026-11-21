package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.JavadocDetailNodeParser;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocCommentsTokenTypes;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * 提取方法和构造器的 Javadoc 正文并保留行号
 *
 * @author lonelight
 */
final class MethodDocumentation {
    /**
     * 限制工具类实例化
     */
    private MethodDocumentation() {
    }

    /**
     * 监听方法和构造器声明
     *
     * @return 用于读取方法 Javadoc 的节点类别
     */
    static int[] tokens() {
        return new int[]{TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF, TokenTypes.COMPACT_CTOR_DEF};
    }

    /**
     * 提取方法正文并按原始行号逐行检查
     *
     * @param nodeDetailAST  待检查的方法或构造器节点
     * @param lineBiConsumer 接收源码行号和正文的检查操作
     */
    static void forEachProseLine(DetailAST nodeDetailAST, BiConsumer<Integer, String> lineBiConsumer) {
        var block = JavadocUtil.getAttachedJavadocComment(nodeDetailAST);
        if (block == null) {
            return;
        }
        var tree = new JavadocDetailNodeParser().parseJavadocComment(block).getTree();
        if (tree == null) {
            return;
        }
        int firstLine = block.getLineNo();
        StringBuilder[] prose = new StringBuilder[block.getLastChild().getLineNo() - firstLine + 1];
        Arrays.setAll(prose, ignored -> new StringBuilder());
        appendProse(tree, prose, firstLine);
        for (int index = 0; index < prose.length; index++) {
            lineBiConsumer.accept(firstLine + index, prose[index].toString().strip());
        }
    }

    /**
     * 将结构化注释中的可见内容写回对应源码行
     *
     * @param nodeDetailNode 待处理的 Javadoc 节点
     * @param prose          按源码行保存的正文
     * @param firstLine      注释起始行
     */
    private static void appendProse(DetailNode nodeDetailNode, StringBuilder[] prose, int firstLine) {
        int type = nodeDetailNode.getType();
        if (type == JavadocCommentsTokenTypes.LEADING_ASTERISK || type == JavadocCommentsTokenTypes.NEWLINE
                || type == JavadocCommentsTokenTypes.HTML_TAG_START || type == JavadocCommentsTokenTypes.HTML_TAG_END) {
            return;
        }
        if (isNonProse(nodeDetailNode)) {
            // 用空格分隔片段两侧的正文
            prose[nodeDetailNode.getLineNumber() - firstLine].append(' ');
            return;
        }
        var child = nodeDetailNode.getFirstChild();
        if (child == null) {
            if (type != JavadocCommentsTokenTypes.JAVADOC_CONTENT && type != JavadocCommentsTokenTypes.HTML_CONTENT) {
                prose[nodeDetailNode.getLineNumber() - firstLine].append(nodeDetailNode.getText());
            }
            return;
        }
        while (child != null) {
            appendProse(child, prose, firstLine);
            child = child.getNextSibling();
        }
    }

    /**
     * 识别代码、字面量和 HTML 注释片段
     *
     * @param nodeDetailNode 待判断的 Javadoc 节点
     * @return 片段无需检查时为 true
     */
    private static boolean isNonProse(DetailNode nodeDetailNode) {
        return switch (nodeDetailNode.getType()) {
            case JavadocCommentsTokenTypes.CODE_INLINE_TAG, JavadocCommentsTokenTypes.LITERAL_INLINE_TAG,
                 JavadocCommentsTokenTypes.SNIPPET_INLINE_TAG, JavadocCommentsTokenTypes.HTML_COMMENT -> true;
            case JavadocCommentsTokenTypes.HTML_ELEMENT -> isHtmlCode(nodeDetailNode);
            default -> false;
        };
    }

    /**
     * 识别闭合的 pre 或 code 元素
     *
     * @param elementDetailNode HTML 元素节点
     * @return 元素表示代码片段时为 true
     */
    private static boolean isHtmlCode(DetailNode elementDetailNode) {
        var closing = JavadocUtil.findFirstToken(elementDetailNode, JavadocCommentsTokenTypes.HTML_TAG_END);
        if (closing == null) {
            return false;
        }
        var tagName = JavadocUtil.findFirstToken(closing, JavadocCommentsTokenTypes.TAG_NAME);
        return tagName != null && ("pre".equalsIgnoreCase(tagName.getText()) || "code".equalsIgnoreCase(tagName.getText()));
    }
}
