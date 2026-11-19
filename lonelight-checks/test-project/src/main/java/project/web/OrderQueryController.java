// 检查：ControllerApiDocumentationCheck
// 每条查询独立验证摘要或参数说明，方法上有注释标明预期

package project.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对照订单查询方法和公开参数的说明
 *
 * @author lonelight
 */
@RestController
@RequestMapping("/orders/query")
public class OrderQueryController {
    // 预期提示缺少操作摘要
    @GetMapping("/recent")
    public long recentCount() {
        return 1;
    }

    // 预期提示操作摘要为空白
    @GetMapping("/pending")
    @Operation(summary = " ")
    public long pendingCount() {
        return 0;
    }

    // 预期提示路径参数缺少说明
    @GetMapping("/{id}")
    @Operation(summary = "按编号查询订单")
    public long byId(@PathVariable long id) {
        return id;
    }

    // 预期提示方法上的说明与实际参数位置不同
    @GetMapping("/search")
    @Operation(summary = "按关键词搜索",
            parameters = @Parameter(name = "q", in = ParameterIn.HEADER, description = "关键词"))
    public String search(@RequestParam("q") String query) {
        return query;
    }

    // 预期提示说明声明的是 Java 参数名，绑定名称实际是 number
    @GetMapping("/by-number")
    @Operation(summary = "按业务编号查找",
            parameters = @Parameter(name = "orderNumber", in = ParameterIn.QUERY, description = "订单号"))
    public String byNumber(@RequestParam("number") String orderNumber) {
        return orderNumber;
    }

    // 预期提示未显式注解的简单请求参数缺少说明
    @GetMapping("/page")
    @Operation(summary = "读取订单页码")
    public int page(int page) {
        return page;
    }

    // 正例使用正确的绑定名称、位置及说明引用
    @GetMapping("/by-status")
    @Operation(summary = "按状态查询订单",
            parameters = @Parameter(name = "status", in = ParameterIn.QUERY,
                    ref = "#/components/parameters/orderStatus"))
    public String byStatus(@RequestParam("status") String orderStatus) {
        return orderStatus;
    }

    // 正例在矩阵参数上直接添加说明
    @GetMapping("/region/{region}")
    @Operation(summary = "按地区筛选订单")
    public String byRegion(
            @Parameter(description = "地区") @PathVariable("region") String region,
            @Parameter(description = "渠道") @MatrixVariable("channel") String channel) {
        return region + channel;
    }
}
