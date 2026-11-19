package project.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 保存对外展示的订单信息
 *
 * @param orderId 订单编号
 * @param tags 订单标签
 * @param internalNote 仅允许写入的内部说明
 * @author lonelight
 */
public record OrderView(
        @Schema(description = "订单编号") long orderId,
        @ArraySchema(arraySchema = @Schema(description = "订单标签")) List<String> tags,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String internalNote
) {
}
