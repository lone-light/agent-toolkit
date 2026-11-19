// 导入组边界有说明时，空行规则保留现有布局

package base;

import org.w3c.dom.Document;
// 数据源与文档类型来自不同的包组
import javax.sql.DataSource;
import java.util.List;

// 收集空结果时复用工具方法
import static java.util.Collections.emptyList;

class ImportsWithComments {
    DataSource dataSource;
    List<Document> documents = emptyList();
}
