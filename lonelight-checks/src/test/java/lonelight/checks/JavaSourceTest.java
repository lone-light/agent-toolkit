package lonelight.checks;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.JavaParser;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 Java 源码中的字面量和变量解析
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
        assertEquals("\n", readStringLiteral(BACKSLASH + "u005c" + BACKSLASH + "u006e"));
        assertEquals(BACKSLASH, readStringLiteral((BACKSLASH + "u005c").repeat(2)));
        assertNull(readStringLiteral(BACKSLASH + "u005c" + BACKSLASH + "u0041"));
    }

    /**
     * 验证非法或截断的 Unicode 转义仍按转义规则处理
     */
    @Test
    void literalText_invalidUnicodeEscapes_respectsBackslashParity() {
        assertNull(readStringLiteral(BACKSLASH + "u00g1"));
        assertNull(readStringLiteral(BACKSLASH + "uu041"));
        assertEquals(BACKSLASH + "u00g1", readStringLiteral(BACKSLASH.repeat(2) + "u00g1"));
    }

    /**
     * 验证括号内的字面量和拼接使用 Java 表达式的值
     *
     * @param directory 临时源码目录
     */
    @Test
    void literalText_parenthesizedConcatenation_preservesValue(@TempDir Path directory) throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
                class Example { String text = (("left") + (("right"))); }
                """);
        var root = JavaParser.parseFile(file.toFile(), JavaParser.Options.WITHOUT_COMMENTS);
        assertEquals("leftright", JavaSource.literalText(JavaSource.descendants(root, TokenTypes.EXPR).getFirst()));
    }

    /**
     * 验证引用位置决定资源、循环变量和显式 this 能访问的声明
     *
     * @param directory 临时源码目录
     */
    @Test
    void find_scopedDeclarations_resolvesOnlyVisibleBindings(@TempDir Path directory) throws Exception {
        Path file = CheckTestSupport.write(directory, "Example.java", """
                class Base { Object value; }
                class Example {
                    String value;
                    void run() {
                        try (java.io.Closeable value = null) {
                            probe(value);
                        } catch (Exception ex) {
                            probe(value);
                        } finally {
                            probe(value);
                        }
                        for (Integer value : values(probe(value))) {
                            probe(value);
                        }
                    }
                    class Inner extends Base {
                        void run() { probe(this.value); }
                    }
                }
                """);
        var root = JavaParser.parseFile(file.toFile(), JavaParser.Options.WITHOUT_COMMENTS);
        var variables = new JavaVariables(root);
        var bindings = JavaSource.descendants(root, TokenTypes.METHOD_CALL).stream()
                .filter(call -> call.getFirstChild().getText().equals("probe"))
                .map(call -> {
                    var argument = call.findFirstToken(TokenTypes.ELIST).getFirstChild().getFirstChild();
                    var declaration = variables.find(argument, "value", argument.getType() == TokenTypes.DOT);
                    return declaration == null ? "" : JavaSource.typeName(declaration.findFirstToken(TokenTypes.TYPE));
                }).toList();
        assertEquals(List.of("java.io.Closeable", "String", "String", "String", "Integer", ""), bindings);
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
