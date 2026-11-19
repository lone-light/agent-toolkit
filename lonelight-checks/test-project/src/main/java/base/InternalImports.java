// 检查：IllegalImport
// 通配导入只用于触发内部包检查，公开的 HttpServer 用作对照。

package base;

import com.sun.net.httpserver.HttpServer;
import sun.misc.*;

class InternalImports {
    int serverPort(HttpServer server) {
        return server.getAddress().getPort();
    }
}
