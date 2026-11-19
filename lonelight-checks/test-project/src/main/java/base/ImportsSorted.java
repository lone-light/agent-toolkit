// 导入顺序对照：其他包、javax、java、静态导入

package base;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

class ImportsSorted {
    Document document;
    InputSource inputSource;
    DataSource dataSource;
    List<Document> documents = emptyList();
    Map<String, InputSource> sources = emptyMap();
}
