package lonelight.checks.javadoc;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.JavadocUtil;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * 提取方法和构造器的 Javadoc 正文并保留行号
 *
 * @author lonelight
 */
final class MethodDocumentation {
    private static final Pattern NON_PROSE = Pattern.compile(
            "(?is)<(pre|code)\\b[^>]*>.*?</\\1\\s*>|<!--.*?-->|\\{@(?:code|literal)\\s+(?:[^{}]|\\{[^{}]*})*}");

    /**
     * 限制工具类实例化
     */
    private MethodDocumentation() { }

    /**
     * 监听方法和构造器声明
     *
     * @return 用于读取方法 Javadoc 的节点类别
     */
    static int[] tokens() {
        return new int[] {TokenTypes.METHOD_DEF, TokenTypes.CTOR_DEF, TokenTypes.COMPACT_CTOR_DEF};
    }

    /**
     * 提取方法正文并按原始行号逐行检查
     *
     * @param node 待检查的方法或构造器节点
     * @param lineBiConsumer 接收源码行号和正文的检查操作
     */
    static void forEachProseLine(DetailAST node, BiConsumer<Integer, String> lineBiConsumer) {
        DetailAST block = JavadocUtil.getAttachedJavadocComment(node);
        if (block == null) {
            return;
        }
        String text = JavadocUtil.getJavadocCommentContent(block);
        var matcher = NON_PROSE.matcher(text);
        StringBuilder prose = new StringBuilder(text);
        while (matcher.find()) {
            for (int index = matcher.start(); index < matcher.end(); index++) {
                if (prose.charAt(index) != '\n') {
                    prose.setCharAt(index, ' ');
                }
            }
        }
        String[] lines = prose.toString().split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].replaceFirst("^\\s*(?:/\\*\\*|\\*)?\\s?", "")
                    .replaceFirst("\\*/\\s*$", "").strip();
            lineBiConsumer.accept(block.getLineNo() + index, line);
        }
    }
}
