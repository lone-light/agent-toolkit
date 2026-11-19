// 检查：CollectionCapacityCheck
package base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 展示可调整容量的集合初始化写法
 *
 * @author lonelight
 */
public class CollectionExamples {
    /**
     * 创建用于收集订单号的集合
     *
     * @param expectedSize 预计收集的订单号数量
     * @return 已分配初始容量的列表
     */
    public List<Long> orderNumbers(int expectedSize) {
        List<Long> unsized = new ArrayList<>();
        List<Long> sized = new ArrayList<>(expectedSize);
        sized.addAll(unsized);
        return sized;
    }

    /**
     * 按预计键数构建映射
     *
     * @param expectedSize 预计键数
     * @return 新映射
     */
    public Map<Long, String> index(int expectedSize) {
        Map<Long, String> unsized = new HashMap<>();
        Map<Long, String> sized = HashMap.newHashMap(expectedSize);
        sized.putAll(unsized);
        return sized;
    }
}
