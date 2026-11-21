package lonelight.checks.whitespace;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean;
import com.puppycrawl.tools.checkstyle.TreeWalkerAuditEvent;
import com.puppycrawl.tools.checkstyle.TreeWalkerFilter;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.blocks.LeftCurlyCheck;
import com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAroundCheck;
import lonelight.checks.JavaSource;

/**
 * 允许只含常量的枚举体采用 IDEA 的单行格式
 *
 * @author lonelight
 */
public final class CompactEnumSpacingFilter extends AbstractAutomaticBean implements TreeWalkerFilter {
    /**
     * 完成无需附加配置的过滤器初始化
     */
    @Override
    protected void finishLocalSetup() {
        // 枚举位置从当前文件的语法树读取
    }

    /**
     * 保留单行枚举花括号内侧之外的空格和换行诊断
     *
     * @param event 待过滤的诊断
     * @return 应保留诊断时为 true
     */
    @Override
    public boolean accept(TreeWalkerAuditEvent event) {
        if (!isInnerBraceViolation(event)) {
            return true;
        }
        return JavaSource.descendants(event.rootAst(), TokenTypes.ENUM_DEF).stream()
                .noneMatch(enumeration -> isCompactBrace(enumeration, event));
    }

    /**
     * 匹配花括号内侧的空格或换行诊断
     *
     * @param event 待判断的诊断
     * @return 诊断发生在花括号内侧时为 true
     */
    private static boolean isInnerBraceViolation(TreeWalkerAuditEvent event) {
        int token = event.getTokenType();
        String source = event.getSourceName();
        String key = event.violation().getKey();
        if (LeftCurlyCheck.class.getName().equals(source)) {
            return LeftCurlyCheck.MSG_KEY_LINE_BREAK_AFTER.equals(key);
        }
        return WhitespaceAroundCheck.class.getName().equals(source)
                && (token == TokenTypes.LCURLY && WhitespaceAroundCheck.MSG_WS_NOT_FOLLOWED.equals(key)
                || token == TokenTypes.RCURLY && WhitespaceAroundCheck.MSG_WS_NOT_PRECEDED.equals(key));
    }

    /**
     * 匹配同一行内只声明常量的枚举体括号
     *
     * @param enumeration 枚举类型声明
     * @param event       待匹配的括号诊断
     * @return 诊断位于单行枚举体边界时为 true
     */
    private static boolean isCompactBrace(DetailAST enumeration, TreeWalkerAuditEvent event) {
        var body = enumeration.findFirstToken(TokenTypes.OBJBLOCK);
        if (body == null || body.findFirstToken(TokenTypes.ENUM_CONSTANT_DEF) == null
                || body.findFirstToken(TokenTypes.SEMI) != null) {
            return false;
        }
        var open = body.findFirstToken(TokenTypes.LCURLY);
        var close = body.findFirstToken(TokenTypes.RCURLY);
        if (open == null || close == null || open.getLineNo() != close.getLineNo()) {
            return false;
        }
        var brace = event.getTokenType() == TokenTypes.RCURLY ? close : open;
        return brace.getLineNo() == event.getLine()
                && brace.getColumnNo() == event.getColumnCharIndex();
    }
}
