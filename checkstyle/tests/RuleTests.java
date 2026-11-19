package tests;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static tests.RuleCase.method;
import static tests.RuleCase.type;

/**
 * 逐项验证 XML 中的检查、过滤器和完整配置
 *
 * @author example
 */
// 测试用例保留边界数值与重复文本，便于逐项阅读
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:MultipleStringLiterals"})
public final class RuleTests {

    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([a-zA-Z0-9_.]+)\\s*;");

    /**
     * 从 XML 加载的完整配置
     */
    private final Configuration configuration;

    /**
     * 规则与展示样例所在目录
     */
    private final Path directory;

    /**
     * 测试片段的临时目录
     */
    private final Path temporaryDirectory;

    /**
     * 实际启用的检查模块
     */
    private final Map<String, Rule> rules = new LinkedHashMap<>();

    /**
     * XML 中的辅助模块
     */
    private final Set<String> filters = new LinkedHashSet<>();

    /**
     * 已验证行为的辅助模块
     */
    private final Set<String> coveredFilters = new LinkedHashSet<>();

    /**
     * 本次运行的断言失败明细
     */
    private final List<String> failures = new ArrayList<>();

    /**
     * 已执行的断言数量
     */
    private int assertions;

    /**
     * 临时源码编号
     */
    private int fileNumber;

    private RuleTests(Path directory, Path temporaryDirectory) throws Exception {
        this.directory = directory;
        this.temporaryDirectory = temporaryDirectory;
        configuration = ConfigurationLoader.loadConfiguration(directory.resolve("checkstyle.xml").toString(),
                new PropertiesExpander(new Properties()));
        collect(configuration, true);
    }

    /**
     * 在规则目录运行全部检查测试
     *
     * @param arguments 可选的规则目录，默认使用当前目录
     * @throws IllegalArgumentException 参数个数超过一个
     * @throws Exception 配置、源码解析或断言失败
     */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length > 1) {
            throw new IllegalArgumentException("用法为 RuleTests [规则目录]");
        }
        Path directory = Path.of(arguments.length == 0 ? "." : arguments[0]).toAbsolutePath().normalize();
        Path temporaryDirectory = Files.createTempDirectory("checkstyle-rule-tests-");
        try {
            new RuleTests(directory, temporaryDirectory).run();
        } finally {
            try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private void run() throws Exception {
        List<RuleCase> cases = new ArrayList<>(NativeRuleCases.cases());
        cases.addAll(ProjectRuleCases.cases());
        cases.addAll(BoundaryRuleCases.cases());
        Map<String, SeverityLevel> expectedLevels = new LinkedHashMap<>();
        for (RuleCase test : cases) {
            SeverityLevel previous = expectedLevels.putIfAbsent(test.key(), test.expectedSeverity());
            require(previous == null || previous == test.expectedSeverity(), test.key() + " 的用例预期等级不一致");
        }
        require(expectedLevels.keySet().equals(rules.keySet()), "检查模块与测试清单不一致，缺失 "
                + difference(rules.keySet(), expectedLevels.keySet()) + "，多余 "
                + difference(expectedLevels.keySet(), rules.keySet()));
        for (Rule rule : rules.values()) {
            SeverityLevel expected = expectedLevels.get(rule.key());
            require(expected != null && expected.getName().equals(rule.configuration().getProperty("severity")),
                    rule.key() + " 的 XML 等级不符合独立测试预期 " + expected);
        }

        List<PreparedCase> prepared = new ArrayList<>();
        for (RuleCase test : cases) {
            Rule rule = rules.get(test.key());
            if (rule == null) {
                continue;
            }
            Path accepted = write("accepted", test.filename(), test.accepted());
            Path rejected = write("rejected", test.filename(), test.rejected());
            List<AuditEvent> events = scan(isolated(rule), List.of(accepted, rejected));
            List<AuditEvent> validEvents = eventsFor(events, accepted);
            List<AuditEvent> invalidEvents = eventsFor(events, rejected);
            require(validEvents.isEmpty(), test.key() + " 合规用例误报 " + messages(validEvents)
                    + "\n" + test.accepted());
            require(!invalidEvents.isEmpty(), test.key() + " 违规用例未触发\n" + test.rejected());
            for (AuditEvent event : invalidEvents) {
                require(matches(event, rule), test.key() + " 触发了其他诊断 " + event.getSourceName());
                require(event.getSeverityLevel() == test.expectedSeverity(), test.key() + " 等级不符合独立测试预期");
            }
            prepared.add(new PreparedCase(rule, accepted, rejected));
        }

        List<File> files = prepared.stream().flatMap(test -> Stream.of(test.accepted(), test.rejected()))
                .map(Path::toFile).toList();
        List<AuditEvent> integrated = scanFiles(configuration, files);
        for (PreparedCase test : prepared) {
            require(eventsFor(integrated, test.accepted()).stream().noneMatch(event -> matches(event, test.rule())),
                    test.rule().key() + " 在完整配置中误报合规用例");
            List<AuditEvent> rejected = eventsFor(integrated, test.rejected()).stream()
                    .filter(event -> matches(event, test.rule())).toList();
            require(!rejected.isEmpty(), test.rule().key() + " 在完整配置中未触发违规用例");
            require(rejected.stream().allMatch(event -> event.getSeverityLevel()
                            == expectedLevels.get(test.rule().key())),
                    test.rule().key() + " 在完整配置中的等级不符合独立测试预期");
        }

        verifyFilters();
        verifyExamples(expectedLevels);
        verifyProjectSources();
        verifyXml();
        verifyReadme();
        require(filters.equals(coveredFilters), "辅助模块测试不完整，缺失 "
                + difference(filters, coveredFilters) + "，多余 " + difference(coveredFilters, filters));
        if (!failures.isEmpty()) {
            failures.forEach(message -> System.err.println("失败 " + message));
            throw new AssertionError(failures.size() + " 项断言失败");
        }
        System.out.println("通过 " + rules.size() + " 个检查模块、" + filters.size() + " 个辅助模块、"
                + cases.size() + " 组正反例，共 " + assertions + " 项断言");
    }

    private void collect(Configuration parent, boolean fileLevel) throws Exception {
        for (Configuration child : parent.getChildren()) {
            if ("TreeWalker".equals(child.getName())) {
                collect(child, false);
            } else if (child.getName().endsWith("Filter") || child.getName().endsWith("Holder")) {
                filters.add(key(child));
            } else {
                String key = key(child);
                require(!rules.containsKey(key), "检查模块标识重复 " + key);
                rules.put(key, new Rule(key, child, fileLevel));
            }
        }
    }

    private static String key(Configuration module) throws Exception {
        List<String> properties = Arrays.asList(module.getPropertyNames());
        if (properties.contains("id")) {
            return module.getName() + "#" + module.getProperty("id");
        }
        if (properties.contains("checks")) {
            return module.getName() + "#" + module.getProperty("checks");
        }
        if ("SuppressionCommentFilter".equals(module.getName())) {
            return module.getName() + (properties.contains("idFormat") ? "#idFormat" : "#checkFormat");
        }
        return module.getName();
    }

    private static Configuration isolated(Rule rule) {
        DefaultConfiguration checker = new DefaultConfiguration("Checker");
        checker.addProperty("charset", "UTF-8");
        if (rule.fileLevel()) {
            checker.addChild(rule.configuration());
        } else {
            DefaultConfiguration treeWalker = new DefaultConfiguration("TreeWalker");
            treeWalker.addChild(rule.configuration());
            checker.addChild(treeWalker);
        }
        return checker;
    }

    private Path write(String folder, String filename, String source) throws Exception {
        Path base = temporaryDirectory.resolve("case-" + fileNumber++).resolve(folder);
        var declaration = PACKAGE.matcher(source);
        Path location = declaration.find() ? base.resolve(declaration.group(1).replace('.', '/')) : base;
        Files.createDirectories(location);
        Path file = location.resolve(filename).toAbsolutePath();
        Files.writeString(file, source);
        return file;
    }

    private static List<AuditEvent> scan(Configuration configuration, List<Path> files) throws Exception {
        return scanFiles(configuration, files.stream().map(Path::toFile).toList());
    }

    private static List<AuditEvent> scanFiles(Configuration configuration, List<File> files) throws Exception {
        List<AuditEvent> events = new ArrayList<>();
        Checker checker = new Checker();
        checker.setModuleClassLoader(RuleTests.class.getClassLoader());
        checker.addListener(new AuditListener() {
            @Override
            public void auditStarted(AuditEvent event) {}

            @Override
            public void auditFinished(AuditEvent event) {}

            @Override
            public void fileStarted(AuditEvent event) {}

            @Override
            public void fileFinished(AuditEvent event) {}

            @Override
            public void addError(AuditEvent event) {
                events.add(event);
            }

            @Override
            public void addException(AuditEvent event, Throwable failure) {
                throw new AssertionError(failure);
            }
        });
        try {
            checker.configure(configuration);
            checker.process(files);
        } finally {
            checker.destroy();
        }
        return events;
    }

    private static List<AuditEvent> eventsFor(List<AuditEvent> events, Path file) {
        return events.stream().filter(event -> file.toString().equals(event.getFileName())).toList();
    }

    private static boolean matches(AuditEvent event, Rule rule) {
        return diagnosticKey(event).equals(rule.key());
    }

    private static String diagnosticKey(AuditEvent event) {
        String source = event.getSourceName();
        String name = source.substring(source.lastIndexOf('.') + 1);
        if (name.endsWith("Check")) {
            name = name.substring(0, name.length() - "Check".length());
        }
        String identifier = event.getModuleId();
        return identifier == null ? name : name + '#' + identifier;
    }

    private static String messages(List<AuditEvent> events) {
        return events.stream().map(event -> event.getLine() + " " + event.getMessage()).toList().toString();
    }

    private static Set<String> difference(Set<String> first, Set<String> second) {
        Set<String> result = new LinkedHashSet<>(first);
        result.removeAll(second);
        return result;
    }

    private void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            failures.add(message);
        }
    }

    private void verifyFilters() throws Exception {
        verifyFilter("SuppressWarningsFilter", "MagicNumber",
                write("main", "Example.java", type("@SuppressWarnings(\"checkstyle:MagicNumber\") int run() { "
                        + "return 42; }")),
                write("main", "Example.java", type("int run() { return 42; }")));
        coveredFilters.add("SuppressWarningsHolder");

        verifyFilter("SuppressionSingleFilter#MagicNumber|MultipleStringLiterals", "MagicNumber",
                write("src/test/java", "Example.java", type("int run() { return 42; }")),
                write("src/main/java", "Example.java", type("int run() { return 42; }")));
        verifyFilter("SuppressionSingleFilter#MagicNumber|MultipleStringLiterals", "MultipleStringLiterals",
                write("src/test/java", "Example.java", method("run(\"duplicate\"); run(\"duplicate\");")),
                write("src/main/java", "Example.java", method("run(\"duplicate\"); run(\"duplicate\");")));

        verifyFilter("SuppressionSingleFilter#fieldJavadoc", "MatchXpath#fieldJavadoc",
                write("src/test/java", "Example.java", type("String name;")),
                write("src/main/java", "Example.java", type("String name;")));
        verifyFilter("SuppressionSingleFilter#fieldSchema", "MatchXpath#fieldSchema",
                write("src/test/java", "Example.java", "class ExampleDTO { String name; }"),
                write("src/main/java", "Example.java", "class ExampleDTO { String name; }"));
        verifyFilter("SuppressionSingleFilter#fieldBooleanPrefix", "MatchXpath#fieldBooleanPrefix",
                write("src/test/java", "Example.java", "class ExampleDTO { @Schema boolean isActive; }"),
                write("src/main/java", "Example.java", "class ExampleDTO { @Schema boolean isActive; }"));

        verifyFilter("BeforeExecutionExclusionFileFilter", "MissingJavadocType",
                write("target/generated-sources", "Example.java", type("void run() {}")),
                write("src/main/java", "Example.java", "package sample.build; class Example {}"));
        verifyFilter("SuppressWithPlainTextCommentFilter", "LineLength",
                write("main", "Example.java", type("// CHECKSTYLE.OFF: LineLength\nString sql = \"\"\"\n"
                        + "x".repeat(130) + "\n\"\"\";\n// CHECKSTYLE.ON: LineLength")),
                write("main", "Example.java", type("String value = \"" + "x".repeat(130) + "\";")));

        verifyFilter("SuppressionXpathSingleFilter#productionMethodName", "MethodName#productionMethodName",
                write("main", "Example.java", type("@Test void run_WhenReady_ReturnsValue() {}")),
                write("main", "Example.java", type("void run_WhenReady_ReturnsValue() {}")));
        verifyFilter("SuppressionXpathSingleFilter#productionMethodName", "MethodName#productionMethodName",
                write("main", "Example.java", type("@org.junit.jupiter.api.Test void run_WhenReady_ReturnsValue() {}")),
                write("main", "Example.java", type("@other.Test void run_WhenReady_ReturnsValue() {}")));
        verifyFilter("SuppressionXpathSingleFilter#productionMethodName", "MethodName#productionMethodName",
                write("main", "Example.java", type("@Test void run_WhenReady_ReturnsValue() {}")),
                write("main", "Example.java", type("@Marker(Test.class) void run_WhenReady_ReturnsValue() {}")));
        verifyFilter("SuppressionXpathSingleFilter#ImportOrder", "ImportOrder",
                write("main", "Example.java", "import javax.sql.DataSource;\nimport java.util.List;\nclass Example {}"),
                write("main", "Example.java", "import org.example.Other;\nimport java.util.List;\nclass Example {}"));

        verifyFilter("SuppressionXpathSingleFilter#FinalClass|HideUtilityClassConstructor", "FinalClass",
                write("main", "Example.java", "class ExampleConverter { private ExampleConverter() {} public "
                        + "static void run() {} }"),
                write("main", "Example.java", "class Example { private Example() {} public static void run() {} }"));
        verifyFilter("SuppressionXpathSingleFilter#FinalClass|HideUtilityClassConstructor",
                "HideUtilityClassConstructor",
                write("main", "Example.java", "class ExampleConverter { public static void run() {} }"),
                write("main", "Example.java", "class Example { public static void run() {} }"));
        verifyFilter("SuppressionXpathSingleFilter#UnusedPrivateField", "UnusedPrivateField",
                write("main", "Example.java", "@Data class Example { private String name; }"),
                write("main", "Example.java", "class Example { private String name; }"));
        verifyFilter("SuppressionXpathSingleFilter#UnusedPrivateField", "UnusedPrivateField",
                write("main", "Example.java", type("@lombok.Getter private String name;")),
                write("main", "Example.java", "import other.Getter; " + type("@Getter private String name;")));
        verifyFilter("SuppressionXpathSingleFilter#UnusedPrivateField", "UnusedPrivateField",
                write("main", "Example.java", "@lombok.Data class Example { private String name; }"),
                write("main", "Example.java", "@other.Data class Example { private String name; }"));
        verifyFilter("SuppressionXpathSingleFilter#UnusedPrivateField", "UnusedPrivateField",
                write("main", "Example.java", type("@SuppressWarnings(\"unused\") private String name;")),
                write("main", "Example.java", type("@SuppressWarnings(\"unchecked\") private String name;")));
        verifyFilter("SuppressionXpathSingleFilter#UnusedPrivateField", "UnusedPrivateField",
                write("main", "Example.java", type("@java.lang.SuppressWarnings({\"unchecked\", \"unused\"}) "
                        + "private String name;")),
                write("main", "Example.java", type("@other.SuppressWarnings(\"unused\") private String name;")));
        verifyFilter("SuppressWarningsFilter", "UnusedPrivateField",
                write("main", "Example.java", type("@SuppressWarnings(\"checkstyle:UnusedPrivateField\") "
                        + "private String name;")),
                write("main", "Example.java", type("@SuppressWarnings(\"unchecked\") private String name;")));

        verifyFilter("SuppressionCommentFilter#checkFormat", "MagicNumber",
                write("main", "Example.java", type("// CHECKSTYLE.OFF: MagicNumber\nint run() { return 42; "
                        + "}\n// CHECKSTYLE.ON: MagicNumber")),
                write("main", "Example.java", type("int run() { return 42; }")));
        verifyFilter("SuppressionCommentFilter#idFormat", "MatchXpath#operationSummary",
                write("main", "Example.java", type("// CHECKSTYLE.OFF: operationSummary\n@Operation void "
                        + "run() {}\n// CHECKSTYLE.ON: operationSummary")),
                write("main", "Example.java", type("@Operation void run() {}")));

        Path restored = write("main", "Example.java", type("""
                // CHECKSTYLE.OFF: operationSummary
                @Operation void first() {}
                // CHECKSTYLE.ON: operationSummary
                @Operation void second() {}
                """));
        List<AuditEvent> events = scan(configuration, List.of(restored));
        require(events.stream().filter(event -> matches(event, rules.get("MatchXpath#operationSummary"))).count() == 1,
                "CHECKSTYLE.ON 未恢复指定模块");
        Path widthRestored = write("main", "Example.java", type("// CHECKSTYLE.OFF: LineLength\n// "
                + "x".repeat(130) + "\n// CHECKSTYLE.ON: LineLength\n// " + "x".repeat(130)));
        require(scan(configuration, List.of(widthRestored)).stream()
                .filter(event -> matches(event, rules.get("LineLength"))).count() == 1,
                "CHECKSTYLE.ON 未恢复行宽建议");
    }

    private void verifyFilter(String filter, String target, Path accepted, Path rejected) throws Exception {
        List<AuditEvent> events = scan(configuration, List.of(accepted, rejected));
        Rule rule = rules.get(target);
        require(eventsFor(events, accepted).stream().noneMatch(event -> matches(event, rule)), filter + " 豁免未生效");
        require(eventsFor(events, rejected).stream().anyMatch(event -> matches(event, rule)), filter + " 豁免范围过宽");
        coveredFilters.add(filter);
    }

    private void verifyExamples(Map<String, SeverityLevel> expectedLevels) throws Exception {
        Map<String, List<String>> examples = Map.of(
                "GoodExample.java", List.of(), "GoodConverter.java", List.of(),
                "BadConverter.java", List.of("MatchXpath#converterFinal", "MatchXpath#converterConstructor"),
                "BadExample.java", List.of("UnusedPrivateField", "MatchXpath#fieldJavadoc",
                        "MethodName#productionMethodName", "MissingJavadocMethod", "StringLiteralEquality",
                        "MatchXpath#methodJavadocPeriod", "MatchXpath#methodJavadocNumbering", "JavadocMethod",
                        "NeedBraces", "MagicNumber", "EmptyCatchBlock", "MissingSwitchDefault"));
        for (Map.Entry<String, List<String>> example : examples.entrySet()) {
            Path file = directory.resolve("examples").resolve(example.getKey());
            List<AuditEvent> events = scan(configuration, List.of(file));
            List<String> actual = events.stream().map(RuleTests::diagnosticKey).sorted().toList();
            require(actual.equals(example.getValue().stream().sorted().toList()),
                    example.getKey() + " 示例诊断不符合预期 " + actual);
            require(events.stream().allMatch(event -> event.getSeverityLevel()
                    == expectedLevels.get(diagnosticKey(event))), example.getKey() + " 示例诊断等级不符合预期");
        }
    }

    private void verifyProjectSources() throws Exception {
        for (Map.Entry<String, String> source : ProjectRuleCases.validSources().entrySet()) {
            Path file = write("src/main/java", source.getKey(), source.getValue());
            List<AuditEvent> events = scan(configuration, List.of(file));
            require(events.isEmpty(), source.getKey() + " 完整项目样例产生额外诊断 " + messages(events));
        }
    }

    private void verifyXml() throws Exception {
        String xml = Files.readString(directory.resolve("checkstyle.xml"));
        require("14.4.0".equals(Checker.class.getPackage().getImplementationVersion()), "测试需要标准 Checkstyle 14.4.0");
        require(xml.contains("Java 代码规范 0.1.0"), "规则版本应为 0.1.0");
        require(!xml.contains("${"), "XML 依赖外部属性");
        require("UTF-8".equals(configuration.getProperty("charset")), "源码解码配置发生变化");
        require("java".equals(configuration.getProperty("fileExtensions")), "扫描扩展名配置发生变化");
        Path otherFile = write("main", "Example.txt", "\tclass Example {}\n");
        require(scan(configuration, List.of(otherFile)).isEmpty(), "非 Java 文件进入了扫描范围");
        var comments = Pattern.compile("<!--(.*?)-->", Pattern.DOTALL).matcher(xml);
        while (comments.find()) {
            require(!comments.group(1).contains("\n") && !comments.group(1).contains("\r"), "XML 注释应为单行");
        }
        require(rules.values().stream().allMatch(rule -> rule.configuration().getName().indexOf('.') < 0),
                "XML 引用了扩展检查器类");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        var document = factory.newDocumentBuilder().parse(directory.resolve("checkstyle.xml").toFile());
        var modules = document.getElementsByTagName("module");
        for (int index = 0; index < modules.getLength(); index++) {
            Element module = (Element) modules.item(index);
            if (Set.of("Checker", "TreeWalker").contains(module.getAttribute("name"))) {
                continue;
            }
            Node previous = module.getPreviousSibling();
            while (previous != null && previous.getNodeType() == Node.TEXT_NODE
                    && previous.getTextContent().isBlank()) {
                previous = previous.getPreviousSibling();
            }
            require(previous != null && previous.getNodeType() == Node.COMMENT_NODE
                    && !previous.getTextContent().isBlank(), module.getAttribute("name") + " 缺少规则说明");
        }
    }

    private void verifyReadme() throws Exception {
        String readme = Files.readString(directory.resolve("README.md"));
        require(readme.contains("规则版本为 **0.1.0**"), "README 规则版本应为 0.1.0");
        var versions = Pattern.compile("(?<!\\d)\\d+\\.\\d+\\.\\d+(?!\\d)").matcher(readme);
        while (versions.find()) {
            require(Set.of("0.1.0", "14.4.0").contains(versions.group()),
                    "README 包含当前规则和引擎之外的版本 " + versions.group());
        }
        var scripts = Pattern.compile("(?<![\\w./-])(\\./[\\w./-]+\\.sh)\\b").matcher(readme);
        while (scripts.find()) {
            require(Files.isRegularFile(directory.resolve(scripts.group(1))),
                    "README 引用了不存在的脚本 " + scripts.group(1));
        }
        try (Stream<Path> sources = Files.list(directory.resolve("tests"))) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String filename = source.getFileName().toString();
                require(readme.contains('`' + filename + '`') || readme.contains("`tests/" + filename + '`'),
                        "README 缺少测试文件 " + filename);
            }
        }
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (String key : rules.keySet()) {
            String label = key.startsWith("MatchXpath#") ? key.substring("MatchXpath#".length()) : key;
            require(readme.contains('`' + label + '`'), "README 缺少检查模块 " + label);
            String level = rules.get(key).configuration().getProperty("severity");
            require(readme.contains("| `" + label + "` | " + level + " |"), "README 等级与 XML 不一致 " + label);
            levels.merge(level, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> level : levels.entrySet()) {
            require(readme.contains(level.getValue() + " 项 " + level.getKey()), "README 等级统计与 XML 不一致");
        }
    }

    /**
     * XML 检查模块与它的执行位置
     *
     * @param key 模块名称和可选 id
     * @param configuration 实际配置
     * @param fileLevel 是否直接挂载在 Checker 下
     */
    private record Rule(String key, Configuration configuration, boolean fileLevel) {}

    /**
     * 已写入临时目录的测试输入
     *
     * @param rule 被验证的检查
     * @param accepted 合规文件
     * @param rejected 违规文件
     */
    private record PreparedCase(Rule rule, Path accepted, Path rejected) {}
}
