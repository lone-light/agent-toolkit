// 检查：TestMethodNameCheck

package project;

import org.junit.Test;

/**
 * 对照 JUnit 4 测试方法的两种命名
 *
 * @author lonelight
 */
public class LegacyJUnitNamingTest {
    @Test
    public void legacySubtotal() { }

    @Test
    public void subtotal_validOrder_returnsAmount() { }
}
