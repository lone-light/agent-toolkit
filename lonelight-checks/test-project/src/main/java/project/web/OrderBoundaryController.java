// 检查：ControllerEntityReturnCheck, LayerDependencyCheck

package project.web;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import project.api.OrderView;
import project.entity.Order;
import project.mapper.OrderMapper;

import java.util.List;

/**
 * 对照页面方法和 REST 方法的分层约束
 *
 * @author lonelight
 */
@Controller
@RequestMapping("/orders/boundary")
public class OrderBoundaryController {
    /**
     * 用于验证控制层依赖持久层的提示
     */
    private final OrderMapper orderMapper;

    /**
     * 保存订单映射器
     *
     * @param orderMapper 订单映射器
     */
    public OrderBoundaryController(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    // 页面方法无需接口摘要
    @GetMapping("/page")
    public String page() {
        return "orders";
    }

    // 预期提示 REST 方法直接返回持久化实体
    @GetMapping("/entity")
    @ResponseBody
    @Operation(summary = "读取持久化订单")
    public Order persistedOrder() {
        return new Order(1);
    }

    // 预期提示泛型响应中存在持久化实体
    @GetMapping("/entities")
    @Operation(summary = "读取持久化订单列表")
    public ResponseEntity<List<Order>> persistedOrders() {
        return ResponseEntity.ok(List.of(new Order(1)));
    }

    // 正例返回独立的视图记录
    @GetMapping("/view")
    @Operation(summary = "读取订单视图")
    public ResponseEntity<OrderView> view() {
        long number = orderMapper == null ? 0 : 1;
        return ResponseEntity.ok(new OrderView(number, List.of(), ""));
    }
}
