package project.api;

import io.swagger.v3.oas.annotations.media.Schema;

// 接口联动样例：应在 OrderController 的请求参数上报告 comment
public record OrderInput(
        String comment,
        @Schema(description = "数量") int quantity,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) String generatedId,
        @Schema(hidden = true) String internalToken
) { }
