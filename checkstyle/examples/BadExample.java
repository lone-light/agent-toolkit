package examples;

/**
 * 基础规则的违规样例，扫描时应当出现对应提示
 *
 * @author example
 */
public class BadExample {

    // 预期提示字段缺少多行说明，以及私有字段未使用
    private String unused;

    // 预期提示方法命名、公开方法缺少文档，以及字符串字面量比较
    public boolean bad_name(String value) {
        return value == "active";
    }

    // 预期提示说明中的编号与句号、缺少参数标签、缺少花括号以及魔法数字
    /**
     * 1. 输出结果。
     */
    public void printResult(boolean enabled) {
        if (enabled) System.out.println(42);
    }

    /**
     * 演示空 catch 检查
     *
     * @param value 输入文本
     * @return 解析结果
     */
    public int parseNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
        }
        return 0;
    }

    /**
     * 演示 switch 缺少 default 检查
     *
     * @param code 当前编码
     * @return 对应结果
     */
    public int status(int code) {
        switch (code) {
            case 1:
                return 1;
            case 2:
                return 2;
        }
        return 0;
    }
}
