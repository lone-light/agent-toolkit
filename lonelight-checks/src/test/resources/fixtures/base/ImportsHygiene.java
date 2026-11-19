// 检查：UnusedImports, RedundantImport, IllegalImport
// 依次展示 JDK 内部包、重复导入、同包导入、java.lang 导入和未使用导入
package base;

import jdk.internal.misc.Unsafe;
import sun.misc.Signal;
import base.NamingCases;
import java.lang.String;
import java.util.List;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

class ImportsHygiene {
    List<String> names = emptyList();
    NamingCases<String> namingCases;
    Unsafe unsafe;
    Signal signal;
}
