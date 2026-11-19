// 检查：ControllerApiDocumentationCheck

package project.web;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对照请求头、Cookie 和框架注入参数的说明
 *
 * @author lonelight
 */
@RestController
@RequestMapping("/orders/access")
public class OrderAccessController {
    // 预期提示请求头缺少说明
    @GetMapping("/trace")
    @Operation(summary = "读取订单追踪编号")
    public String trace(@RequestHeader("X-Trace") String traceId) {
        return traceId;
    }

    // 正例的请求头说明忽略大小写
    @GetMapping("/trace-documented")
    @Operation(summary = "读取已说明的追踪编号")
    @Parameters(@Parameter(name = "x-trace", in = ParameterIn.HEADER, description = "追踪编号"))
    public String traceDocumented(@RequestHeader("X-Trace") String traceId) {
        return traceId;
    }

    // 预期提示公开 Cookie 缺少说明
    @GetMapping("/session")
    @Operation(summary = "读取当前订单会话")
    public String session(@CookieValue("session") String sessionId) {
        return sessionId;
    }

    // 正例中的参数为框架注入或显式隐藏
    @GetMapping("/metadata")
    @Operation(summary = "读取订单请求元数据")
    public String metadata(
            HttpServletRequest httpServletRequest,
            @RequestAttribute String traceId,
            @Parameter(hidden = true) @CookieValue("session") String sessionId) {
        return httpServletRequest.getMethod() + traceId;
    }

    // 正例中的整个接口已隐藏
    @Hidden
    @GetMapping("/internal")
    public String internal(@RequestParam String token) {
        return token;
    }
}
