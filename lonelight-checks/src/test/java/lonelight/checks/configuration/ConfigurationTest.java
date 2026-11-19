package lonelight.checks.configuration;

import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核对两份配置的规则、等级和格式参数
 *
 * @author lonelight
 */
class ConfigurationTest {
    /**
     * 核对每条启用规则独立声明的预期等级
     */
    @Test
    void load_enabledChecks_matchesExpectedSeverities() throws Exception {
        Map<String, SeverityLevel> expected = new LinkedHashMap<>();
        NativeRuleCases.cases().forEach(test -> expected.put(test.key(), test.expectedSeverityLevel()));
        for (String name : Set.of("naming.VariableTypeNameCheck", "naming.TypeSuffixCheck", "naming.TestMethodNameCheck",
                "framework.BooleanFieldNameCheck", "javadoc.MultilineJavadocCheck", "javadoc.MethodJavadocPeriodCheck",
                "javadoc.MethodJavadocNumberingCheck", "javadoc.TypeJavadocCheck", "framework.ConstructorInjectionCheck",
                "framework.RequestValidationCheck", "framework.EntityBuilderCheck",
                "javadoc.FieldJavadocCheck", "framework.ControllerEntityReturnCheck", "framework.LayerDependencyCheck",
                "framework.LoggingPlaceholderCheck", "framework.ControllerApiDocumentationCheck",
                "imports.ImportGroupSpacingCheck", "imports.QualifiedAnnotationCheck",
                "concurrent.ExecutorFactoryCheck", "numeric.BigDecimalFloatingConstructorCheck")) {
            expected.put("lonelight.checks." + name, SeverityLevel.WARNING);
        }
        expected.put("lonelight.checks.framework.AnnotationAttributeCheck", SeverityLevel.ERROR);
        expected.put("lonelight.checks.collections.CollectionCapacityCheck", SeverityLevel.INFO);
        expected.put("lonelight.checks.framework.BeanUtilsCallCheck", SeverityLevel.ERROR);
        expected.put("IllegalImport#reflectionCopyImports", SeverityLevel.ERROR);
        var rules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-base"));
        var conventions = CheckTestSupport.rules(CheckTestSupport.load("lonelight-project"));
        assertTrue(java.util.Collections.disjoint(rules.keySet(), conventions.keySet()));
        rules.putAll(conventions);
        assertEquals(expected.keySet(), rules.keySet());
        for (var entry : expected.entrySet()) {
            assertEquals(entry.getValue().getName(), rules.get(entry.getKey()).configuration().getProperty("severity"), entry.getKey());
        }
    }

    /**
     * 核对通用配置的格式参数和可移植性约定
     */
    @Test
    void load_baseProfile_appliesFormattingDefaults() throws Exception {
        var rules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-base"));
        var imports = rules.get("CustomImportOrder").configuration();
        assertEquals("false", imports.getProperty("separateLineBetweenGroups"));
        assertEquals("4", rules.get("Indentation").configuration().getProperty("lineWrappingIndentation"));
        assertEquals("8", rules.get("Indentation").configuration().getProperty("arrayInitIndent"));
        assertEquals("120", rules.get("LineLength").configuration().getProperty("max"));
        assertFalse(java.util.Arrays.asList(rules.get("JavadocType").configuration().getPropertyNames()).contains("authorFormat"));
    }
}
