package lonelight.checks.naming;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证类型后缀配置及其作用范围
 *
 * @author lonelight
 */
class TypeSuffixCheckTest extends CustomCheckTest {
    /**
     * 验证未配置后缀映射时不限制类型名
     */
    @Test
    void scan_unconfiguredSuffixes_allowsAnyName() throws Exception {
        verify(TypeSuffixCheck.class, "package sample.dto; record Example(String name) {}", 0);
    }

    /**
     * 验证后缀映射支持多种风格和明确包边界
     */
    @Test
    void scan_configuredSuffixes_acceptsStylesInMatchedPackage() throws Exception {
        String mapping = "sample\\.dto\\..*=DTO|Request|Query";
        verify(TypeSuffixCheck.class, "package sample.dto; class Example {}", 1, "mappings", mapping);
        verify(TypeSuffixCheck.class, "package sample.dto; class UserRequest {}", 0, "mappings", mapping);
        verify(TypeSuffixCheck.class, "package sample.dtox; class Example {}", 0, "mappings", mapping);
        assertThrows(IllegalArgumentException.class, () -> new TypeSuffixCheck().setMappings("invalid"));
    }

    /**
     * 验证成员类型的限定名、同名导入及局部类型的检查范围
     */
    @Test
    void scan_nestedDeclarations_matchesQualifiedDeclarationNames() throws Exception {
        String source = """
                package sample.dto;

                import java.util.List;

                class Order {
                    static class List {}

                    void prepare() {
                        class Local {}
                    }
                }

                @interface Archive {
                    class List {}
                }
                """;
        verify(TypeSuffixCheck.class, source, 1, "mappings", "sample\\.dto\\.Order\\.List=DTO");
        verify(TypeSuffixCheck.class, source, 1, "mappings", "sample\\.dto\\.Archive\\.List=View");
        verify(TypeSuffixCheck.class, source, 0, "mappings", "java\\.util\\.List=DTO");
        verify(TypeSuffixCheck.class, source, 3, "mappings", "sample\\.dto\\..*=DTO");
    }

    /**
     * 验证长后缀列表可加载并拒绝空项和非法字符
     */
    @Test
    void configure_longAndMalformedSuffixLists_rejectsOnlyInvalidEntries() {
        var check = new TypeSuffixCheck();
        check.setMappings("sample\\.dto\\..*=" + "DTO|".repeat(10_000) + "Request");
        for (String suffixes : new String[]{"", "|DTO", "DTO||Request", "DTO|", "DTO| Re", "DTO|Re_quest"}) {
            assertThrows(IllegalArgumentException.class, () -> check.setMappings("sample\\.dto\\..*=" + suffixes));
        }
    }
}
