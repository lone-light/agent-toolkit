// 检查：BooleanFieldNameCheck, RequestValidationCheck
package project.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对照请求体校验和接口模型的布尔属性写法
 *
 * @author lonelight
 */
@RestController
class OrderValidationController {
    @PostMapping("/orders/check")
    @Operation(summary = "检查订单输入")
    boolean inspect(@RequestBody OrderInput orderInput) {
        return orderInput.isReady();
    }

    @PostMapping("/orders/validate")
    @Operation(summary = "校验订单输入")
    boolean validate(@Valid @RequestBody ValidatedInput validatedInput) {
        return validatedInput.ready();
    }

    /**
     * 展示保留 is 前缀的布尔属性
     *
     * @param title 订单标题
     * @param isReady 是否就绪
     * @author lonelight
     */
    record OrderInput(
            @Schema(description = "订单标题") @NotBlank(message = "订单标题不能为空") String title,
            @Schema(description = "是否就绪") boolean isReady) { }

    /**
     * 展示直接表达状态的布尔属性
     *
     * @param title 订单标题
     * @param ready 是否就绪
     * @author lonelight
     */
    record ValidatedInput(
            @Schema(description = "订单标题") @NotBlank(message = "订单标题不能为空") String title,
            @Schema(description = "是否就绪") boolean ready) { }
}
