package lonelight.checks.numeric;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证浮点构造检查能区分数值参数与其他重载
 *
 * @author lonelight
 */
class BigDecimalFloatingConstructorCheckTest extends CustomCheckTest {
    /**
     * 验证不同浮点写法和安全的替代构造
     */
    @Test
    void scan_floatingExpressions_reportsLossyConstructors() throws Exception {
        verify(BigDecimalFloatingConstructorCheck.class, """
                import java.math.*;
                class Example {
                    Double tax = 0.1;
                    void decimals(double amount, int count) {
                        new BigDecimal(0.1);
                        new BigDecimal(-0.1f, MathContext.DECIMAL64);
                        new java.math.BigDecimal((double) count);
                        new BigDecimal(amount);
                        new BigDecimal(this.tax + count);
                        new BigDecimal(1 + 0.1);
                        new BigDecimal(1);
                        new BigDecimal((int) 0.1);
                        new BigDecimal("0." + 1.0);
                        new BigDecimal(new char[] {'1'});
                        BigDecimal.valueOf(amount);
                        new BigDecimal(BigInteger.ONE);
                    }
                }
                """, 6);
    }

    /**
     * 验证遮蔽、包装类型和未知表达式的处理
     */
    @Test
    void scan_shadowedVariables_usesVisibleDeclaration() throws Exception {
        verify(BigDecimalFloatingConstructorCheck.class, """
                import java.math.BigDecimal;
                class Example {
                    private double amount;
                    void decimals(Float factor, int amount, double[] rates) {
                        new BigDecimal(amount);
                        new BigDecimal(this.amount);
                        new BigDecimal(factor);
                        new BigDecimal(rates[0]);
                        new BigDecimal(unknown());
                    }
                    double unknown() { return 0.0; }
                }
                """, 2);
        verify(BigDecimalFloatingConstructorCheck.class, """
                import custom.BigDecimal;
                class Example {
                    void decimals() { new BigDecimal(0.1); }
                }
                """, 0);
    }

    /**
     * 验证记录组件能识别且 lambda 参数和模式变量不会误用外层类型
     */
    @Test
    void scan_lambdaAndPatternBindings_resolvesScopedVariables() throws Exception {
        verify(BigDecimalFloatingConstructorCheck.class, """
                import java.math.BigDecimal;
                import java.util.function.Function;
                record Example(double amount) {
                    BigDecimal fromComponent() { return new BigDecimal(amount); }
                    void formats(Object value) {
                        Function<String, BigDecimal> f = amount -> new BigDecimal(amount);
                        Function<String, BigDecimal> g = (amount) -> new BigDecimal(amount);
                        if (value instanceof String amount) {
                            new BigDecimal(amount);
                        }
                    }
                }
                """, 1);
    }
}
