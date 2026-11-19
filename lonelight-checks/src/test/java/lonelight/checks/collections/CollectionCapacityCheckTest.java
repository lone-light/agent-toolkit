package lonelight.checks.collections;

import lonelight.checks.CustomCheckTest;
import org.junit.jupiter.api.Test;

/**
 * 验证容量建议仅适用于已知 JDK 集合的无参构造
 *
 * @author lonelight
 */
class CollectionCapacityCheckTest extends CustomCheckTest {
    /**
     * 验证集合构造、复制、预计元素工厂和同名类型的区别
     */
    @Test
    void scan_collectionConstructors_reportsDefaultCapacity() throws Exception {
        verify(CollectionCapacityCheck.class, """
                import java.util.*;
                import example.ArrayList;
                class Example {
                    void collections(int size, Map<String, String> source) {
                        new HashMap<String, String>();
                        new java.util.ArrayList<String>();
                        new java.util.ArrayList<String>() {{ add("元素"); }};
                        new java.util.concurrent.ConcurrentHashMap<String, String>();
                        new ArrayList<String>();
                        new HashMap<>(source);
                        new java.util.ArrayList<String>(size);
                        HashMap.newHashMap(size);
                        new LinkedList<String>();
                        new String[size];
                    }
                }
                """, 3);
    }
}
