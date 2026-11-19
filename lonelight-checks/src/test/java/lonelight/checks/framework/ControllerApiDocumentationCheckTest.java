package lonelight.checks.framework;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import lonelight.checks.CheckTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 REST 接口和请求响应模型的注解检查
 *
 * @author lonelight
 */
class ControllerApiDocumentationCheckTest {
    private static final String RULE = ControllerApiDocumentationCheck.class.getName();

    @TempDir
    Path directoryPath;

    /**
     * 验证缺少摘要或公开参数说明的接口会被报告
     */
    @Test
    void scan_restEndpoints_reportsMissingOrBlankDescriptions() throws Exception {
        Path sourcePath = write("Users.java", """
                package app.web;
                import org.springframework.web.bind.annotation.*;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.Parameter;
                import jakarta.servlet.http.HttpServletRequest;

                @RestController class Users {
                    @GetMapping("/{id}")
                    void missing(@PathVariable Long id, @RequestHeader String trace, HttpServletRequest request) {}
                    @PostMapping
                    @Operation(summary = " " + "\\t")
                    void empty(@RequestParam @Parameter(description = " ") String query, java.util.Optional<Integer> limit) {}
                    @GetMapping
                    @Operation(summary = "查询")
                    void good(@RequestParam @Parameter(description = "页数") Integer page) {}
                    void helper(String input) {}
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(List.of(9, 9, 9, 12, 12, 12), events.stream().map(AuditEvent::getLine).toList());
        assertEquals(2, count(events, "@Operation"));
        assertEquals(4, count(events, "@Parameter"));
        assertTrue(events.stream().allMatch(event -> event.getSeverityLevel() == SeverityLevel.WARNING));
        assertEquals(0, CheckTestSupport.scan(CheckTestSupport.load("lonelight-base"), sourcePath).stream()
                .filter(event -> CheckTestSupport.matches(event, RULE)).count());
    }

    /**
     * 验证请求和响应模型可跨文件按类型引用定位
     */
    @Test
    void scan_requestAndResponseTypes_followsSourceReferences() throws Exception {
        write("OrderData.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.Schema;
                public record OrderData(
                    @Schema(description = "客户") String customer,
                    /** Internal prose is not a public field annotation. */ String email,
                    Address address
                ) {}
                """);
        write("Address.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.Schema;
                import com.fasterxml.jackson.annotation.JsonIgnore;
                public class Address {
                    @Schema(description = "地址") String city;
                    @JsonIgnore String privateCode;
                    @Schema(hidden = true) String hiddenCode;
                    static String constant;
                    @Schema(description = "上一笔") OrderData parent;
                    @Schema(description = " ") String postcode;
                }
                """);
        write("Envelope.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.Schema;
                public record Envelope<T>(@Schema(description = "结果") T data) {}
                """);
        write("Snapshot.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.Schema;
                public record Snapshot(@Schema(description = "结果编号") String reference, String note) {}
                """);
        Path sourcePath = write("Orders.java", """
                package app.web;
                import java.util.List;
                import app.data.*;
                import org.springframework.web.bind.annotation.*;
                import org.springframework.http.ResponseEntity;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.media.Schema;
                @RestController class Orders {
                    @PostMapping
                    @Operation(summary = "查询订单")
                    ResponseEntity<Envelope<List<? extends app.data.Snapshot[]>>> find(@RequestBody OrderData body) {
                        return null;
                    }
                    record UnusedDTO(String ignored) {}
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(4, events.size(), () -> messages(events).toString());
        assertEquals(1, count(events, "app.data.OrderData 的字段 email"));
        assertEquals(1, count(events, "app.data.OrderData 的字段 address"));
        assertEquals(1, count(events, "app.data.Address 的字段 postcode"));
        assertEquals(1, count(events, "app.data.Snapshot 的字段 note"));
        assertTrue(events.stream().allMatch(event -> Path.of(event.getFileName()).equals(sourcePath)));
    }

    /**
     * 验证参数别名说明和显式隐藏接口受到支持
     */
    @Test
    void scan_parameterAliasesAndHiddenEndpoints_acceptsDocumentedApi() throws Exception {
        Path sourcePath = write("Users.java", """
                package app.web;
                import io.swagger.v3.oas.annotations.Hidden;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.Parameters;
                import io.swagger.v3.oas.annotations.enums.ParameterIn;
                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.*;

                @Controller class Users {
                    @GetMapping("/{id}")
                    @ResponseBody
                    @Operation(summary = Labels.READ, parameters = @Parameter(name = "id", description = "用户编号"))
                    @Parameters(@Parameter(name = "X-Trace", in = ParameterIn.HEADER, ref = "#/components/parameters/trace"))
                    void valid(@PathVariable("id") int userId, @RequestHeader(name = "x-trace") String traceId,
                               @RequestParam @Parameter(hidden = true) String skip) {}
                    @GetMapping @ResponseBody @Hidden void hidden(@RequestParam String skip) {}
                    @GetMapping @ResponseBody @Operation(hidden = true) void hiddenOperation(@RequestParam String skip) {}
                    @GetMapping String view(@RequestParam String skip) { return "index"; }
                }
                """);
        assertTrue(projectEvents(sourcePath).isEmpty(), "valid annotations should satisfy the rule");
    }

    /**
     * 验证参数说明同时匹配名称、位置和注解类型
     */
    @Test
    void scan_parameterDocumentation_matchesBindingNameAndLocation() throws Exception {
        Path sourcePath = write("Users.java", """
                package app.web;
                import org.springframework.web.bind.annotation.*;
                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.enums.ParameterIn;
                import other.Operation;
                @RestController class Users {
                    @GetMapping("/{id}")
                    @Operation(summary = "同名的其他注解")
                    @Parameter(name = "id", in = ParameterIn.QUERY, description = "位置错误")
                    void read(@PathVariable Long id) {}
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(2, events.size(), () -> messages(events).toString());
        assertEquals(1, count(events, "非空 summary"));
        assertEquals(1, count(events, "公开请求参数"));
    }

    /**
     * 验证原始请求体和表单模型使用各自的说明要求
     */
    @Test
    void scan_requestBodiesAndFormModels_requiresRelevantDescription() throws Exception {
        Path sourcePath = write("Search.java", """
                package app.web;
                import org.springframework.web.bind.annotation.*;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.media.Schema;
                import java.util.Map;

                @RestController class Search {
                    @PostMapping @Operation(summary = "原始请求")
                    void raw(@RequestBody Map<String, Object> body) {}

                    @PostMapping
                    @Operation(summary = "有说明", requestBody =
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "原始内容"))
                    void described(@RequestBody String raw) {}

                    @GetMapping @Operation(summary = "检索")
                    void search(@ModelAttribute Filters filters) {}

                    @GetMapping @Operation(summary = "筛选")
                    void implicit(@Parameter(description = "补充筛选") OtherFilters filters) {}

                    record Filters(@Schema(description = "关键词") String keyword, String type) {}
                    record OtherFilters(String group) {}
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(3, events.size(), () -> messages(events).toString());
        assertEquals(1, count(events, "请求体需要"));
        assertEquals(1, count(events, "app.web.Search.Filters 的字段 type"));
        assertEquals(1, count(events, "app.web.Search.OtherFilters 的字段 group"));
    }

    /**
     * 验证复扫会读取更新后的模型并跳过缺失源码
     */
    @Test
    void scan_updatedAndMissingModels_refreshesAvailableTypes() throws Exception {
        String definition = "package app.data; public record Entry(String label) {}";
        write("Entry.java", definition);
        Path sourcePath = write("Users.java", """
                package app.web;
                import app.data.Entry;
                import external.Binary;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.media.Schema;
                import org.springframework.web.bind.annotation.*;

                @RestController class Users {
                    @GetMapping @Operation(summary = "取得条目") Entry read() { return null; }
                    @GetMapping @Operation(summary = "取得外部资料") Binary external() { return null; }
                }
                """);
        assertEquals(1, count(projectEvents(sourcePath), "app.data.Entry 的字段 label"));
        write("Entry.java", definition.replace("String label",
                "@io.swagger.v3.oas.annotations.media.Schema(description = \"标签\") String label"));
        assertTrue(projectEvents(sourcePath).isEmpty());
    }

    /**
     * 验证跨文件的数组说明使用模型自身的导入解析
     */
    @Test
    void arraySchema_usesModelImports_reportsOnlyBlankDescription() throws Exception {
        write("Item.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.ArraySchema;
                import io.swagger.v3.oas.annotations.media.Schema;
                import java.util.List;

                public record Item(
                    @ArraySchema(arraySchema = @Schema(description = "订单标签")) List<String> tags,
                    @ArraySchema(arraySchema = @Schema(description = " ")) List<String> notes
                ) {}
                """);
        Path sourcePath = write("Users.java", """
                package app.web;
                import app.data.Item;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController class Users {
                    @GetMapping
                    @Operation(summary = "读取订单")
                    Item read() { return null; }
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(1, events.size(), () -> messages(events).toString());
        assertEquals(1, count(events, "字段 notes"));
    }

    /**
     * 验证模型诊断按字段去重并区分读写方向
     */
    @Test
    void scan_sharedModels_checksAccessDirectionAndDeduplicates() throws Exception {
        write("Item.java", """
                package app.data;
                import io.swagger.v3.oas.annotations.media.Schema;
                import com.fasterxml.jackson.annotation.JsonProperty;
                public record Item(
                    String label,
                    @Schema(accessMode = Schema.AccessMode.READ_ONLY) int serial,
                    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String secret
                ) {}
                """);
        Path sourcePath = write("Users.java", """
                package app.web;
                import app.data.Item;
                import org.springframework.web.bind.annotation.*;
                import io.swagger.v3.oas.annotations.Operation;
                @RestController class Users {
                    @GetMapping @Operation(summary = "读")
                    Item read() { return null; }
                    @GetMapping @Operation(summary = "再读")
                    Item readAgain() { return null; }
                    @PostMapping @Operation(summary = "写")
                    void write(@RequestBody Item item) {}
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(3, events.size(), () -> messages(events).toString());
        assertEquals(List.of(7, 7, 11), events.stream().map(AuditEvent::getLine).toList());
        assertEquals(1, count(events, "字段 label"));
        assertEquals(1, count(events, "字段 serial"));
        assertEquals(1, count(events, "字段 secret"));
    }

    /**
     * 验证隐式枚举参数及包内父类字段的说明检查
     */
    @Test
    void scan_implicitEnumAndInheritedFields_checksBothDescriptions() throws Exception {
        write("Item.java", """
                package app.data;
                public class Item extends Base {
                    @io.swagger.v3.oas.annotations.media.Schema(description = "主键") Long id;
                    int compute() { return 0; }
                }
                """);
        write("Base.java", "package app.data; class Base { String note; }");
        write("Status.java", "package app.data; public enum Status { AVAILABLE }");
        Path sourcePath = write("Users.java", """
                package app.web;
                import app.data.Item;
                import app.data.Status;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.http.ResponseEntity;
                import org.springframework.stereotype.Controller;
                import org.springframework.web.bind.annotation.GetMapping;
                @Controller class Users {
                    @GetMapping @Operation(summary = "读")
                    ResponseEntity<Item> read(Status status) { return null; }
                }
                """);
        var events = projectEvents(sourcePath);
        assertEquals(2, events.size(), () -> messages(events).toString());
        assertEquals(1, count(events, "app.data.Base 的字段 note"));
        assertEquals(1, count(events, "@Parameter"));
    }

    /**
     * 把模型或接口源码写入当前用例目录
     *
     * @param filename Java 文件名
     * @param input    待写入的源码
     * @return 已写入的文件路径
     * @throws Exception 目录创建或写入失败时
     */
    private Path write(String filename, String input) throws Exception {
        return CheckTestSupport.write(directoryPath, filename, input);
    }

    /**
     * 从项目配置的扫描结果中筛出接口文档诊断
     *
     * @param sourcePath 待扫描的源码路径
     * @return 接口文档检查产生的诊断
     * @throws Exception 配置读取或扫描失败时
     */
    private static List<AuditEvent> projectEvents(Path sourcePath) throws Exception {
        return CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), sourcePath).stream()
                .filter(event -> CheckTestSupport.matches(event, RULE)).toList();
    }

    /**
     * 提取诊断文本以便断言
     *
     * @param events 扫描事件列表
     * @return 按事件顺序排列的诊断文本
     */
    private static List<String> messages(List<AuditEvent> events) {
        return events.stream().map(AuditEvent::getMessage).toList();
    }

    /**
     * 统计包含指定文本的诊断
     *
     * @param events 扫描事件列表
     * @param text   需要匹配的消息片段
     * @return 包含该片段的诊断数量
     */
    private static long count(List<AuditEvent> events, String text) {
        return messages(events).stream().filter(message -> message.contains(text)).count();
    }
}
