// 导入顺序对照：其他包、javax、java、静态导入

package base;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * 保存文档及其数据来源
 *
 * @param document    已解析的文档
 * @param inputSource 文档输入
 * @param dataSource  数据源
 * @author lonelight
 */
record ImportsSorted(Document document, InputSource inputSource, DataSource dataSource) {
    /**
     * 返回可使用的文档
     *
     * @return 当前文档列表
     */
    List<Document> documents() {
        return document == null ? emptyList() : List.of(document);
    }

    /**
     * 返回可使用的输入
     *
     * @return 按名称索引的文档输入
     */
    Map<String, InputSource> sources() {
        return inputSource == null ? emptyMap() : Map.of("source", inputSource);
    }
}
