package lonelight.checks.framework;

import lonelight.checks.CheckTestSupport;
import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

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
                import jakarta.validation.Valid;
                import jakarta.validation.constraints.*;
                import org.springframework.validation.annotation.Validated;
                import org.springframework.web.bind.annotation.*;
                record Query(@NotBlank String text) {}
                class Base { @NotNull Long id; }
                class Input extends Base { @Valid Query query; }
                class Wrapper<T> { T value; }
                class PlainInput extends Wrapper<Query> {}
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
}
