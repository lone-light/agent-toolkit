package lonelight.checks.configuration;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 用独立规则和完整配置验证内置规则的正反例
 *
 * @author lonelight
 */
class NativeRulesTest {
    @TempDir
    Path directory;

    /**
     * 为每条内置规则生成独立的正反例测试
     *
     * @return 按规则生成的动态测试
     * @throws Exception 配置或规则读取失败时
     */
    @TestFactory
    Stream<DynamicTest> nativeRules() throws Exception {
        Map<String, CheckTestSupport.LocatedRule> rules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-base"));
        var projectRules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-project"));
        rules.putAll(projectRules);
        var baseExamples = FixtureCatalog.load("base");
        var projectExamples = FixtureCatalog.load("project");
        return NativeRuleCases.cases().stream().map(test -> DynamicTest.dynamicTest(test.key(), () -> {
            var rule = rules.get(test.key());
            assertNotNull(rule, test.key());
            Path valid = CheckTestSupport.write(directory.resolve(test.key()).resolve("valid"), test.filename(), test.accepted());
            var examples = projectRules.containsKey(test.key()) ? projectExamples : baseExamples;
            var invalid = examples.filesFor(test.key());
            assertFalse(invalid.isEmpty(), test.key());
            var files = new ArrayList<>(invalid);
            files.add(valid);
            var events = CheckTestSupport.scan(rule.isolated(), files, getClass().getClassLoader());
            assertTrue(events.stream().noneMatch(event -> event.getFileName().equals(valid.toString())), events.toString());
            invalid.forEach(file -> assertTrue(events.stream().anyMatch(event -> occursIn(event, file)),
                    test.key() + " " + file));
            events.forEach(event -> {
                assertTrue(CheckTestSupport.matches(event, test.key()), event.getMessage());
                assertEquals(test.expectedSeverity(), event.getSeverityLevel());
            });
        }));
    }

    /**
     * 验证内置规则正反例在完整配置下仍有预期结果
     */
    @Test
    void nativePairsAlsoWorkThroughTheCompleteProfiles() throws Exception {
        for (String group : List.of("base", "project")) {
            var configuration = CheckTestSupport.load("lonelight-" + group);
            var keys = CheckTestSupport.rules(configuration).keySet();
            var cases = NativeRuleCases.cases().stream().filter(test -> keys.contains(test.key())).toList();
            var examples = FixtureCatalog.load(group);
            var files = new ArrayList<>(examples.files());
            Map<RuleCase, Path> acceptedFiles = new LinkedHashMap<>();
            for (RuleCase test : cases) {
                Path accepted = CheckTestSupport.write(directory.resolve(test.key()).resolve("valid"),
                        test.filename(), test.accepted());
                acceptedFiles.put(test, accepted);
                files.add(accepted);
            }
            var events = CheckTestSupport.scan(configuration, files, getClass().getClassLoader());
            acceptedFiles.forEach((test, accepted) -> {
                assertTrue(events.stream().noneMatch(event -> occursIn(event, accepted)
                        && CheckTestSupport.matches(event, test.key())), test.key() + " false positive in full profile");
                examples.filesFor(test.key()).forEach(file -> assertTrue(events.stream().anyMatch(event ->
                        occursIn(event, file) && CheckTestSupport.matches(event, test.key())
                                && event.getSeverityLevel() == test.expectedSeverity()), test.key() + " " + file));
            });
        }
    }

    /**
     * 判断诊断是否来自指定的样例文件
     *
     * @param event 待核对的诊断
     * @param file 样例的绝对路径
     * @return 诊断来自此文件时为 true
     */
    private static boolean occursIn(AuditEvent event, Path file) {
        return event.getFileName().equals(file.toString());
    }
}
