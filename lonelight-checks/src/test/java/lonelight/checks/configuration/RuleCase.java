package lonelight.checks.configuration;

import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * 记录内置规则的合规源码及预期诊断等级
 *
 * @param key                   检查模块名称，包含可选的配置 id
 * @param expectedSeverityLevel 独立于 XML 配置的预期诊断等级
 * @param accepted              应通过目标检查的合规源码
 * @param filename              创建合规样例时使用的 Java 文件名
 * @author lonelight
 */
record RuleCase(String key, SeverityLevel expectedSeverityLevel, String accepted, String filename) {

    /**
     * 用默认文件名建立规则用例
     *
     * @param key                   规则名称和可选标识
     * @param expectedSeverityLevel 预期诊断等级
     * @param accepted              应通过规则的源码
     */
    RuleCase(String key, SeverityLevel expectedSeverityLevel, String accepted) {
        this(key, expectedSeverityLevel, accepted, "Example.java");
    }

    /**
     * 将成员代码包成可扫描的类型
     *
     * @param members 类型成员源码
     * @return 完整类型源码
     */
    static String type(String members) {
        return "class Example {\n" + members + "\n}\n";
    }

    /**
     * 将方法体包成可扫描的类型
     *
     * @param body 方法体源码
     * @return 完整类型源码
     */
    static String method(String body) {
        return type("void run() {\n" + body + "\n}");
    }
}
