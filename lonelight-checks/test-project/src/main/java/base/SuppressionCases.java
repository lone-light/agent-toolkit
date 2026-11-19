// 检查：NeedBraces, UnusedPrivateField
// 同一文件对照框架字段、局部豁免和仍应报告的写法

package base;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 对照框架注入和显式豁免的检查范围
 *
 * @author lonelight
 */
class SuppressionCases {
    @Autowired
    private OrderSummary injectedSummary;

    @JsonProperty
    private String serializedNumber;

    @SuppressWarnings("unused")
    private String intentionallyUnused;

    // 未使用且无豁免，应提示
    private String unusedNumber;

    @SuppressWarnings("checkstyle:NeedBraces")
    boolean suppressedByAnnotation(boolean available) {
        if (available) return true;
        return false;
    }

    boolean suppressedByComment(boolean available, boolean reserved) {
        // CHECKSTYLE.OFF: NeedBraces
        if (available) return true;
        // CHECKSTYLE.ON: NeedBraces
        if (reserved) return false;
        return true;
    }
}
