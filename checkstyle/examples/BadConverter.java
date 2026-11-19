package examples;

/**
 * Converter 契约的违规样例，扫描时应当提示 final 和私有构造器要求
 *
 * @author example
 */
public class BadConverter {

    /** 创建转换工具 */
    public BadConverter() {}

    /**
     * 规范化名称
     *
     * @param name 原始名称
     * @return 去掉首尾空白的名称
     */
    public static String normalize(String name) {
        return name.strip();
    }
}
