package examples;

/**
 * Converter 契约的合规样例，扫描时应当通过全部检查
 *
 * @author example
 */
public final class GoodConverter {

    private GoodConverter() {}

    /**
     * 规范化名称
     *
     * @param name 原始名称
     * @return 去掉首尾空白的名称，空输入返回空串
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.strip();
    }
}
