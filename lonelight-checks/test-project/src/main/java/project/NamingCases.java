// 检查：VariableTypeNameCheck

package project;

import com.fasterxml.jackson.databind.ObjectMapper;

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

}
