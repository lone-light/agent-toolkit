package project.entity;

/**
 * 展示带有公开构建器的订单实体
 *
 * @author lonelight
 */
public class BuildableOrder {
    /**
     * 支持框架创建实体
     */
    public BuildableOrder() { }

    /**
     * 建立订单构建器
     *
     * @return 新的构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建订单实体
     *
     * @author lonelight
     */
    public static class Builder {
        /**
         * 构建订单实例
         *
         * @return 新的订单实例
         */
        public BuildableOrder build() {
            return new BuildableOrder();
        }
    }
}
