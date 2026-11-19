// 检查：ImportGroupSpacingCheck
// 导入顺序正确；分别演示缺少组间空行、javax 和 java 之间多空行、静态组前多空行
package base;

import org.w3c.dom.Document;
import javax.sql.DataSource;

import java.util.List;


import static java.util.Collections.emptyList;

class ImportsSpacing {
    Document document;
    DataSource dataSource;
    List<Document> documents = emptyList();
}
