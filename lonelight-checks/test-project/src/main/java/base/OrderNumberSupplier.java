package base;

import java.util.function.Supplier;

/**
 * 为模块服务声明提供订单编号样例
 *
 * @author lonelight
 */
public final class OrderNumberSupplier implements Supplier<String> {

    /**
     * 返回固定的样例订单编号
     *
     * @return 样例订单编号
     */
    @Override
    public String get() {
        return "ORDER-001";
    }
}
