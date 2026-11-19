package tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.ERROR;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.INFO;
import static com.puppycrawl.tools.checkstyle.api.SeverityLevel.WARNING;
import static tests.RuleCase.method;
import static tests.RuleCase.type;

/**
 * 规则分支、语法变体与误报边界的正反例
 *
 * @author example
 */
// 测试用例保留边界数值与重复文本，便于逐项阅读
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
final class BoundaryRuleCases {

    private BoundaryRuleCases() {}

    static List<RuleCase> cases() {
        List<RuleCase> cases = new ArrayList<>(List.of(
                new RuleCase("LineLength", INFO,
                        "// https://example.com/" + "x".repeat(140) + "\n", "// " + "x".repeat(121)),
                new RuleCase("ClassTypeParameterName", WARNING,
                        "class Example<ResultT> {}", "class Example<result> {}"),
                new RuleCase("UnusedImports", WARNING, "import java.util.List; /** {@link List} */ class Example {}",
                        "import java.util.List; class Example {}"),
                new RuleCase("IllegalImport", ERROR, "import com.sun.source.tree.Tree; class Example {}",
                        "import jdk.internal.misc.Unsafe; class Example {}"),
                new RuleCase("IllegalImport", ERROR, "import static sample.BeanUtils.copyProperties; class Example {}",
                        "import static org.springframework.beans.BeanUtils.*; class Example {}"),
                new RuleCase("MagicNumber", WARNING, "@Order(42) class Example {}", type("int run() { return 42; }")),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("@Override public void run() {}"),
                        "public " + type("public void run() {}")),
                new RuleCase("JavadocMethod", ERROR, type("/**\n * 结果\n *\n * @param value 输入\n * @return 结果\n "
                        + "*/\nprivate int run(int value) { return value; }"),
                        type("/** 结果 */ private int run(int value) { return value; }")),
                new RuleCase("MatchXpath#projectTypeSuffix", WARNING,
                        "import other.RestController; @RestController class Example {}",
                        "@org.springframework.web.bind.annotation.RestController class Example {}"),
                new RuleCase("MatchXpath#projectTypeSuffix", WARNING,
                        "import org.mapstruct.Mapper; @Mapper interface UserConverter {}",
                        "@Mapper interface UserDao {}"),
                new RuleCase("MatchXpath#roleInstanceName", WARNING, type("UserDTO[] users;"), type("UserDTO dto;")),
                new RuleCase("MatchXpath#roleInstanceName", WARNING,
                        type("void run(UserDTO sourceUserDTO, UserDTO targetUserDTO) {}"),
                        type("void run(UserDTO dto) {}")),
                new RuleCase("MatchXpath#fieldSchema", ERROR, "class ExampleDTO { static String name; }",
                        "record ExampleDTO(String name) {}"),
                new RuleCase("MatchXpath#fieldSchema", ERROR, "record ExampleDTO(@Schema String name) {}",
                        "import other.Schema; class ExampleDTO { @Schema String name; }"),
                new RuleCase("MatchXpath#fieldBooleanPrefix", ERROR, "class ExampleDTO { @Schema Boolean[] isActive; }",
                        "record ExampleDTO(@Schema Boolean isActive) {}"),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR, type("/**\n * 名称\n */ String name;"),
                        type("/** 名称 */ String name;")),
                new RuleCase("MatchXpath#typeSummary", WARNING, "/** 说明 */ class Example {}",
                        "/** @author example */ class Example {}"),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING,
                        type("/** {@code if (a) { { value. } }} */ void run() {}"),
                        type("/** {@code value.} 后续说明。 */ void run() {}")),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING, type("/** <pre>value.</pre> */ void run() {}"),
                        type("/** <p>说明。</p> */ void run() {}")),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING,
                        type("/** <span title=\"a >\n * b.\n * \">说明</span> */ void run() {}"),
                        type("/** <span title='a >\n * b.\n * '>说明。</span> */ void run() {}")),
                new RuleCase("MatchXpath#methodJavadocNumbering", WARNING,
                        type("/** 说明\n * @return 1. 结果\n */ int run() { return 1; }"),
                        type("/** 1、返回结果 */ int run() { return 1; }")),
                new RuleCase("MatchXpath#operationSummary", ERROR,
                        type("@Operation(summary=\"\\\\u0020\") void run() {}"),
                        type("@Operation(summary=\"\\u0020\") void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR,
                        type("@Operation(summary=Constants.SUMMARY) void run() {}"),
                        type("@Operation(summary=(\" \" + (\"\" + \"\\t\"))) void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR,
                        "import other.Operation; class Example { @Operation void run() {} }",
                        type("@io.swagger.v3.oas.annotations.Operation void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR, type("@Operation(summary=\"\\101\") void run() {}"),
                        type("@Operation(summary=\"\\040\\11\") void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR,
                        type("@Operation(summary=\"\"\"\n查询\n\"\"\") void run() {}"),
                        type("@Operation(summary=\"\"\"\n  \n\"\"\") void run() {}")),
                new RuleCase("MatchXpath#annotationText", ERROR,
                        "import other.NotNull; class Example { @NotNull String name; }",
                        type("@jakarta.validation.constraints.NotNull(message=\"\") String name;")),
                new RuleCase("MatchXpath#annotationText", ERROR, type("@RateLimit(key=\"users\") void run() {}"),
                        type("@RateLimit void run() {}")),
                new RuleCase("MatchXpath#annotationText", ERROR, "@lombok.extern.java.Log class Example {}",
                        type("@Log(\"\") void run() {}")),
                new RuleCase("MatchXpath#schemaDefaultExample", ERROR,
                        "class PageDTO { @Schema(description=\"页码\",example=\"-1\") int page=-1; }",
                        "class PageDTO { @Schema(description=\"页码\") int page=-1; }"),
                new RuleCase("MatchXpath#logDesensitize", ERROR,
                        type("@Log(value=\"登录\",params={\"#user.password\"},desensitize=true) void run() {}"),
                        type("@Log(value=\"登录\",params=\"#user.\"+\"pass\"+\"word\",desensitize=false) void run() {}")),
                new RuleCase("MatchXpath#controllerOperation", ERROR,
                        "import other.GetMapping; @RestController class Example { @GetMapping void run() {} }",
                        "@RestController class Example { @org.springframework.web.bind.annotation.GetMapping "
                                + "void run() {} }"),
                new RuleCase("MatchXpath#controllerApiPrefix", ERROR,
                        "@RestController @RequestMapping(\"/users\") class Example {}",
                        "@RestController @RequestMapping(\"/\"+\"api\"+\"/users\") class Example {}"),
                new RuleCase("MatchXpath#controllerPath", ERROR,
                        "@RestController @RequestMapping({\"/users\",\"/user-profiles/{id}\"}) class Example {}",
                        "@RestController @RequestMapping({\"/users\",\"/User_List\"}) class Example {}"),
                new RuleCase("MatchXpath#serviceHttp", ERROR,
                        "import sample.HttpServletRequest; @Service class Example { HttpServletRequest run() "
                                + "{ return null; } }",
                        "@Service class Example { void run(jakarta.servlet.http.HttpServletRequest request) {} }"),
                new RuleCase("MatchXpath#layerEntityReturn", ERROR,
                        "@Service class Example { void run(UserEntity user) {} }",
                        "import sample.entity.User; @Service class Example { User run() { return null; } }"),
                new RuleCase("MatchXpath#loggingPlaceholder", ERROR,
                        "@Slf4j class Example { void run() { log.info(\"创建\"+(\"新\"+\"用户\")); } }",
                        "@Slf4j class Example { void run(Object user) { log.info(\"用户\"+user); } }")));
        Map<String, String> suffixes = Map.ofEntries(
                Map.entry("controller", "Controller"), Map.entry("dto", "DTO"), Map.entry("vo", "VO"),
                Map.entry("mapper", "Mapper"), Map.entry("converter", "Converter"), Map.entry("convert", "Converter"),
                Map.entry("config", "Config"), Map.entry("exception", "Exception"),
                Map.entry("util", "Utils"), Map.entry("utils", "Utils"),
                Map.entry("constant", "Constants"), Map.entry("constants", "Constants"));
        for (Map.Entry<String, String> role : suffixes.entrySet()) {
            cases.add(new RuleCase("MatchXpath#projectTypeSuffix", WARNING,
                    "package sample." + role.getKey() + "; class Example" + role.getValue() + " {}",
                    "package sample." + role.getKey() + "; class Example {}"));
        }
        cases.addAll(List.of(
                new RuleCase("MatchXpath#projectTypeSuffix", WARNING,
                        "package sample.service; interface ExampleService {}",
                        "package sample.service; interface Example {}"),
                new RuleCase("MatchXpath#projectTypeSuffix", WARNING,
                        "package sample.service.impl; class ExampleServiceImpl {}",
                        "package sample.service.impl; class Example {}"),
                new RuleCase("JavadocMethod", ERROR,
                        type("/**\n * 检查输入\n *\n * @throws IllegalArgumentException 输入不合法\n */\n"
                                + "public void run() { throw new IllegalArgumentException(); }"),
                        type("/**\n * 检查输入\n *\n * @param missing 不存在的参数\n */ public void run() {}")),
                new RuleCase("JavadocType", WARNING,
                        "/**\n * 泛型类型\n *\n * @author example\n * @param <T> 数据类型\n */\nclass Example<T> {}",
                        "/**\n * 泛型类型\n *\n * @author example\n */\nclass Example<T> {}"),
                new RuleCase("MatchXpath#annotationText", ERROR,
                        type("@javax.validation.constraints.NotNull(message=\"名称必填\") String name;"),
                        type("@javax.validation.constraints.NotNull String name;")),
                new RuleCase("MatchXpath#annotationText", ERROR, type("@Log(\"创建用户\") void run() {}"),
                        type("@Log(value=\"\") void run() {}")),
                new RuleCase("MatchXpath#aspectContract", ERROR, "@Aspect @Component @Order(1) class Example {}",
                        "@Aspect @Component class Example {}"),
                new RuleCase("MatchXpath#aspectContract", ERROR, "@Aspect @Component @Order(1) class Example {}",
                        "@Aspect @Order(1) class Example {}"),
                new RuleCase("MatchXpath#roleInstanceName", WARNING, type("static UserService service;"),
                        type("UserService service;")),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR, type("static String name;"), type("String name;")),
                new RuleCase("MatchXpath#fieldSchema", ERROR,
                        "package sample.dto; class Example { @Schema String name; }",
                        "package sample.dto; class Example { String name; }"),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING, "/** 类型说明。 */ class Example {}",
                        type("/** 方法说明。 */ void run() {}")),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING, type("/** <!-- 文本。 --> */ void run() {}"),
                        type("/**\r\n * 方法说明。\r\n */ void run() {}")),
                new RuleCase("MatchXpath#methodJavadocNumbering", WARNING,
                        type("/** {@code 1. value} */ void run() {}"),
                        type("/** 2) 返回数据 */ void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR,
                        type("@Operation(summary=\"\\u200b\") void run() {}"),
                        type("@Operation(summary=\"\\u3000\") void run() {}")),
                new RuleCase("MatchXpath#operationSummary", ERROR, type("@Operation(summary=\"查询\") void run() {}"),
                        type("@Operation(summary={\"查询\"}) void run() {}")),
                new RuleCase("MatchXpath#requestParamDefault", ERROR,
                        "import other.RequestParam; class Example { void run(@RequestParam(required=true) "
                                + "String name) {} }",
                        type("void run(@org.springframework.web.bind.annotation.RequestParam(required=true) "
                                + "String name) {}")),
                new RuleCase("MatchXpath#controllerPath", ERROR,
                        "@RestController @RequestMapping(\"${api.path}/*\") class Example {}",
                        "@RestController @RequestMapping(\"/user_list\") class Example {}"),
                new RuleCase("MatchXpath#controllerJavadoc", ERROR,
                        "@RestController class Example { /** 辅助说明 */ void run() {} }",
                        "@RestController class Example { /** 接口说明 */ @GetMapping @Operation void run() {} }"),
                new RuleCase("MatchXpath#layerEntityReturn", ERROR,
                        "@Service class Example { Result<java.util.List<UserDTO>> run() { return null; } }",
                        "@Service class Example { Result<java.util.List<UserEntity>> run() { return null; } }"),
                new RuleCase("MatchXpath#beanUtilsCall", ERROR,
                        method("sample.BeanUtils.copyProperties(first, second);"),
                        method("org.apache.commons.beanutils.BeanUtils.copyProperties(first, second);")),
                new RuleCase("MatchXpath#loggingPlaceholder", ERROR,
                        "import other.Slf4j; @Slf4j class Example { void run(Object user) { log.info(\"用户\"+user); } }",
                        "@lombok.extern.slf4j.Slf4j class Example { void run(Object user) { "
                                + "log.info(\"用户\"+user); } }"),
                new RuleCase("MatchXpath#converterFinal", WARNING, "@Component class ExampleConverter {}",
                        "class ExampleConverter {}"),
                new RuleCase("MatchXpath#converterConstructor", ERROR, "@Component class ExampleConverter {}",
                        "class ExampleConverter {}")));
        cases.addAll(List.of(
                new RuleCase("MatchXpath#testMethodName", WARNING, type("@other.Test void run() {}"),
                        type("@org.junit.jupiter.api.Test void run() {}")),
                new RuleCase("MatchXpath#testMethodName", WARNING, type("@Marker(Test.class) void run() {}"),
                        type("@org.junit.Test void run() {}")),
                new RuleCase("MatchXpath#converterFinal", WARNING, "@Component class ExampleConverter {}",
                        "@other.Component class ExampleConverter {}"),
                new RuleCase("MatchXpath#converterConstructor", ERROR, "@Component class ExampleConverter {}",
                        "@Marker(Component.class) class ExampleConverter {}"),
                new RuleCase("MatchXpath#aspectContract", ERROR,
                        "@Aspect @Component @org.springframework.core.annotation.Order(1) class Example {}",
                        "@Aspect @Component @other.Order(1) class Example {}"),
                new RuleCase("MatchXpath#schemaDefaultExample", ERROR,
                        "class ExampleDTO { @Schema(description=\"默认值\") static final int DEFAULT = 1; }",
                        "class ExampleDTO { @Schema(description=\"默认值\") int count = 1; }"),
                new RuleCase("MatchXpath#controllerMapper", ERROR,
                        "@RestController class Example { UserService[] services; }",
                        "@RestController class Example { UserMapper[] mappers; }"),
                new RuleCase("MatchXpath#serviceHttp", ERROR, "@Service class Example { void run(String[] values) {} }",
                        "@Service class Example { void run(jakarta.servlet.http.HttpServletRequest[] requests) {} }"),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR, type("/**\n * 名称\n */ String name;"),
                        type("/**\n *\n */ String name;")),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR, type("/**\n * <p>名称</p>\n */ String name;"),
                        type("/**\n * <p></p>\n */ String name;")),
                new RuleCase("MatchXpath#fieldJavadoc", ERROR, type("/** 旧文档 */ /**\n * 名称\n */ String name;"),
                        type("/**\n * 旧文档\n */ /** 名称 */ String name;")),
                new RuleCase("MatchXpath#typeSummary", WARNING, "/** <p>类型说明</p> */ class Example {}",
                        "/** <p></p> */ class Example {}"),
                new RuleCase("MatchXpath#typeSummary", WARNING, "/** 旧说明 */ /** 新说明 */ class Example {}",
                        "/** 旧说明 */ /** <!-- 无可见说明 --> */ class Example {}"),
                new RuleCase("MatchXpath#methodJavadocPeriod", WARNING, type("/** 旧说明。 */ /** 新说明 */ void run() {}"),
                        type("/** 旧说明 */ /** 新说明。 */ void run() {}")),
                new RuleCase("JavadocMethod", ERROR, "import java.io.*; " + type("/**\n * 执行\n *\n"
                        + " * @throws IOException 输入输出异常\n */ public void run() throws IOException "
                        + "{ throw new FileNotFoundException(); }"),
                        type("/**\n * 执行\n *\n * @param missing 不存在的参数\n */ public void run() {}")),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("@ParameterizedTest public void "
                        + "run_WhenReady_ReturnsValue() {}"), "public " + type("public void run() {}")),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("@RepeatedTest(2) public void "
                        + "run_WhenReady_ReturnsValue() {}"), "public " + type("public void run() {}")),
                new RuleCase("MissingJavadocMethod", WARNING, "public " + type("@TestFactory public void run() {}"),
                        "public " + type("public void run() {}")),
                new RuleCase("LineLength", INFO, type("// \"\"\"\n// short"),
                        type("// \"\"\"\n// " + "x".repeat(130))),
                new RuleCase("LineLength", INFO, type("String text = (\"\"\"\nshort\n\"\"\"\n);\n// short"),
                        type("String text = (\"\"\"\nshort\n\"\"\"\n);\n// " + "x".repeat(130)))));
        cases.addAll(List.of(
                new RuleCase("MatchXpath#fieldBooleanPrefix", ERROR,
                        "import other.Boolean; class ExampleDTO { @Schema Boolean isActive; }",
                        "class ExampleDTO { @Schema boolean isActive; }"),
                new RuleCase("MatchXpath#fieldBooleanPrefix", ERROR,
                        "record ExampleDTO(@Schema java.lang.Boolean active) {}",
                        "record ExampleDTO(@Schema java.lang.Boolean isActive) {}"),
                new RuleCase("MatchXpath#layerEntityReturn", ERROR,
                        "@Service class Example { java.util.List<? extends UserDTO> run() { return null; } }",
                        "@Service class Example { java.util.List<? extends UserEntity> run() { return null; } }"),
                new RuleCase("MatchXpath#controllerMapper", ERROR,
                        "@RestController class Example { java.util.List<? extends UserService> services; }",
                        "@RestController class Example { java.util.List<? extends UserMapper> mappers; }"),
                new RuleCase("MatchXpath#serviceHttp", ERROR, "@Service class Example<T extends Runnable> {}",
                        "@Service class Example<T extends jakarta.servlet.ServletRequest> {}")));
        cases.add(new RuleCase("MethodName#productionMethodName", WARNING,
                type("@TestFactory void createTests() {}"),
                type("@TestFactory void create_tests() {}")));
        cases.addAll(List.of(
                new RuleCase("MissingJavadocMethod", WARNING,
                        "public " + type("@io.swagger.v3.oas.annotations.Operation(summary=\"查询\") "
                                + "public void run() {}"),
                        "public " + type("public void run() {}")),
                new RuleCase("MissingJavadocMethod", WARNING,
                        "public " + type("@org.junit.jupiter.api.Test public void run_WhenReady_ReturnsValue() {}"),
                        "public " + type("public void run() {}")),
                new RuleCase("JavadocMethod", ERROR,
                        type("/** 覆写说明 */ @java.lang.Override public String toString() { return \"example\"; }"),
                        type("/** 覆写说明 */ public String toString() { return \"example\"; }"))));
        return cases;
    }

}
