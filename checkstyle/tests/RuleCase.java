package tests;

import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * 同一约束的合规和违规输入
 *
 * @param key 被验证的模块
 * @param expectedSeverity 独立于 XML 配置的预期等级
 * @param accepted 合规源码
 * @param rejected 违规源码
 * @param filename 源码文件名
 * @author example
 */
record RuleCase(String key, SeverityLevel expectedSeverity, String accepted, String rejected, String filename) {

    RuleCase(String key, SeverityLevel expectedSeverity, String accepted, String rejected) {
        this(key, expectedSeverity, accepted, rejected, "Example.java");
    }

    /**
     * 包装类型成员，省去用例中重复的外层声明
     *
     * @param members 类型成员源码
     * @return 完整类型源码
     */
    static String type(String members) {
        return "class Example {\n" + members + "\n}\n";
    }

    /**
     * 包装方法体，省去用例中重复的方法声明
     *
     * @param body 方法体源码
     * @return 完整类型源码
     */
    static String method(String body) {
        return type("void run() {\n" + body + "\n}");
    }
}
