// 检查：ControllerApiDocumentationCheck, ControllerEntityReturnCheck, LayerDependencyCheck
// 模型源码放在相邻 api、entity 包，扫描本文件或整个 project 目录都能看到跨文件检查
package project;

import project.api.OrderInput;
import project.api.OrderSnapshot;
import project.entity.Order;
import project.mapper.OrderMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
class OrderController {
    private OrderMapper orderMapper;

    @PostMapping("/orders/{id}")
    ResponseEntity<List<OrderSnapshot>> update(@PathVariable long id, @RequestBody OrderInput orderInput) {
        return ResponseEntity.ok(List.of(new OrderSnapshot(id, "新建", "", "")));
    }

    @GetMapping("/orders")
    @Operation(summary = "读取订单")
    List<Order> entityList() {
        return List.of();
    }

    @GetMapping("/search/{region}")
    @Operation(summary = " ",
            parameters = @Parameter(name = "q", in = ParameterIn.HEADER, description = "位置错误"))
    String search(@RequestParam("q") String query, @RequestHeader("X-Trace") String trace,
                  @CookieValue("session") String session, @MatrixVariable String region,
                  @PathVariable("region") String path, int page, Mode mode) {
        return query;
    }

    @GetMapping("/documented")
    @Operation(summary = "按订单编号或请求头查找",
            parameters = @Parameter(name = "x-trace", in = ParameterIn.HEADER, description = "追踪编号"))
    long documented(@RequestHeader("X-Trace") String trace,
                    @Parameter(description = "订单编号") @RequestParam long id,
                    @Parameter(hidden = true) @CookieValue String session) {
        return id;
    }

    @PostMapping("/raw")
    @Operation(summary = "接收原始文本")
    void raw(@RequestBody String payload, @RequestPart("attachment") MultipartFile attachment) { }

    @PostMapping("/raw-documented")
    @Operation(summary = "接收说明齐全的原始文本",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "原始订单内容"))
    void rawDocumented(@RequestBody String payload) { }

    @PostMapping("/form")
    @Operation(summary = "从表单读取订单")
    void form(@ModelAttribute OrderInput orderInput) { }

    @Hidden
    @GetMapping("/hidden")
    String hidden(@RequestParam String prompt) {
        return prompt;
    }

    String internalHelper(int count) {
        return Integer.toString(count);
    }

    enum Mode { BASIC, FULL }
}
