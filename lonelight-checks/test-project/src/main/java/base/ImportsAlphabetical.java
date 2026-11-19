// 检查：CustomImportOrder
// 各组的位置和空行正确；在其他包、javax、java、静态导入四组内分别打乱字母顺序

package base;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;

import javax.sql.DataSource;
import javax.naming.Context;
import java.util.Map;
import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;

class ImportsAlphabetical {
    Document document;
    InputSource inputSource;
    DataSource dataSource;
    Context context;
    List<Document> documents = emptyList();
    Map<String, InputSource> sources = emptyMap();
}
