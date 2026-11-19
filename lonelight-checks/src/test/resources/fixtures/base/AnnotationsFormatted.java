// 标准注解的排版对照：声明注解另起一行，参数和类型使用注解保留在相关位置
package base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Deprecated
class AnnotationsFormatted {
    @Deprecated
    private String title = "订单";

    @Deprecated
    @SuppressWarnings("unused")
    AnnotationsFormatted() { }

    @Deprecated
    void update(@Marker String value) {
        title = value;
    }

    @Deprecated
    interface Handler { }

    @Deprecated
    record Entry(@Marker String title) { }

    @Deprecated
    enum Mode { ON, OFF }

    @Target({ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
    @interface Marker { }

    @Deprecated
    @interface OldFlag { }
}
