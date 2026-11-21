package base;

import java.util.List;

/**
 * 展示单行枚举、省略迭代表达式和 Javadoc 段落的排版
 *
 * @author lonelight
 */
public class IdeaFormattedCases {
    /**
     * 从列表取出第一个就绪状态
     * <p>
     * 取出后从列表移除该状态
     *
     * @param orderStates 可修改的订单状态列表
     * @return 取出的状态，无就绪状态时返回 WAITING
     */
    public OrderState takeFirstReady(List<OrderState> orderStates) {
        for (var iterator = orderStates.iterator(); iterator.hasNext(); ) {
            OrderState orderState = iterator.next();
            if (orderState == OrderState.READY) {
                iterator.remove();
                return orderState;
            }
        }
        return OrderState.WAITING;
    }

    /**
     * 订单状态
     *
     * @author lonelight
     */
    public enum OrderState {WAITING, READY}
}
