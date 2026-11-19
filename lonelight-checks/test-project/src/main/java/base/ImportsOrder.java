// 检查：CustomImportOrder, ImportGroupSpacingCheck
// 此文件的导入都被用到：组顺序和组内排序打乱，同时包含缺失与多余的空行

package base;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import java.util.Map;
import java.util.List;

import javax.sql.DataSource;


import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;

class ImportsOrder {
    Document document;
    InputSource inputSource;
    DataSource dataSource;
    List<Document> documents = emptyList();
    Map<String, InputSource> sources = emptyMap();
}
