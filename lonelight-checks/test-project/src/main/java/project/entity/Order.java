package project.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public record Order(@Schema(description = "持久化订单编号") long id) { }
