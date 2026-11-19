// 检查：EntityBuilderCheck
package project;

import project.entity.BuildableOrder;

/**
 * 对照实体的直接创建和构建器调用
 *
 * @author lonelight
 */
class EntityCreationCases {
    /**
     * 展示调用方直接创建实体
     *
     * @return 新订单
     */
    BuildableOrder direct() {
        return new BuildableOrder();
    }

    /**
     * 展示通过构建器创建实体
     *
     * @return 新订单
     */
    BuildableOrder build() {
        return BuildableOrder.builder().build();
    }
}
