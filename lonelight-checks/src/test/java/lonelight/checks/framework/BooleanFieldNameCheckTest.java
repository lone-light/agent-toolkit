package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证布尔字段规则沿接口签名识别模型并豁免持久化实体
 *
 * @author lonelight
 */
class BooleanFieldNameCheckTest extends CustomCheckTest {
    /**
     * 验证未参与接口的模型、内部参数及隐藏接口不会触发检查
     */
    @Test
    void scan_hiddenAndExposedModels_checksExposedFields() throws Exception {
        verify(BooleanFieldNameCheck.class, """
                import io.swagger.v3.oas.annotations.Hidden;
                import org.springframework.web.bind.annotation.*;
                record Internal(boolean isReady) {}
                record HiddenModel(Boolean isActive) {}
                @RestController class Web {
                    @GetMapping void internal(@RequestAttribute Internal internal) {}
                    @Hidden @GetMapping HiddenModel hidden() { return null; }
                    Internal helper() { return null; }
                }
                """, 0);
    }

    /**
     * 验证跨文件、数组、泛型和继承路径会报告公开布尔字段
     */
    @Test
    void scan_nestedModels_reportsExposedFieldsOnce() throws Exception {
        CheckTestSupport.write(directoryPath, "Stored.java", """
                package app.entity;
                public record Stored(Boolean isDeleted) {}
                """);
        CheckTestSupport.write(directoryPath, "Payload.java", """
                package app.api;
                import app.entity.Stored;
                import com.fasterxml.jackson.annotation.JsonIgnore;
                public class Payload extends Base {
                    Details[] details;
                    Stored stored;
                    @JsonIgnore Boolean isHidden;
                    boolean[] isFlags;
                    static boolean isShared;
                    record Details(boolean isReady, boolean issue) {}
                }
                """);
        CheckTestSupport.write(directoryPath, "Base.java", """
                package app.api;
                class Base { Boolean isActive; }
                """);
        var file = CheckTestSupport.write(directoryPath, "Web.java", """
                package app.web;
                import app.api.Payload;
                import app.entity.Stored;
                import java.util.List;
                import org.springframework.web.bind.annotation.*;
                @RestController class Web {
                    @GetMapping List<Payload> read() { return null; }
                    @PostMapping Payload update(@RequestBody Payload payload) { return payload; }
                    @GetMapping("stored") Stored stored() { return null; }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, BooleanFieldNameCheck.class.getName()))
                .map(AuditEvent::getMessage).toList();
        assertEquals(2, events.size(), events.toString());
        assertTrue(events.stream().anyMatch(message -> message.contains("app.api.Payload.Details") && message.contains("isReady")));
        assertTrue(events.stream().anyMatch(message -> message.contains("app.api.Base") && message.contains("isActive")));
    }
}
