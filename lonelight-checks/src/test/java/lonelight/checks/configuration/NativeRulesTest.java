package lonelight.checks.configuration;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import lonelight.checks.CheckTestSupport;
import lonelight.checks.FixtureCatalog;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用独立规则和完整配置验证内置规则的正反例
 *
 * @author lonelight
 */
class NativeRulesTest {
    @TempDir
    Path directoryPath;

    /**
     * 为每条内置规则生成独立的正反例测试
     *
     * @return 按规则生成的动态测试
     * @throws Exception 配置或规则读取失败时
     */
    @TestFactory
    Stream<DynamicTest> scan_nativeRules_checksAcceptedAndRejectedCases() throws Exception {
        Map<String, CheckTestSupport.LocatedRule> rules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-base"));
        var projectRules = CheckTestSupport.rules(CheckTestSupport.load("lonelight-project"));
        rules.putAll(projectRules);
        var baseExamples = FixtureCatalog.load("base");
        var projectExamples = FixtureCatalog.load("project");
        return NativeRuleCases.cases().stream().map(test -> DynamicTest.dynamicTest(test.key(), () -> {
            var rule = rules.get(test.key());
            assertNotNull(rule, test.key());
            Path validSourcePath = CheckTestSupport.write(directoryPath.resolve(test.key()).resolve("valid"), test.filename(), test.accepted());
            var examples = projectRules.containsKey(test.key()) ? projectExamples : baseExamples;
            var invalid = examples.filesFor(test.key());
            assertFalse(invalid.isEmpty(), test.key());
            var files = new ArrayList<>(invalid);
            files.add(validSourcePath);
            var events = CheckTestSupport.scan(rule.isolated(), files, getClass().getClassLoader());
            assertTrue(events.stream().noneMatch(event -> event.getFileName().equals(validSourcePath.toString())), events.toString());
            invalid.forEach(file -> assertTrue(events.stream().anyMatch(event -> occursIn(event, file)),
                    test.key() + " " + file));
            events.forEach(event -> {
                assertTrue(CheckTestSupport.matches(event, test.key()), event.getMessage());
                assertEquals(test.expectedSeverityLevel(), event.getSeverityLevel());
            });
        }));
    }

    /**
     * 验证内置规则正反例在完整配置下仍有预期结果
     */
    @Test
    void scan_completeProfiles_preservesNativeRuleResults() throws Exception {
        for (String group : List.of("base", "project")) {
            var configuration = CheckTestSupport.load("lonelight-" + group);
            var keys = CheckTestSupport.rules(configuration).keySet();
            var cases = NativeRuleCases.cases().stream().filter(test -> keys.contains(test.key())).toList();
            var examples = FixtureCatalog.load(group);
            var files = new ArrayList<>(examples.files());
            Map<RuleCase, Path> acceptedFiles = new LinkedHashMap<>();
            for (RuleCase ruleCase : cases) {
                Path acceptedSourcePath = CheckTestSupport.write(directoryPath.resolve(ruleCase.key()).resolve("valid"),
                        ruleCase.filename(), ruleCase.accepted());
                acceptedFiles.put(ruleCase, acceptedSourcePath);
                files.add(acceptedSourcePath);
            }
            var events = CheckTestSupport.scan(configuration, files, getClass().getClassLoader());
            acceptedFiles.forEach((test, accepted) -> {
                assertTrue(events.stream().noneMatch(event -> occursIn(event, accepted)
                        && CheckTestSupport.matches(event, test.key())), test.key() + " false positive in full profile");
                examples.filesFor(test.key()).forEach(file -> assertTrue(events.stream().anyMatch(event ->
                        occursIn(event, file) && CheckTestSupport.matches(event, test.key())
                                && event.getSeverityLevel() == test.expectedSeverityLevel()), test.key() + " " + file));
            });
        }
    }

    /**
     * 判断诊断是否来自指定的样例文件
     *
     * @param auditEvent 待核对的诊断
     * @param filePath   样例的绝对路径
     * @return 诊断来自此文件时为 true
     */
    private static boolean occursIn(AuditEvent auditEvent, Path filePath) {
        return auditEvent.getFileName().equals(filePath.toString());
    }
}
