package lonelight.checks.naming;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
    void imposesNoSuffixWithoutProjectConfiguration() throws Exception {
        verify(TypeSuffixCheck.class, "package sample.dto; record Example(String name) {}", 0);
    }

    /**
     * 验证后缀映射支持多种风格和明确包边界
     */
    @Test
    void supportsSeveralStylesAndPackageBoundaries() throws Exception {
        String mapping = "sample\\.dto\\..*=DTO|Request|Query";
        verify(TypeSuffixCheck.class, "package sample.dto; class Example {}", 1, "mappings", mapping);
        verify(TypeSuffixCheck.class, "package sample.dto; class UserRequest {}", 0, "mappings", mapping);
        verify(TypeSuffixCheck.class, "package sample.dtox; class Example {}", 0, "mappings", mapping);
        assertThrows(IllegalArgumentException.class, () -> new TypeSuffixCheck().setMappings("invalid"));
    }
}
