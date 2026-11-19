// 声明注解另起一行，参数和 record 组件注解保留在声明处

package base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * 展示声明注解和类型注解的排版
 *
 * @author lonelight
 */
@AnnotationsFormatted.Marker
class AnnotationsFormatted {
    /**
     * 当前标题
     */
    @Marker
    private String title = "订单";

    /**
     * 创建标题记录
     */
    @Marker
    AnnotationsFormatted() {
    }

    /**
     * 保存并返回标题
     *
     * @param value 新标题
     * @return 当前标题
     */
    @Marker
    String update(@Marker String value) {
        title = value;
        return title;
    }

    /**
     * 展示 record 组件上的注解
     *
     * @param title 标题
     * @author lonelight
     */
    @Marker
    record Entry(@Marker String title) {
    }

    /**
     * 展示枚举声明上的注解
     *
     * @author lonelight
     */
    @Marker
    enum Mode {
        ON, OFF
    }

    /**
     * 标记声明及类型位置
     *
     * @author lonelight
     */
    @Target({
            ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
            ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE,
    })
    @interface Marker {
    }
}
