// 两份配置都能通过的完整示例

package base;

/**
 * 保存对外展示的订单摘要
 *
 * @param orderNumber 订单编号
 * @param status 当前状态
 * @author lonelight
 */
public record OrderSummary(String orderNumber, String status) {
}
