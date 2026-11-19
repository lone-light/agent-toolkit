// 检查：MissingDeprecated
package base;

/**
 * 对照废弃注解和迁移说明的写法
 *
 * @author lonelight
 */
public class DeprecatedCases {
    /**
     * 展示仅声明废弃注解的情况
     */
    @Deprecated
    public void annotatedOnly() { }

    /**
     * 展示仅填写废弃说明的情况
     *
     * @deprecated 请使用 {@link #read()}。
     */
    @SuppressWarnings("dep-ann")
    public void documentedOnly() { }

    /**
     * 展示废弃注解和说明成对的写法
     *
     * @deprecated 请使用 {@link #read()}。
     */
    @Deprecated
    public void legacy() { }

    /**
     * 提供当前的读取入口
     */
    public void read() { }
}
