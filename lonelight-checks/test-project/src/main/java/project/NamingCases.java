// 检查：VariableTypeNameCheck

package project;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

class NamingCases {
    boolean isReady;
    Boolean isActive;
    static boolean isShared;
    boolean[] isSelected;
    OrderShape item;
    OrderShape orderShape;
    OrderShape targetOrderShape;
    ObjectMapper mapper;

    String naming(OrderShape value, String text, List<OrderShape> orders) {
        OrderShape current = value;
        var inferred = current;
        OrderShape[] items = {inferred};
        return text + orders + items.length;
    }

    record OrderShape(boolean isAvailable, Boolean enabled) { }

    record OrderGroup(OrderShape item, OrderShape primaryOrderShape) { }

    // 显式接收者和 orderShape 均合规，只有 other 应收到变量命名提示
    boolean sameShape(NamingCases this, NamingCases.@TypeUse OrderShape orderShape,
                      NamingCases.@TypeUse OrderShape other) {
        return orderShape.equals(other);
    }

    /**
     * 标记限定类型中的类型用法
     *
     * @author lonelight
     */
    @Target(ElementType.TYPE_USE)
    @interface TypeUse { }
}
