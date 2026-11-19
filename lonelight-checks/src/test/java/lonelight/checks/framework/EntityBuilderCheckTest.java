package lonelight.checks.framework;

import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证实体创建检查使用已声明的可访问构建器
 *
 * @author lonelight
 */
class EntityBuilderCheckTest extends CustomCheckTest {
    /**
     * 验证跨文件实体、实体内部工厂及普通对象的不同处理
     */
    @Test
    void scan_entityCreation_reportsExternalBuilderBypass() throws Exception {
        CheckTestSupport.write(directoryPath, "Stock.java", """
                package app.entity;
                public class Stock {
                    public static Builder builder() { return new Builder(); }
                    static Stock internal() { return new Stock(); }
                    static class Builder {}
                }
                """);
        CheckTestSupport.write(directoryPath, "Receipt.java", """
                package app.entity;
                public class Receipt {}
                """);
        var file = CheckTestSupport.write(directoryPath, "Warehouse.java", """
                package app.service;
                import app.entity.Receipt;
                import app.entity.Stock;
                class Warehouse {
                    Stock direct() { return new Stock(); }
                    Object factory() { return Stock.builder(); }
                    Object simple() { return new Receipt(); }
                    Stock anonymous() { return new Stock() {}; }
                    Stock[] array() { return new Stock[1]; }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, EntityBuilderCheck.class.getName())).toList();
        assertEquals(1, events.size());
    }

    /**
     * 验证默认 Lombok 构建器和关闭或限制构建器的情况
     */
    @Test
    void scan_lombokBuilders_checksPublicEnabledBuilders() throws Exception {
        verify(EntityBuilderCheck.class, """
                import jakarta.persistence.Entity;
                import lombok.AccessLevel;
                import lombok.Builder;
                @Entity @Builder class Stock {}
                @Entity @Builder(builderMethodName = "") class Hidden {}
                @Entity @Builder(access = AccessLevel.PRIVATE) class PrivateBuilder {}
                @Entity record Snapshot(int value) {
                    public static Object builder() { return new Object(); }
                }
                @Builder class Command {}
                class Example {
                    void build() {
                        new Stock();
                        new Hidden();
                        new PrivateBuilder();
                        new Snapshot(1);
                        new Command();
                    }
                }
                """, 2);
    }
}
