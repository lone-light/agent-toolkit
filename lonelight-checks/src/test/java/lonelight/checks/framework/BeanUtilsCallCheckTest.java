package lonelight.checks.framework;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证全限定 BeanUtils 调用的诊断范围
 *
 * @author lonelight
 */
class BeanUtilsCallCheckTest extends CustomCheckTest {
    /**
     * 验证全限定 BeanUtils 调用与导入诊断各归一条规则
     */
    @Test
    void qualifiesCallsWithoutDuplicatingImportDiagnostics() throws Exception {
        verify(BeanUtilsCallCheck.class, "class Example { void run() { org.springframework.beans.BeanUtils.copyProperties(a, b); } }", 1);
        verify(BeanUtilsCallCheck.class, "class Example { void run() { org.apache.commons.beanutils.BeanUtils.copyProperties(a, b); } }", 1);
        verify(BeanUtilsCallCheck.class, "import org.springframework.beans.BeanUtils; class Example { void run() { BeanUtils.copyProperties(a, b); } }", 0);
        verify(BeanUtilsCallCheck.class, "class Example { void run() { app.BeanUtils.copyProperties(a, b); } }", 0);
    }
}
