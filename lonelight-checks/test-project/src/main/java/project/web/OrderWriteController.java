// 检查：ControllerApiDocumentationCheck

package project.web;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project.api.OrderInput;
import project.api.OrderNote;

/**
 * 对照创建订单、表单和原始请求体的说明
 *
 * @author lonelight
 */
@RestController
@RequestMapping("/orders")
public class OrderWriteController {
    // 正例中的只读、隐藏字段无需提供请求侧说明
    @PostMapping
    @Operation(summary = "创建订单")
    public String create(@Valid @RequestBody OrderInput orderInput) {
        return orderInput.number();
    }

    // 预期在此方法的请求参数处提示 OrderNote 的 note 缺少说明
    @PostMapping("/notes")
    @Operation(summary = "追加订单备注")
    public String leaveNote(@RequestBody OrderNote orderNote) {
        return orderNote.note();
    }

    // 预期提示表单字段 title 缺少说明
    @PostMapping("/form")
    @Operation(summary = "从表单创建订单")
    public String submitForm(@ModelAttribute OrderForm orderForm) {
        return orderForm.title();
    }

    // 预期提示没有本地模型的字符串请求体缺少说明
    @PostMapping("/raw")
    @Operation(summary = "接收原始订单内容")
    public String raw(@RequestBody String payload) {
        return payload;
    }

    // 正例在操作注解中填写原始请求体说明
    @PostMapping("/raw-documented")
    @Operation(summary = "接收说明完整的原始订单内容",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "订单原始内容"))
    public String rawDocumented(@RequestBody String payload) {
        return payload;
    }

    /**
     * 保存表单中的订单标题
     *
     * @param title 订单标题
     * @author lonelight
     */
    public record OrderForm(String title) {
    }
}
