package lonelight.checks.framework;

import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证注解属性缺失和空白值的检查
 *
 * @author lonelight
 */
class AnnotationAttributeCheckTest extends CustomCheckTest {
    /**
     * 验证属性缺失、空白值和注解身份的判定
     */
    @Test
    void checksPresenceBlankEscapesConstantsAndCanonicalIdentity() throws Exception {
        String[] options = {"attributes", "jakarta.validation.constraints.NotNull=message,app.Log=value"};
        verify(AnnotationAttributeCheck.class, "class Example { @jakarta.validation.constraints.NotNull String value; }", 1, options);
        verify(AnnotationAttributeCheck.class, "import jakarta.validation.constraints.*; class Example { @NotNull(message = \" \\t\") String value; }", 1, options);
        verify(AnnotationAttributeCheck.class, "import jakarta.validation.constraints.NotNull; class Example { @NotNull(message = Messages.REQUIRED) String value; }", 0, options);
        verify(AnnotationAttributeCheck.class, "import other.NotNull; class Example { @NotNull String value; }", 0, options);
        verify(AnnotationAttributeCheck.class, "import app.Log; @Log(\"操作记录\") class Example {}", 0, options);
        verify(AnnotationAttributeCheck.class, "import app.Log; @Log(\"\") class Example {}", 1, options);
        verify(AnnotationAttributeCheck.class, "@io.swagger.v3.oas.annotations.media.Schema(hidden = true) class Example {}", 0, options);
        assertThrows(IllegalArgumentException.class, () -> new AnnotationAttributeCheck().setAttributes("invalid"));
    }

    /**
     * 验证文本块、拼接和 Unicode 转义中的空白值
     */
    @Test
    void recognizesBlankConstantConcatenationTextBlocksAndUnicodeEscapes() throws Exception {
        String[] options = {"attributes", "app.Log=value"};
        verify(AnnotationAttributeCheck.class, "@app.Log(\" \" + \"\\t\") class Example {}", 1, options);
        verify(AnnotationAttributeCheck.class, "@app.Log(\"\"\"\n    \n    \"\"\") class Example {}", 1, options);
        verify(AnnotationAttributeCheck.class, "@app.Log(\"\\u0020\") class Example {}", 1, options);
        verify(AnnotationAttributeCheck.class, "@app.Log(\"\\\\u0020\") class Example {}", 0, options);
    }

    /**
     * 验证嵌套注解的属性不能替代外层属性
     */
    @Test
    void nestedAttributesCannotSatisfyTheOuterAnnotation() throws Exception {
        verify(AnnotationAttributeCheck.class,
                "@app.Log(meta = @app.Meta(message = \"内部描述\")) class Example {}",
                1, "attributes", "app.Log=message");
        verify(AnnotationAttributeCheck.class,
                "@app.Log(meta = @app.Meta(message = \"\"), message = \"外部描述\") class Example {}",
                0, "attributes", "app.Log=message");
    }

    /**
     * 验证每项配置属性只检查一次
     */
    @Test
    void checksEachConfiguredAttributeOnce() throws Exception {
        verify(AnnotationAttributeCheck.class, "@app.Log class Example {}", 2,
                "attributes", "app.Log=value,app.Log=message,app.Log=value");
    }

    /**
     * 核对主题样例能触发配置中的每种校验注解及四种空白值
     */
    @Test
    void messages_groupedExample_coversConfiguredAnnotations() throws Exception {
        String ruleName = AnnotationAttributeCheck.class.getName();
        var profile = CheckTestSupport.load("lonelight-project");
        var rule = CheckTestSupport.rules(profile).get(ruleName);
        var attributes = rule.configuration().getProperty("attributes").split(",");
        var files = FixtureCatalog.load("project").filesFor(ruleName);
        assertEquals(1, files.size());
        var events = CheckTestSupport.scan(profile, files, getClass().getClassLoader()).stream()
                .filter(event -> CheckTestSupport.matches(event, ruleName)).toList();
        assertEquals(attributes.length + 4, events.size());
        for (String attribute : attributes) {
            String annotationName = attribute.strip().split("=", 2)[0];
            assertTrue(events.stream().anyMatch(event -> event.getMessage().contains(" " + annotationName + " ")),
                    annotationName);
        }
    }
}
