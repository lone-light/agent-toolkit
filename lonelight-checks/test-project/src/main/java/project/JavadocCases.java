// 检查：MultilineJavadocCheck, MethodJavadocPeriodCheck, MethodJavadocNumberingCheck, FieldJavadocCheck, TypeJavadocCheck

package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

class JavadocCases {
    private String title;

    /**
     * 订单编号
     */
    private String orderNumber;

    @Schema(description = "备注")
    private String remark;

    @Schema(hidden = true)
    private String internalKey;

    @Schema(description = " ")
    private String emptyDescription;

    @JsonIgnore
    private String ignored;

    @JsonIgnore(false)
    private String visible;

    static String shared;

    /** 订单数量 */
    int quantity;

    /** 单行方法 */
    void oneLine() { }

    /**
     * 1. 更新订单。
     * 二、保存结果.
     * （3）返回结果
     */
    void numberedAndWithPeriods() { }

    /**
     * 保留示例中的符号
     *
     * <pre>
     * 1. 示例。
     * </pre>
     * {@code 2. 示例.}
     * {@literal 3. 示例。}
     * <!-- 4. 隐藏说明。 -->
     */
    void documented() { }

    /**
     * 保留短说明 */
    void closingMarkerOnSameLine() { }

    /**
     * 部分组件的说明可以写在类型文档中
     *
     * @param note 备注
     */
    record Item(String number, String note, @Schema(description = "数量") int count) { }
}
