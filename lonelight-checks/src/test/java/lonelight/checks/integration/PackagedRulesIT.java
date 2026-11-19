package lonelight.checks.integration;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import lonelight.checks.JavaSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证规则 JAR 能独立加载并运行违规样例
 *
 * @author lonelight
 */
class PackagedRulesIT {
    private static final String TYPE_SUFFIX = "lonelight.checks.naming.TypeSuffixCheck";

    @TempDir
    Path directory;

    /**
     * 验证构建产物能独立加载扩展并运行两份配置
     */
    @Test
    void builtJarLoadsMessagesChecksFiltersAndBothXmlProfiles() throws Exception {
        Path jar = Path.of("target/lonelight-checks-0.1.0.jar").toAbsolutePath();
        try (JarFile archive = new JarFile(jar.toFile())) {
            assertEquals("0.1.0", archive.getManifest().getMainAttributes().getValue("Implementation-Version"));
            assertNotNull(archive.getJarEntry("lonelight/checks/messages.properties"));
            assertTrue(archive.stream().noneMatch(entry -> entry.getName().startsWith("com/puppycrawl/")));
        }
        // 检查器实现必须从打包产物加载，避免误用工作区编译结果
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, getClass().getClassLoader()) {
            /**
             * 优先从构建产物读取规则资源
             *
             * @param name 资源路径
             * @return 资源地址，找不到时为 null
             */
            @Override
            public URL getResource(String name) {
                return name.startsWith("lonelight/checks/") ? findResource(name) : super.getResource(name);
            }

            /**
             * 从构建产物加载规则类以避免回落到编译目录
             *
             * @param name 类的全限定名
             * @param resolve 是否链接已加载的类
             * @return 已加载的类
             * @throws ClassNotFoundException 找不到请求的类时
             */
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("lonelight.checks.") && !name.endsWith("CheckTestSupport")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> type = findLoadedClass(name);
                        if (type == null) {
                            type = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(type);
                        }
                        return type;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> check = loader.loadClass("lonelight.checks.naming.VariableTypeNameCheck");
            assertEquals(jar.toUri().toURL(), check.getProtectionDomain().getCodeSource().getLocation());
            Path file = CheckTestSupport.write(directory, "Example.java", "package sample; public class Example { UserDTO request; }\n");
            for (String profile : List.of("lonelight-base", "lonelight-project")) {
                var events = CheckTestSupport.scan(CheckTestSupport.load(profile), List.of(file), loader);
                assertFalse(events.isEmpty());
                assertTrue(events.stream().noneMatch(event -> event.getMessage().contains("Unable to")));
                if (profile.equals("lonelight-project")) {
                    assertTrue(events.stream().anyMatch(event -> event.getMessage().contains("变量名")));
                }
            }
            CheckTestSupport.write(directory, "Result.java", "package sample; public record Result(String message) {}");
            Path controller = CheckTestSupport.write(directory, "Web.java", """
                    package sample;
                    import org.springframework.web.bind.annotation.GetMapping;
                    import org.springframework.web.bind.annotation.RestController;
                    @RestController class Web {
                        @GetMapping Result read() { return null; }
                    }
                    """);
            var apiEvents = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"), List.of(controller), loader)
                    .stream().filter(event -> CheckTestSupport.matches(event, "ControllerApiDocumentation")).toList();
            assertEquals(2, apiEvents.size());
            assertTrue(apiEvents.stream().anyMatch(event -> event.getMessage().contains("@Operation")));
            assertTrue(apiEvents.stream().anyMatch(event -> event.getMessage().contains("sample.Result 的字段 message")));
            var properties = new Properties();
            properties.setProperty("lonelight.typeSuffixMappings", "optional\\..*=DTO|Request");
            for (String profile : List.of("base", "project")) {
                var configuration = CheckTestSupport.load("lonelight-" + profile, properties);
                var examples = FixtureCatalog.load(profile);
                var events = CheckTestSupport.scan(configuration, examples.files(), loader);
                var rules = CheckTestSupport.rules(configuration);
                var expectedMarkers = rules.keySet().stream().map(JavaSource::simpleName).collect(Collectors.toSet());
                assertEquals(expectedMarkers, examples.markedFiles().keySet(), profile);
                rules.forEach((name, rule) -> verifyFinding(events, examples, name, rule));
                if (profile.equals("project")) {
                    assertEquals(1, events.stream().filter(event -> CheckTestSupport.matches(event, TYPE_SUFFIX)).count());
                    var defaultEvents = CheckTestSupport.scan(CheckTestSupport.load("lonelight-project"),
                            examples.filesFor(TYPE_SUFFIX), loader);
                    assertTrue(defaultEvents.stream().noneMatch(event -> CheckTestSupport.matches(event, TYPE_SUFFIX)));
                    var messages = events.stream()
                            .filter(event -> CheckTestSupport.matches(event, "ControllerApiDocumentation"))
                            .map(AuditEvent::getMessage).toList();
                    for (String needle : List.of("@Operation", "@Parameter", "OrderInput 的字段 comment",
                            "OrderSnapshot 的字段 orderId")) {
                        assertTrue(messages.stream().anyMatch(message -> message.contains(needle)), needle);
                    }
                }
            }
        }
    }

    /**
     * 断言指定样例目录存在目标规则的预期诊断
     *
     * @param events 扫描事件列表
     * @param examples 该配置的主题样例索引
     * @param ruleName 目标规则名称和可选标识
     * @param rule 包含预期等级的规则配置
     */
    private static void verifyFinding(List<AuditEvent> events, FixtureCatalog examples, String ruleName,
                                      CheckTestSupport.LocatedRule rule) {
        var expectedFiles = examples.filesFor(ruleName);
        assertFalse(expectedFiles.isEmpty(), ruleName);
        String severity = severity(rule);
        for (Path file : expectedFiles) {
            assertTrue(events.stream().anyMatch(event -> event.getFileName().equals(file.toString())
                    && CheckTestSupport.matches(event, ruleName)
                    && event.getSeverityLevel().getName().equals(severity)), ruleName + " " + file);
        }
    }

    /**
     * 读取规则配置的严重等级
     *
     * @param rule 待检查的规则配置
     * @return 严重等级名称
     */
    private static String severity(CheckTestSupport.LocatedRule rule) {
        try {
            return rule.configuration().getProperty("severity");
        } catch (com.puppycrawl.tools.checkstyle.api.CheckstyleException exception) {
            throw new AssertionError(exception);
        }
    }
}
