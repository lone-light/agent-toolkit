package project.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

// 接口联动样例：通过泛型响应发现缺少说明的 orderId
public record OrderSnapshot(
        long orderId,
        @Schema(description = "状态") String status,
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String secretInput,
        @JsonIgnore String internalToken
) { }
