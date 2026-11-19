# 规则测试项目

`test-project` 是可编译的 Maven 样例项目。Java 规范样例位于 `src/main/java`，JUnit 命名样例位于 `src/test/java`。用 JDK 21 执行 `mvn clean verify` 编译并打包。

每条检查的触发条件和等级见 [规则清单](../RULES.md)。

## 在 IDEA 检查

1. 在 IDEA 的 Maven 面板选择「添加 Maven 项目」，导入本目录的 `pom.xml`。
2. 按 [构建与验证](../README.md#构建与验证) 生成检查器 JAR，再按 [在 IDEA 导入](../README.md#在-idea-导入) 配置 Checkstyle。
3. 在项目约定配置的 `Edit Properties` 中，将 `lonelight.typeSuffixMappings` 设为 `optional\..*=DTO|Request`。
4. 启用通用规则和项目约定，扫描范围选 `Only Java sources (including tests)`。打开测试项目内任一 Java 文件，在 CheckStyle 的 `Rules` 中选择 `<active rules>`，点 `Check Module` 扫描整个 `test-project`。文件头的 `// 检查：` 标出该文件演示的违规规则。两份配置也会报告文件中的其他违规。

[AnnotationsFormatted](src/main/java/base/AnnotationsFormatted.java)、[ImportsSorted](src/main/java/base/ImportsSorted.java) 和 [OrderSummary](src/main/java/base/OrderSummary.java) 是两份配置均无诊断的对照样例。检查器的打包测试会核对对照样例和各条规则的预期诊断。IDEA 也会将包名与目录不符的反例标为错误，Maven 仍可完成编译。

## 按主题选文件

| 主题 | 文件 | 主要对照 |
| --- | --- | --- |
| 文件和模块 | [FileLayout](src/main/java/base/FileLayout.java)、[包与目录](src/main/java/base/PackageLocation.java)、[文件名](src/main/java/base/FilenameMismatch.java)、[模块声明](src/main/java/module-info.java) | 行尾空白、Tab、行宽、换行、包名、包私有类型与文件名不符、多个顶层类型和模块指令顺序 |
| 导入 | [分组顺序](src/main/java/base/ImportsOrder.java)、[四组内排序](src/main/java/base/ImportsAlphabetical.java)、[空行](src/main/java/base/ImportsSpacing.java)、[冗余导入](src/main/java/base/ImportsHygiene.java) | [正确排序](src/main/java/base/ImportsSorted.java)、[带说明的组边界](src/main/java/base/ImportsWithComments.java)、[内部 API 与公开 API](src/main/java/base/InternalImports.java) |
| 排版、声明和命名 | [LayoutCases](src/main/java/base/LayoutCases.java)、[NamingCases](src/main/java/base/NamingCases.java)、[UtilityCases](src/main/java/base/UtilityCases.java) | 缩进、空格、括号、修饰符、声明顺序、变量和类型命名、工具类构造器 |
| 代码写法 | [ControlFlowCases](src/main/java/base/ControlFlowCases.java)、[CodeQualityCases](src/main/java/base/CodeQualityCases.java)、[CollectionExamples](src/main/java/base/CollectionExamples.java) | 控制流、空异常处理、未使用声明、可简化表达式和集合的预设容量 |
| 并发与十进制数 | [RuntimeApiCases](src/main/java/base/RuntimeApiCases.java) | 传统线程池工厂与显式有界配置、虚拟线程执行器、浮点与字符串构造 |
| 注解和文档 | [AnnotationCases](src/main/java/base/AnnotationCases.java)、[AnnotationsFormatted](src/main/java/base/AnnotationsFormatted.java)、[通用 Javadoc](src/main/java/base/JavadocCases.java)、[项目 Javadoc](src/main/java/project/JavadocCases.java) | 注解排版、同名冲突、类型摘要和 `@author`、字段说明和代码片段豁免 |
| API 废弃 | [DeprecatedCases](src/main/java/base/DeprecatedCases.java) | `@Deprecated` 或 `@deprecated` 单独出现，以及带迁移说明的完整写法 |
| 项目命名和校验 | [项目命名](src/main/java/project/NamingCases.java)、[JUnit 命名](src/test/java/project/JUnitNamingCases.java)、[ValidationAnnotations](src/main/java/project/ValidationAnnotations.java) | 类型变量名、JUnit 各类入口、44 种校验注解，以及空白、转义、拼接、常量和同名注解 |
| 分层、转换和日志 | [FrameworkCases](src/main/java/project/FrameworkCases.java)、[InjectionCases](src/main/java/project/InjectionCases.java)、[EntityCreationCases](src/main/java/project/EntityCreationCases.java)、[OrderBoundaryController](src/main/java/project/web/OrderBoundaryController.java) | 分层依赖、字段和构造器注入、实体 Builder、BeanUtils 和日志占位符 |
| 过滤和正例 | [SuppressionCases](src/main/java/base/SuppressionCases.java)、[OrderSummary](src/main/java/base/OrderSummary.java) | 注解豁免、成对注释豁免、框架字段和两份配置均通过的代码 |
| 类型后缀 | [UserProfile](src/main/java/optional/UserProfile.java) | `UserProfile` 缺少后缀，`AddressDTO` 和 `AddressRequest` 通过 |

生成目录排除见 [FiltersTest](../src/test/java/lonelight/checks/configuration/FiltersTest.java)。

## 接口注解对照

| Controller | 应提示 | 应通过 |
| --- | --- | --- |
| [OrderQueryController](src/main/java/project/web/OrderQueryController.java) | 摘要缺失或空白、路径和隐式标量参数缺少说明、查询参数的说明名称或位置有误 | 说明引用、矩阵参数和路径参数的直接说明 |
| [OrderAccessController](src/main/java/project/web/OrderAccessController.java) | 请求头、Cookie 缺少说明 | 请求头名称忽略大小写，框架注入、隐藏参数和隐藏接口豁免 |
| [OrderWriteController](src/main/java/project/web/OrderWriteController.java) | 跨文件请求字段、表单字段和原始请求体缺少说明 | 说明完整的请求模型、只读或隐藏字段，以及带说明的原始请求体 |
| [OrderFilesController](src/main/java/project/web/OrderFilesController.java) | 泛型响应的 `orderId` 字段和上传分段缺少说明，同一字段在同一文件只报一次 | 说明完整的数组字段、只写字段、文件、流式和 `void` 返回 |
| [OrderBoundaryController](src/main/java/project/web/OrderBoundaryController.java) | Controller 声明 Mapper 依赖，REST 方法直接或在泛型中返回 Entity | MVC 页面方法、独立的对外视图 |
| [OrderValidationController](src/main/java/project/web/OrderValidationController.java) | 有字段约束的请求对象缺少 `@Valid`，接口模型布尔属性使用 `is` 前缀 | 带 `@Valid` 的请求对象，以及 `ready` 这类直接表达状态的属性名 |
