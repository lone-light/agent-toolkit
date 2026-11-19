package project.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 保存订单详情返回的数据
 *
 * @param orderId 订单编号
 * @param status 当前状态
 * @param internalToken 不公开的内部标记
 * @author lonelight
 */
public record OrderSnapshot(
        long orderId,
        @Schema(description = "状态") String status,
        @JsonIgnore String internalToken
) {
}
