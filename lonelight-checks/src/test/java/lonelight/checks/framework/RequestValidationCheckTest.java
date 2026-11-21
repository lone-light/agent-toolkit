package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证请求模型约束只在可识别的绑定入口触发
 *
 * @author lonelight
 */
class RequestValidationCheckTest extends CustomCheckTest {
    /**
     * 验证标准约束、级联和继承，以及已启用校验的例外
     */
    @Test
    void scan_requestModels_requiresValidationOnBoundModels() throws Exception {
        verify(RequestValidationCheck.class, """
                import jakarta.validation.*;
                import jakarta.validation.constraints.*;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.*;
                record Query(@NotBlank String text) {}
                class Base { @NotNull Long id; }
                class Input extends Base { @Valid Query query; }
                class Wrapper<T> { T value; }
                class PlainInput extends Wrapper<Query> {}
                class Local {
                    @interface NotBlank {}
                    @interface Valid {}
                }
                @RestController @Validated class Web {
                    @PostMapping void body(@RequestBody Input input) {}
                    @GetMapping void implicit(Query query) {}
                    @PostMapping void form(@ModelAttribute Input input) {}
                    @PostMapping void part(@RequestPart Input input) {}
                    @PostMapping void accepted(@Valid @RequestBody Input input) {}
                    @GetMapping void grouped(@Validated @ModelAttribute Query query) {}
                    @GetMapping void header(@RequestHeader Query query) {}
                    @GetMapping void internal(@RequestAttribute Query query) {}
                    @PostMapping void generic(@RequestBody java.util.List<@Valid Query> query) {}
                    @PostMapping void withoutCascade(@RequestBody PlainInput input) {}
                    @PostMapping void raw(@RequestBody String text) {}
                    void helper(Input input) {}
                }
                """, 4);
    }

    /**
     * 验证成员 record 上的显式访问器，并区分普通方法返回值约束
     */
    @Test
    void scan_recordGetters_checksBeanProperties() throws Exception {
        var file = CheckTestSupport.write(directoryPath, "Orders.java", """
                package app.web;
                import jakarta.validation.constraints.AssertTrue;
                import jakarta.validation.constraints.NotBlank;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;

                @RestController class Orders {
                    @PostMapping("/reference")
                    String create(@RequestBody Payload.Order order) { return order.reference(); }

                    @PostMapping("/status")
                    String status(@RequestBody Status status) { return status.state(); }

                    @PostMapping("/verification")
                    boolean verify(@RequestBody Verification verification) { return verification.active(); }

                    static class Payload {
                        record Order(String reference) {
                            @NotBlank(message = "订单编号不能为空")
                            public String getReference() { return reference; }
                        }
                    }

                    record Status(String state) {
                        @NotBlank(message = "状态不能为空")
                        public String isCurrent() { return state; }
                    }

                    record Verification(boolean active) {
                        @AssertTrue(message = "状态必须有效")
                        public boolean isActive() { return active; }
                    }
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, RequestValidationCheck.class.getName())).toList();
        assertEquals(List.of(10, 16), events.stream().map(AuditEvent::getLine).toList());
    }

    /**
     * 验证从同一源码根读取外部模型和自定义约束声明
     */
    @Test
    void scan_crossFileConstraints_requiresValidation() throws Exception {
        CheckTestSupport.write(directoryPath, "CatalogCode.java", """
                package app.api;
                import jakarta.validation.Constraint;
                @Constraint(validatedBy = {}) public @interface CatalogCode {}
                """);
        CheckTestSupport.write(directoryPath, "Search.java", """
                package app.api;
                public record Search(@CatalogCode String query) {}
                """);
        var file = CheckTestSupport.write(directoryPath, "Catalog.java", """
                package app.web;
                import app.api.Search;
                import org.springframework.web.bind.annotation.*;
                @RestController class Catalog {
                    @GetMapping void search(Search search) {}
                }
                """);
        var events = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), file).stream()
                .filter(event -> CheckTestSupport.matches(event, RequestValidationCheck.class.getName())).toList();
        assertEquals(1, events.size());
    }

    /**
     * 验证带类型注解的限定父类也能提供继承的约束
     */
    @Test
    void scan_annotatedSupertype_requiresValidation() throws Exception {
        verify(RequestValidationCheck.class, """
                import jakarta.validation.constraints.NotNull;
                import org.springframework.web.bind.annotation.*;
                @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Mark {}
                class Models { static class Base { @NotNull String text; } }
                class Input extends Models.@Mark Base {}
                @RestController class Example {
                    @PostMapping void submit(@RequestBody Input input) {}
                }
                """, 1);
    }

    /**
     * 验证跨文件模型能解析同一编译单元中的包级父类
     */
    @Test
    void scan_packagePrivateSuperclassInModelFile_requiresValidation() throws Exception {
        CheckTestSupport.write(directoryPath, "Input.java", """
                package app.api;
                public class Input extends Base {}
                class Base { @jakarta.validation.constraints.NotNull String text; }
                """);
        var controller = CheckTestSupport.write(directoryPath, "Orders.java", """
                package app.web;
                @org.springframework.web.bind.annotation.RestController class Orders {
                    @org.springframework.web.bind.annotation.PostMapping
                    void submit(@org.springframework.web.bind.annotation.RequestBody app.api.Input input) {}
                }
                """);
        var check = CheckTestSupport.rule(RequestValidationCheck.class.getName());
        var events = CheckTestSupport.scan(CheckTestSupport.isolate(check, true), controller);
        assertEquals(1, events.size());
    }
}
