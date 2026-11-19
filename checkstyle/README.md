# Java 代码规范

规则版本为 **0.1.0**。导入文件为 `checkstyle.xml`，运行引擎为标准 **Checkstyle 14.4.0**。在 IDEA 的 Checkstyle 设置中选择该引擎版本，添加并启用 XML 即可扫描 Java 源码。

## 规则概览

当前启用 **124 个检查模块**，其中 37 项 error、80 项 warning、7 项 info。以下表格列出实际模块或 id，规则说明与 XML 对应。

- error 用于错误风险和强制约定，CLI 遇到此等级的违规时返回非零状态。
- warning 用于命名、排版和一般规范。
- info 用于行宽和简化建议。

## 文件与结构

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `FileTabCharacter` | warning | 禁止 Tab 字符 |
| `NewlineAtEndOfFile` | warning | 非空文件末尾保留换行 |
| `LineLength` | info | 提示超过 120 字符的行 |
| `RegexpSingleline#noTrailingWhitespace` | warning | 禁止行尾空白 |
| `OuterTypeFilename` | error | 核对外层类型名称与 Java 文件名，避免类型和文件错配 |
| `PackageDeclaration` | error | 要求包声明存在并与源码所在目录对应 |
| `OneTopLevelClass` | warning | 要求一个源码文件只声明一个顶层类型，便于导航与维护 |
| `NoLineWrap` | warning | 让 package、各类 import 和模块声明头保持在同一行 |
| `ModuleDirectiveOrder` | warning | 按 requires、exports、opens、uses、provides 排列 Java 模块指令 |

## 命名

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `PackageName` | warning | 统一包名为小写分段形式 |
| `TypeName` | warning | 统一具名类型使用大驼峰名称 |
| `MethodName#productionMethodName` | warning | 统一普通方法使用以小写字母开头的驼峰名称 |
| `MemberName` | warning | 统一普通实例字段使用小驼峰名称 |
| `StaticVariableName` | warning | 统一可变静态字段使用小驼峰名称 |
| `ParameterName` | warning | 统一方法和构造方法参数使用小驼峰名称 |
| `LocalVariableName` | warning | 统一普通局部变量使用小驼峰名称 |
| `LocalFinalVariableName` | warning | 让 final 局部变量和内置规则识别的 catch、资源变量保持小驼峰命名 |
| `PatternVariableName` | warning | 统一模式匹配中声明的变量名称 |
| `LambdaParameterName` | warning | 统一 Lambda 参数名称，便于在回调中追踪数据含义 |
| `CatchParameterName` | warning | 统一捕获异常时的参数名称 |
| `RecordComponentName` | warning | 统一 record 组件名称，使组件与生成的访问器保持一致 |
| `ConstantName` | warning | 统一常量字段使用大写字母、数字和下划线 |
| `AbbreviationAsWordInName` | warning | 限制名称中的连续大写缩写，保留 DTO、VO 两个角色缩写 |
| `ClassTypeParameterName` | warning | 类泛型参数允许 T、T1、ResultT 等名称 |
| `InterfaceTypeParameterName` | warning | 接口泛型参数允许 T、T1、ResultT 等名称 |
| `MethodTypeParameterName` | warning | 方法泛型参数允许 T、T1、ResultT 等名称 |
| `RecordTypeParameterName` | warning | record 泛型参数允许 T、T1、ResultT 等名称 |

## 导入

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `UnusedImports` | warning | 报告单文件中未使用的显式导入，减少多余依赖 |
| `RedundantImport` | warning | 报告重复导入、同包导入和其他可由语法确定的冗余导入 |
| `IllegalImport` | error | 禁止依赖内部 JDK 包及指定的 BeanUtils 转换入口 |
| `ImportOrder` | warning | 其他包 → javax → java → 静态导入，组内按字母排序，javax 与 java 可相邻 |

## 排版与空白

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `Indentation` | warning | 基础、续行、case、throws 和数组初始化采用 4 空格 |
| `NeedBraces` | error | 要求 if、else、for、while 和 do 的受控语句使用花括号 |
| `LeftCurly` | warning | 左花括号采用行尾风格 |
| `RightCurly` | warning | 统一 if/else 和 try/catch/finally 的右花括号及后续关键字位置 |
| `WhitespaceAfter` | warning | 检查逗号、分号、类型转换及指定关键字之后的必要空白 |
| `WhitespaceAround` | warning | 检查赋值、运算符、控制流关键字和花括号周围的空白 |
| `WhitespaceBeforeEmptyBody` | warning | 要求空方法、空类型和其他空体的左花括号前保留空白 |
| `GenericWhitespace` | warning | 检查泛型尖括号附近的空白，避免类型参数被不规则间距分隔 |
| `SingleSpaceSeparator` | warning | 限制同一行相邻语法元素之间出现多个分隔空格 |
| `MethodParamPad` | warning | 要求方法名、构造调用等名称与随后的左圆括号直接相接 |
| `ParenPad` | warning | 去掉普通圆括号内侧的多余空格 |
| `TypecastParenPad` | warning | 去掉强制类型转换圆括号内侧的多余空格 |
| `EmptyForIteratorPad` | warning | 统一传统 for 中省略迭代表达式时的空白 |
| `NoWhitespaceBefore` | warning | 去掉逗号、分号、后置递增递减、可变参数标记及标签冒号前的多余空白 |
| `NoWhitespaceBeforeCaseDefaultColon` | warning | 去掉传统 switch 中 case 和 default 冒号前的多余空格 |
| `EmptyLineSeparator` | warning | 分隔包、导入和成员声明，允许相邻字段不空行以及多个空行 |
| `AnnotationLocation` | warning | 让类型、方法、构造器和字段的声明注解各占一行 |

## 声明与封装

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `OneStatementPerLine` | warning | 避免把多条独立语句压在同一行，便于定位与调试 |
| `MultipleVariableDeclarations` | warning | 要求变量分别声明，避免一条声明混放多个名称和初始值 |
| `ArrayTypeStyle` | warning | 统一采用 String[] names 形式的 Java 数组声明风格 |
| `UpperEll` | warning | 要求 long 字面量使用大写 L 后缀，减少与数字 1 混淆 |
| `ModifierOrder` | warning | 让 Java 修饰符遵循语言规范建议顺序，注解置于修饰符之前 |
| `RedundantModifier` | info | 提示语言语义中已经隐含的多余修饰符 |
| `DeclarationOrder` | warning | 检查静态字段、实例字段、构造器和方法的大类排列 |
| `VisibilityModifier` | warning | 约束字段可见性，普通字段默认采用 private 封装 |
| `VariableDeclarationUsageDistance` | info | 局部变量声明与首次使用距离超过 12 时提示，默认忽略 final |
| `MissingOverrideOnRecordAccessor` | warning | 要求显式编写的 record 组件访问器标注 @Override |
| `HideUtilityClassConstructor` | warning | 限制纯静态工具类的可访问构造器，避免工具类被直接实例化 |
| `FinalClass` | warning | 提示只有私有构造器等适合封闭继承的类显式声明 final |

## 错误预防与简化

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `NoFinalizer` | error | 禁止声明 finalize 清理方法，避免依赖不可控的终结时机 |
| `EmptyStatement` | error | 报告无意保留的空语句，防止循环或条件后多出的分号改变行为 |
| `InnerAssignment` | warning | 提示子表达式中的赋值，减少隐藏的状态变化 |
| `EqualsHashCode` | error | 要求 equals 与 hashCode 成对覆写，维护散列集合使用契约 |
| `StringLiteralEquality` | error | 报告使用相等或不等运算符比较字符串字面量的写法 |
| `CovariantEquals` | error | 识别只定义 equals(本类型) 而缺少 equals(Object) 的错误重载 |
| `ModifiedControlVariable` | error | 限制在循环体中再次修改控制变量，减少循环推进逻辑分散 |
| `AvoidDoubleBraceInitialization` | warning | 提示通过匿名类加实例初始化块完成的双大括号初始化 |
| `SimplifyBooleanExpression` | info | 提示和 true、false 组合后可直接简化的布尔表达式 |
| `SimplifyBooleanReturn` | info | 提示 if/else 分别返回 true 和 false 时可直接返回条件 |
| `UnusedLocalVariable` | warning | 报告声明后没有实际读取的局部变量，减少失效代码 |
| `UnusedPrivateField` | warning | 报告当前源码中没有读取的 private 字段 |
| `MagicNumber` | warning | 提示未命名数字，放行 -1、0、1、2、注解与 hashCode 中的数字 |
| `MultipleStringLiterals` | info | 相同字符串出现次数超过 1 时提示，忽略空串、单字符和注解 |

## 控制流

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `EmptyCatchBlock` | error | 要求空 catch 至少写明忽略异常的原因 |
| `FallThrough` | error | 报告传统 switch 中缺少终止语句且没有贯穿说明的分支 |
| `DefaultComesLast` | warning | 要求已有的 default 分支位于 switch 最后 |
| `MissingSwitchDefault` | error | 要求传统 switch 语句显式处理默认分支 |
| `EmptyBlock` | error | 避免 try、finally、if、else 和 switch 中出现无内容的代码块 |

## 注释与 Javadoc

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `TrailingComment` | warning | 提示将说明性注释追加在代码行尾的写法，便于把说明放在代码上方 |
| `TodoComment#noCommentDivider` | warning | 禁止用只有等号组成的注释行充当分隔线 |
| `TodoComment#todoMarker` | info | 提示将待办注释统一写成 TODO: 形式 |
| `MissingJavadocType` | warning | 要求具名类型具有 Javadoc，便于说明类型职责 |
| `JavadocType` | warning | 类型标签完整，顶层类型需要非空作者，内部类型沿用外层作者策略 |
| `MissingJavadocMethod` | warning | 公开方法和构造器需要 Javadoc，Override、JUnit 测试注解和 Operation 有例外 |
| `JavadocMethod` | error | 核对已有方法文档中的参数与返回值标签 |
| `InvalidJavadocPosition` | warning | 提示放在无效位置、无法正确关联声明的 Javadoc |
| `JavadocMissingLeadingAsterisk` | warning | 要求多行 Javadoc 的正文行保留行首星号 |
| `JavadocMissingWhitespaceAfterAsterisk` | warning | 要求 Javadoc 行首星号与后续文字之间有空白 |
| `JavadocLeadingAsteriskAlign` | warning | 让多行 Javadoc 的行首星号和结束标记垂直对齐 |
| `JavadocContentLocation` | warning | 让多行 Javadoc 正文从第二行开始，保持文档外观一致 |
| `JavadocParagraph` | warning | 检查文档段落之间的空行及 p 标签使用位置 |
| `SingleLineJavadoc` | warning | 限制单行 Javadoc 中使用块级标签，含此类标签时应拆成多行 |
| `NonEmptyAtclauseDescription` | error | 要求 Javadoc 块标签具有非空说明文字 |
| `RequireEmptyLineBeforeBlockTagGroup` | warning | 要求 Javadoc 正文与块标签组之间空一行 |
| `AtclauseOrder` | warning | 将方法相关 Javadoc 标签按 param、return、throws、deprecated 的相对顺序排列 |

## 项目角色与文档

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `projectTypeSuffix` | warning | 按包或已识别注解检查 Controller、Service、ServiceImpl、Mapper、DTO、VO、Converter、Config、Exception、Utils、Constants 后缀 |
| `roleInstanceName` | warning | 角色实例变量保留完整类型名，允许 primaryUserService、sourceUserDTO 等上下文前缀 |
| `fieldSchema` | error | DTO/VO 实例字段与 record 组件需要 Schema |
| `fieldBooleanPrefix` | error | DTO/VO 布尔字段省去 is 前缀 |
| `fieldJavadoc` | error | 普通实例字段需要多行且非空的 Javadoc |
| `typeSummary` | warning | 类型功能摘要恰好一行且非空 |
| `methodJavadocPeriod` | warning | 方法 Javadoc 行尾省去中英文句号 |
| `methodJavadocNumbering` | warning | 方法功能说明省去步骤编号 |

## 接口注解与路径

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `operationSummary` | error | Operation 需要非空 summary |
| `annotationText` | error | 检查 Schema 描述、校验消息、RateLimit.key 与 Log.value |
| `schemaRequiredMode` | error | 省略 Schema.requiredMode，必填状态交给校验约束 |
| `schemaDefaultExample` | error | DTO/VO 实例字段的标量默认值需要非空 Schema.example |
| `requestParamDefault` | error | 省略 RequestParam 中显式的 required=true，保留 required=false |
| `controllerOperation` | error | RestController 映射方法需要 Operation |
| `controllerJavadoc` | error | 映射方法统一使用 Operation，省去重复 Javadoc |
| `controllerApiPrefix` | error | 要求映射路径省略起始 /api 前缀，由服务 context-path 统一配置 |
| `controllerPath` | error | 静态路径片段采用 kebab-case，变量、配置占位符与通配片段保留例外 |

## 分层与转换

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `controllerMapper` | error | Controller 的声明类型避免 Mapper 依赖，包含数组和泛型边界 |
| `serviceHttp` | error | Service 的声明类型避免常见 Servlet、Session、WebRequest 与 ResponseEntity，包含数组和泛型边界 |
| `layerEntityReturn` | error | Controller 与 Service 返回类型避免 Entity，包含数组和泛型边界 |
| `beanUtilsCall` | error | 禁止 Spring 与 Commons BeanUtils.copyProperties 的全限定调用 |
| `converterFinal` | warning | 要求手写 Converter 工具类显式声明 final，限制无意义的继承 |
| `converterConstructor` | error | 要求手写 Converter 工具类显式声明私有构造器，阻止直接实例化 |

## 日志与切面

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `logDesensitize` | error | 业务 Log 显式引用敏感参数时启用脱敏 |
| `loggerRole` | error | DTO、VO、Entity、Mapper 和 Converter 省略 Slf4j |
| `aspectContract` | error | Aspect 同时声明 Component 和 Order |
| `loggingPlaceholder` | error | Slf4j 动态消息使用占位符参数 |

## 测试命名

| 检查模块／id | 等级 | 规则 |
| --- | --- | --- |
| `testMethodName` | warning | Test、ParameterizedTest、RepeatedTest 方法采用 method_Condition_ExpectedResult |

TestFactory 工厂方法沿用普通方法的小驼峰命名。

## 例外与豁免

- 跳过构建与生成目录，正常 `src/main/java` 和 `src/test/java` 源码保留在扫描范围。
- `src/test/java` 允许夹具使用魔法数字、重复字符串，并豁免普通字段文档、DTO/VO Schema 和布尔字段命名。
- 已列出的 Lombok、注入、ORM、序列化和测试注解可豁免未使用字段提示。
- 私有字段上的 `@SuppressWarnings("unused")` 豁免未使用字段提示，`unchecked` 等其他标签保持原有检查。
- 测试命名、Converter 和未使用字段的注解识别核对显式导入与全限定名，支持直接使用短名。
- 原生方法 Javadoc 检查的注解例外同时列出短名与全限定名，按源码中实际书写的名称匹配。
- 方法句号检查排除行内标签、pre/code 内容及 HTML 注释，每条规则对同一注释合并提示并定位到注释开头。
- 连续 Javadoc 采用紧邻声明的最后一份，字段与类型摘要中的纯空白和空 HTML 标签视为空说明。
- 可通过 `@SuppressWarnings("checkstyle:规则名或id")` 精确豁免语法检查，成对的 `CHECKSTYLE.OFF: 规则名或id` 与 `CHECKSTYLE.ON: 规则名或id` 支持局部豁免。
- 行宽建议支持单独成行的 `// CHECKSTYLE.OFF: LineLength` 与 `// CHECKSTYLE.ON: LineLength`，可用于 SQL、JSON 等长文本。
- 路径与 XPath 例外直接配置在同一份 XML 中。

## 文件结构

```text
checkstyle.xml
README.md
examples/
  GoodExample.java
  BadExample.java
  GoodConverter.java
  BadConverter.java
tests/
  RuleTests.java
  NativeRuleCases.java
  ProjectRuleCases.java
  BoundaryRuleCases.java
  RuleCase.java
references/
  README.md
  sun_checks.xml
  google_checks.xml
  NacosCheckStyle.xml
  LICENSE-Checkstyle
  LICENSE-Nacos
  NOTICE-Nacos
```

上游配置的来源与兼容性说明见 [参考规则](references/README.md)。

## 展示样例

使用 IDEA 的 Checkstyle 扫描操作检查 `examples` 中的文件。

| 文件 | 预期结果 |
| --- | --- |
| GoodExample.java | 零提示 |
| GoodConverter.java | 零提示 |
| BadExample.java | 12 项提示 |
| BadConverter.java | 2 项提示 |

四份展示样例只使用 JDK 标准库，采用一层 `examples` 包，违规写法旁标注检查意图。

## 自动测试

`tests/RuleTests.java` 是统一运行入口，负责加载 XML、执行检查、核对过滤器和验证 README。用例按职责拆分。

- `NativeRuleCases.java` 放置 Checkstyle 内置规则的基础正反例。
- `ProjectRuleCases.java` 放置项目角色、注解、接口与分层约定的基础正反例。
- `BoundaryRuleCases.java` 放置规则分支、语法变体和误报边界用例。
- `RuleCase.java` 定义共用用例结构和源码包装方法。

测试加载 XML 中的实际配置，并使用用例中单独声明的预期结果逐项验证。

正反例片段按目标规则判断合规性，完整项目样例按全量规则检查。

- 124 个检查模块均有合规与违规输入，分别验证单模块结果和完整 XML 中的结果。
- 每组用例明确声明预期严重级别，核对 XML 和实际诊断，发现等级变化、缺失或多余模块时失败。
- 14 个过滤及辅助模块验证豁免生效范围，包含测试目录、生成目录、导入分组、框架字段和命名豁免。
- 附加边界覆盖同名注解、全限定引用、record、嵌套泛型、字符串拼接、Unicode、八进制、文本块和 Javadoc HTML。
- 四份展示样例逐项核对诊断模块与严重级别。
- 五份完整合规源码覆盖 Controller、Service、DTO record、JUnit 和 Override，全量扫描要求零提示。
- 核对单行规则注释、XML 与 README 的版本、模块清单和测试文件说明。
- README 中的本地脚本路径必须实际存在，版本引用限定为当前规则版本和引擎版本。

在本目录使用 JDK 21 或更高版本运行，命令中的运行库路径替换为本机路径。

```bash
(
    checkstyle_jar=/path/to/checkstyle-14.4.0-all.jar
    test_classes=$(mktemp -d)
    trap 'rm -rf "$test_classes"' EXIT
    javac --release 21 -Xlint:all -Werror -cp "$checkstyle_jar" -d "$test_classes" tests/*.java &&
        java -cp "$checkstyle_jar:$test_classes" tests.RuleTests
)
```

标准运行库可从 Checkstyle 官方发布页取得。[Checkstyle 14.4.0](https://github.com/checkstyle/checkstyle/releases/tag/checkstyle-14.4.0)

测试类编译到临时目录，退出时清理。测试片段由 Checkstyle 解析，框架类型使用源码中的名称和导入参与检查。临时源码在运行结束后清理，断言失败时程序返回非零状态。

## 检查范围

本配置处理 Java 源码的形式与直接结构约定。未知常量和需要跨文件类型信息的表达式采用保守处理。异常标签验证说明文字是否为空，异常类型继承和各个抛出点的文档覆盖需代码审阅。重复且未使用的导入可能收到原生检查的多条提示。复杂 HTML 与 Javadoc 正文中直接书写的反斜杠 n、r 存在表示歧义，代码标签中的示例由排除逻辑保护。
