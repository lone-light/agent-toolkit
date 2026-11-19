// 检查：ControllerApiDocumentationCheck

package project.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import project.api.OrderSnapshot;
import project.api.OrderView;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 对照泛型响应、上传、下载和流式响应的说明
 *
 * @author lonelight
 */
@RestController
@RequestMapping("/orders/files")
public class OrderFilesController {
    // 预期在方法处提示泛型内部的 OrderSnapshot 缺少 orderId 的说明
    @GetMapping("/snapshots")
    @Operation(summary = "列出订单快照")
    public ResponseEntity<List<OrderSnapshot>> snapshots() {
        return ResponseEntity.ok(List.of(new OrderSnapshot(1, "已确认", "")));
    }

    // 正例对照同一个模型字段在同一文件内只报一次
    @GetMapping("/latest-snapshot")
    @Operation(summary = "读取最近的订单快照")
    public OrderSnapshot latest() {
        return new OrderSnapshot(1, "已确认", "");
    }

    // 正例使用数组字段说明并隐藏只写字段
    @GetMapping("/views")
    @Operation(summary = "列出已说明的订单视图")
    public ResponseEntity<List<OrderView>> views() {
        return ResponseEntity.ok(List.of(new OrderView(1, List.of("新订单"), "")));
    }

    // 预期提示上传分段缺少参数说明
    @PostMapping("/attachment")
    @Operation(summary = "上传订单凭据")
    public String attach(@RequestPart("attachment") MultipartFile multipartFile) {
        return multipartFile.getName();
    }

    // 正例直接说明上传分段
    @PostMapping("/documented-attachment")
    @Operation(summary = "上传说明完整的订单凭据")
    public String attachDocumented(
            @Parameter(description = "订单凭据") @RequestPart("attachment") MultipartFile multipartFile) {
        return multipartFile.getName();
    }

    @GetMapping("/export")
    @Operation(summary = "下载订单清单")
    public ByteArrayResource export() {
        return new ByteArrayResource("订单清单".getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/stream")
    @Operation(summary = "流式导出订单")
    public StreamingResponseBody stream() {
        return output -> output.write("订单清单".getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/ack")
    @Operation(summary = "确认收到导出请求")
    public void acknowledge() {
    }
}
