package lonelight.checks.javadoc;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证实例字段和 record 组件的说明检查
 *
 * @author lonelight
 */
class FieldJavadocCheckTest extends CustomCheckTest {
    /**
     * 验证字段说明与隐藏标记的合规边界
     */
    @Test
    void scan_documentedAndHiddenFields_acceptsExemptFields() throws Exception {
        verify(FieldJavadocCheck.class, """
                import com.fasterxml.jackson.annotation.JsonIgnore;
                import io.swagger.v3.oas.annotations.media.Schema;

                class Example {
                    @JsonIgnore
                    @Schema(hidden = true)
                    String secret;

                    @Schema(description = "名称")
                    String name;

                    @Schema(description = " ")
                    String empty;

                    String missing;
                }
                """, 2);
        verify(FieldJavadocCheck.class, "class Example { /** 名称 */ String name; static int count; void run() { int local; } }", 0);
        verify(FieldJavadocCheck.class, "record Example(String name) {}", 1);
        verify(FieldJavadocCheck.class, "/**\n * 示例\n *\n * @param name 名称\n */\nrecord Example(String name) {}", 0);
        verify(FieldJavadocCheck.class, "class Example { /** */ String name; }", 1);
        verify(FieldJavadocCheck.class, "class Example { @com.fasterxml.jackson.annotation.JsonIgnore(false) String name; }", 1);
    }

    /**
     * 验证多组通配导入下的标准注解与同名成员注解
     */
    @Test
    void scan_wildcardImports_recognizesOnlyKnownAnnotations() throws Exception {
        verify(FieldJavadocCheck.class, """
                import com.fasterxml.jackson.annotation.*;
                import io.swagger.v3.oas.annotations.media.*;

                class Example {
                    @JsonIgnore String secret;
                    @Schema(description = "名称") String name;
                }
                """, 0);
        verify(FieldJavadocCheck.class, """
                import com.fasterxml.jackson.annotation.*;
                import io.swagger.v3.oas.annotations.media.*;

                class Example {
                    @interface Schema { String description(); }
                    @Schema(description = "同名注解") String name;
                }
                """, 1);
    }
}
