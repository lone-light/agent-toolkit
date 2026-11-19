package tests;

import java.util.List;
import java.util.Map;

import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.ERROR;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.WARNING;
import static tests.RuleCase.method;
import static tests.RuleCase.type;

/**
 * 项目角色、注解与分层约定的基础正反例
 *
 * @author example
 */
// 测试用例保留边界数值与重复文本，便于逐项阅读
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
final class ProjectRuleCases {

    private ProjectRuleCases() {}

    static List<RuleCase> cases() {
        return List.of(
                new RuleCase("MatchXpath#testMethodName", WARNING, type("@Test void run_WhenReady_ReturnsValue() {}"),
                        type("@Test void run() {}")),
                new RuleCase("MatchXpath#projectTypeSuffix", WARNING, "package sample.dto; class ExampleDTO {}",
                        "package sample.dto; class Example {}"),
                new RuleCase("MatchXpath#roleInstanceName", WARNING, type("UserService primaryUserService;"),
                        type("UserService service;")),
                new RuleCase("MatchXpath#fieldSchema", ERROR,
                        "class ExampleDTO { @Schema(description=\"名称\") String name; }",
                        "class ExampleDTO { String name; }"),
                new RuleCase("MatchXpath#fieldBooleanPrefix", ERROR, "class ExampleDTO { @Schema boolean active; }",
                        "class ExampleDTO { @Schema boolean isActive; }"),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR,
                        type("/**\n * 名称\n */\nString name;"), type("String name;")),
                new RuleCase("MatchXpath#typeSummary", WARNING,
                        "/**\n * 示例类型\n *\n * @author example\n */\nclass Example {}",
                        "/**\n * 示例\n * 类型\n */\nclass Example {}"),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING, type("/** 查询用户 */\nvoid run() {}"),
                        type("/** 查询用户。 */\nvoid run() {}")),
                new RuleCase("MatchXpath#methodJavadocNumbering", WARNING, type("/** 创建用户 */\nvoid run() {}"),
                        type("/** 1. 创建用户 */\nvoid run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR, type("@Operation(summary=\"查询\") void run() {}"),
                        type("@Operation(summary=\" \" + \"\") void run() {}")),
                new RuleCase("MatchXpath#annotationText", ERROR, "@Schema(description=\"示例\") class Example {}",
                        "@Schema(description=\"\") class Example {}"),
                new RuleCase("MatchXpath#schemaRequiredMode", ERROR, "@Schema(description=\"示例\") class Example {}",
                        "@Schema(description=\"示例\", requiredMode=REQUIRED) class Example {}"),
                new RuleCase("MatchXpath#schemaDefaultExample", ERROR,
                        "class PageDTO { @Schema(description=\"页码\", example=\"1\") int page = 1; }",
                        "class PageDTO { @Schema(description=\"页码\") int page = 1; }"),
                new RuleCase("MatchXpath#requestParamDefault", ERROR,
                        type("void run(@RequestParam(required=false) String name) {}"),
                        type("void run(@RequestParam(required=true) String name) {}")),
                new RuleCase("MatchXpath#logDesensitize", ERROR,
                        type("@Log(value=\"登录\", params=\"#user.password\", desensitize=true) void run() {}"),
                        type("@Log(value=\"登录\", params=\"#user.password\") void run() {}")),
                new RuleCase("MatchXpath#loggerRole", ERROR,
                        "@Slf4j class ExampleService {}", "@Slf4j class ExampleDTO {}"),
                new RuleCase("MatchXpath#aspectContract", ERROR, "@Aspect @Component @Order(1) class Example {}",
                        "@Aspect class Example {}"),
                new RuleCase("MatchXpath#controllerOperation", ERROR,
                        "@RestController class Example { @GetMapping @Operation(summary=\"查询\") void run() {} }",
                        "@RestController class Example { @GetMapping void run() {} }"),
                new RuleCase("MatchXpath#controllerJavadoc", ERROR,
                        "@RestController class Example { @GetMapping @Operation void run() {} }",
                        "@RestController class Example { /** 查询 */ @GetMapping @Operation void run() {} }"),
                new RuleCase("MatchXpath#controllerApiPrefix", ERROR,
                        "@RestController @RequestMapping(\"/users\") class Example {}",
                        "@RestController @RequestMapping(\"/api/users\") class Example {}"),
                new RuleCase("MatchXpath#controllerPath", ERROR,
                        "@RestController @RequestMapping(\"/user-profiles/{userId}\") class Example {}",
                        "@RestController @RequestMapping(\"/User_List\") class Example {}"),
                new RuleCase("MatchXpath#controllerMapper", ERROR,
                        "@RestController class Example { UserService userService; }",
                        "@RestController class Example { UserMapper userMapper; }"),
                new RuleCase("MatchXpath#serviceHttp", ERROR, "@Service class Example { void run(UserDTO userDTO) {} }",
                        "@Service class Example { void run(HttpServletRequest request) {} }"),
                new RuleCase("MatchXpath#layerEntityReturn", ERROR,
                        "@Service class Example { UserDTO run() { return null; } }",
                        "@Service class Example { java.util.List<UserEntity> run() { return null; } }"),
                new RuleCase("MatchXpath#beanUtilsCall", ERROR,
                        method("sample.BeanUtils.copyProperties(first, second);"),
                        method("org.springframework.beans.BeanUtils.copyProperties(first, second);")),
                new RuleCase("MatchXpath#loggingPlaceholder", ERROR,
                        "@Slf4j class Example { void run(String id) { log.info(\"用户 {}\", id); } }",
                        "@Slf4j class Example { void run(String id) { log.info(\"用户\" + id); } }"),
                new RuleCase("MatchXpath#converterFinal", WARNING,
                        "final class ExampleConverter { private ExampleConverter() {} }",
                        "class ExampleConverter { private ExampleConverter() {} }"),
                new RuleCase("MatchXpath#converterConstructor", ERROR,
                        "final class ExampleConverter { private ExampleConverter() {} }",
                        "final class ExampleConverter { public ExampleConverter() {} }"));
    }

    /**
     * 完整配置下应当通过的项目源码
     *
     * @return 按文件名组织的合规源码
     */
    static Map<String, String> validSources() {
        return Map.of(
                "UserDTO.java", """
                        package examples.dto;

                        /**
                         * 用户数据
                         *
                         * @param name 用户名称
                         * @param active 是否启用
                         * @author example
                         */
                        public record UserDTO(
                            @io.swagger.v3.oas.annotations.media.Schema(description = "名称") String name,
                            @io.swagger.v3.oas.annotations.media.Schema(description = "启用状态") boolean active) {}
                        """,
                "UserService.java", """
                        package examples.service;

                        /**
                         * 用户服务
                         *
                         * @author example
                         */
                        public interface UserService {

                            /**
                             * 查询用户
                             *
                             * @param id 用户编号
                             * @return 用户数据
                             */
                            examples.dto.UserDTO query(long id);
                        }
                        """,
                "UserController.java", """
                        package examples.controller;

                        /**
                         * 用户接口
                         *
                         * @author example
                         */
                        @org.springframework.web.bind.annotation.RestController
                        @org.springframework.web.bind.annotation.RequestMapping("/users")
                        public class UserController {

                            /**
                             * 用户服务
                             */
                            private final examples.service.UserService userService;

                            /**
                             * 创建用户接口
                             *
                             * @param userService 用户服务
                             */
                            public UserController(examples.service.UserService userService) {
                                this.userService = userService;
                            }

                            @org.springframework.web.bind.annotation.GetMapping("/{id}")
                            @io.swagger.v3.oas.annotations.Operation(summary = "查询用户")
                            public examples.dto.UserDTO query(
                                @org.springframework.web.bind.annotation.PathVariable("id") long id) {
                                return userService.query(id);
                            }
                        }
                        """,
                "ExampleTest.java", """
                        package examples;

                        /**
                         * 用户测试
                         *
                         * @author example
                         */
                        public class ExampleTest {

                            @org.junit.jupiter.api.Test
                            public void query_WhenReady_ReturnsValue() {}
                        }
                        """,
                "OverrideExample.java", """
                        package examples;

                        /**
                         * 覆写示例
                         *
                         * @author example
                         */
                        public class OverrideExample {

                            @java.lang.Override
                            public String toString() {
                                return "example";
                            }
                        }
                        """);
    }

}
