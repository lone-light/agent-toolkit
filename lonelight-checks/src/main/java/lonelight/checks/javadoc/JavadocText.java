package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocCommentsTokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 提取 Javadoc 中可见的说明文字
 *
 * @author lonelight
 */
final class JavadocText {
    /**
     * 限制工具类实例化
     */
    private JavadocText() {
    }

    /**
     * 判断块标签是否包含可见说明
     *
     * @param tagDetailNode Javadoc 块标签
     * @return 包含说明文字或行内标签时为 true
     */
    static boolean hasDescription(DetailNode tagDetailNode) {
        Set<Integer> lines = new HashSet<>();
        collectLines(JavadocUtil.findFirstToken(tagDetailNode, JavadocCommentsTokenTypes.DESCRIPTION), lines);
        return !lines.isEmpty();
    }

    /**
     * 收集正文可见文字所在的源码行
     *
     * @param detailNode Javadoc 节点，可为空
     * @param lines      待更新的行号集合
     */
    static void collectLines(DetailNode detailNode, Set<Integer> lines) {
        if (detailNode == null) {
            return;
        }
        int type = detailNode.getType();
        if (type == JavadocCommentsTokenTypes.JAVADOC_INLINE_TAG) {
            var tag = detailNode.getFirstChild();
            int tagType = tag.getType();
            if (tagType == JavadocCommentsTokenTypes.CODE_INLINE_TAG || tagType == JavadocCommentsTokenTypes.LITERAL_INLINE_TAG
                    || tagType == JavadocCommentsTokenTypes.SUMMARY_INLINE_TAG) {
                collectLines(tag, lines);
            } else {
                lines.add(detailNode.getLineNumber());
            }
            return;
        }
        if (type == JavadocCommentsTokenTypes.TEXT && !detailNode.getText().isBlank()) {
            lines.add(detailNode.getLineNumber());
            return;
        }
        if (type == JavadocCommentsTokenTypes.TEXT || type == JavadocCommentsTokenTypes.HTML_COMMENT
                || type == JavadocCommentsTokenTypes.HTML_TAG_START || type == JavadocCommentsTokenTypes.HTML_TAG_END) {
            return;
        }
        for (DetailNode childDetailNode = detailNode.getFirstChild(); childDetailNode != null;
             childDetailNode = childDetailNode.getNextSibling()) {
            collectLines(childDetailNode, lines);
        }
    }
}
