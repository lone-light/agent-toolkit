package lonelight.checks;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证字符串字面量的转义解析
 *
 * @author lonelight
 */
class JavaSourceTest {
    private static final String BACKSLASH = "\\";

    /**
     * 验证相邻 Unicode 转义不会吞掉前后的普通字符
     */
    @Test
    void literalText_adjacentUnicodeEscapes_preservesOrdinaryCharacters() {
        assertEquals("xA中z", readStringLiteral("x" + BACKSLASH + "uuu0041" + BACKSLASH + "u4E2D" + "z"));
    }

    /**
     * 验证连续反斜杠的奇偶性决定是否解码
     */
    @Test
    void literalText_consecutiveBackslashes_decodesByParity() {
        assertEquals(BACKSLASH + "u0041", readStringLiteral(BACKSLASH.repeat(2) + "u0041"));
        assertEquals(BACKSLASH + "A", readStringLiteral(BACKSLASH.repeat(3) + "u0041"));
    }

    /**
     * 验证解码产生的反斜杠会影响后续转义
     */
    @Test
    void literalText_decodedBackslash_affectsFollowingEscapes() {
        assertEquals("\n", readStringLiteral(BACKSLASH + "u005cn"));
        assertEquals(BACKSLASH + "u0041", readStringLiteral(BACKSLASH + "u005c" + BACKSLASH + "u0041"));
    }

    /**
     * 验证非法或截断的 Unicode 转义仍按转义规则处理
     */
    @Test
    void literalText_invalidUnicodeEscapes_respectsBackslashParity() {
        assertThrows(IllegalArgumentException.class, () -> readStringLiteral(BACKSLASH + "u00g1"));
        assertThrows(IllegalArgumentException.class, () -> readStringLiteral(BACKSLASH + "uu041"));
        assertEquals(BACKSLASH + "u00g1", readStringLiteral(BACKSLASH.repeat(2) + "u00g1"));
    }

    /**
     * 直接构造字符串节点来测试字面量解码
     *
     * @param raw 不含引号的原始字符串
     * @return 解码后的字符串
     */
    private static String readStringLiteral(String raw) {
        // 直接构造字面量节点，避免词法分析提前拒绝非法转义。
        DetailAstImpl detailAstImpl = new DetailAstImpl();
        detailAstImpl.initialize(TokenTypes.STRING_LITERAL, '"' + raw + '"');
        return JavaSource.literalText(detailAstImpl);
    }
}
