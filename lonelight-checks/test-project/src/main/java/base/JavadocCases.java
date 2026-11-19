// 检查：MissingJavadocType, JavadocType, MissingJavadocMethod, JavadocMethod, InvalidJavadocPosition
// 检查：JavadocMissingLeadingAsterisk, JavadocMissingWhitespaceAfterAsterisk, JavadocLeadingAsteriskAlign
// 检查：JavadocContentLocation, JavadocParagraph, NonEmptyAtclauseDescription
// 检查：RequireEmptyLineBeforeBlockTagGroup, AtclauseOrder, TrailingComment
// 检查：TodoComment#noCommentDivider, TodoComment#todoMarker

package base;

// ======
// todo 调整注释
public class JavadocCases {
    int count; // 行尾说明

    public JavadocCases() { }

    public void undocumented() { }

    /** 首行文字
     * 描述正文
     */
    void contentOnOpeningLine() { }

    /**
      * 星号错位
     */
    void misalignedAsterisk() { }

    /**
       缺少星号
     */
    void missingAsterisk() { }

    /**
     *星号后少空格
     */
    void missingSpace() { }

    /**
     * 第一段
     *
     * 第二段未标记
     */
    void missingParagraph() { }

    /**
     * 缺少参数和返回标签
     */
    int missingTags(int value) {
        return value;
    }

    /**
     * 顺序错误并缺少空行
     * @return 返回值
     * @param value
     */
    int invalidTags(int value) {
        return value;
    }

    void wrongPosition() {
        /** 这里不是声明的文档 */
        undocumented();
    }

    /**
     * 缺少泛型参数标签的类型
     */
    static class Generic<T> { }

    /**
     * 合规方法
     *
     * @param value 输入值
     * @return 输入值
     */
    int documented(int value) {
        return value;
    }
}
