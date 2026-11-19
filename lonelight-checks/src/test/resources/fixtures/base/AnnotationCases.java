// 检查：AnnotationLocation, QualifiedAnnotationCheck
// IDEA 可识别文件中的标准注解和本地注解，适合测试类、接口、record、枚举、方法、构造器和字段的排版
package base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@java.lang.Deprecated class AnnotationCases {
    @Deprecated String title = "订单";

    @SuppressWarnings("unused") @Deprecated
    AnnotationCases() { }

    @Deprecated void update(@Marker String value) {
        @Marker String text = value;
        title = text;
    }

    @Deprecated interface Handler { }

    @Deprecated record Entry(@Marker String title) { }

    @Deprecated enum Mode { ON, OFF }

    @Target({ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.LOCAL_VARIABLE})
    @interface Marker { }

    @Deprecated @interface OldFlag { }

    // 同名注解使短名有歧义，必须保留其中一个全名
    static class SameName {
        @java.lang.annotation.Target(ElementType.TYPE)
        @interface Flag { }

        @Flag
        @AnnotationCases.Other.Flag
        static class Marked { }
    }

    // 同一作用域存在自定义 SuppressWarnings，标准注解需要保留全名
    static class NameCollision {
        @interface SuppressWarnings { }

        @SuppressWarnings
        @java.lang.SuppressWarnings("unused")
        private String unused;
    }

    static class Other {
        @interface Flag { }
    }
}
