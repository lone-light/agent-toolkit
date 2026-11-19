package examples;

/**
 * 基础规则的合规样例，扫描时应当通过全部检查
 *
 * @author example
 */
public final class GoodExample {

    /**
     * 示例名称
     */
    private final String name;

    /**
     * 创建示例
     *
     * @param name 示例名称
     */
    public GoodExample(String name) {
        this.name = name;
    }

    /**
     * 获取示例名称
     *
     * @return 示例名称
     */
    public String name() {
        return name;
    }

    /**
     * 判断是否为启用状态
     *
     * @param value 当前状态
     * @return 是否启用
     */
    public boolean isActive(String value) {
        return "active".equals(value);
    }

    /**
     * 根据开关返回状态
     *
     * @param enabled 是否启用
     * @return 状态说明
     */
    public String status(boolean enabled) {
        if (enabled) {
            return "enabled";
        }
        return "disabled";
    }

    /**
     * 解析数字
     *
     * @param value 输入文本
     * @return 解析结果，格式错误时返回零
     */
    public int parseNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            // 输入格式不合法时采用默认值
            return 0;
        }
    }
}
