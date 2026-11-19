// 检查：VariableTypeNameCheck, BooleanFieldNameCheck

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

    void naming(OrderShape value, String text, List<OrderShape> orders) {
        OrderShape current = value;
        var inferred = current;
        OrderShape[] items = {inferred};
        System.out.println(text + orders + items.length);
    }

    record OrderShape(boolean isAvailable, Boolean enabled) { }

    record OrderGroup(OrderShape item, OrderShape primaryOrderShape) { }

}
