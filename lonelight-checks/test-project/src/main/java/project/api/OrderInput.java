package project.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 保存提交订单时接受的字段
 *
 * @param number 订单编号
 * @param generatedId 仅响应侧公开的生成编号
 * @param internalToken 不公开的内部标记
 * @author lonelight
 */
public record OrderInput(
        @NotBlank(message = "订单编号不能为空")
        @Schema(description = "订单编号") String number,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) String generatedId,
        @Schema(hidden = true) String internalToken
) {
}
