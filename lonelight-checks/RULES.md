# Java 检查规则清单

[1. 通用规则](#1-通用规则) · [2. 项目约定](#2-项目约定) · [3. 检查范围与豁免](#3-检查范围与豁免)

[通用配置](config/lonelight-base.xml) 定义 98 项检查，[项目配置](config/lonelight-project.xml) 定义 21 项检查。表格列出检查模块、等级和触发条件。

构建和导入步骤见 [README](README.md)，正反例见 [测试项目](test-project/README.md)。

## 1. 通用规则

### 1.1 文件结构与命名

表中的小驼峰命名要求以小写英文字母开头，其余位置可用英文字母和数字。泛型类型参数采用同一套命名规则，例如 `T`、`T1`、`RequestT`。

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `FileTabCharacter` | warning | 禁止 Tab 字符 |
| `NewlineAtEndOfFile` | warning | 非空文件末尾保留换行 |
| `LineLength` | info | 提示超过 120 字符的行，跳过以 `package` 或 `import` 开头的声明行，以及含 `http://`、`https://` 或 `ftp://` 的行 |
| `RegexpSingleline#noTrailingWhitespace` | warning | 禁止行尾空白 |
| `OuterTypeFilename` | error | 检查首个顶层类型的名称是否与文件名一致，后续存在公开顶层类型时跳过 |
| `PackageDeclaration` | error | 检查包声明及其与源码目录的对应关系，模块描述文件和紧凑源文件除外 |
| `OneTopLevelClass` | warning | 每个文件最多声明一个顶层类型 |
| `NoLineWrap` | warning | `package`、`import` 和模块声明头各自保持在一行内 |
| `ModuleDirectiveOrder` | warning | 按 requires、exports、opens、uses、provides 排列 Java 模块指令 |
| `PackageName` | warning | 首段仅用小写英文字母，后续各段以小写英文字母开头，后面可用小写英文字母和数字 |
| `TypeName` | warning | 具名类型名以大写英文字母开头，其余位置可用英文字母和数字 |
| `MethodName#productionMethodName` | warning | 统一普通方法使用以小写字母开头的驼峰名称 |
| `MemberName` | warning | 统一普通实例字段使用小驼峰名称 |
| `StaticVariableName` | warning | 统一可变静态字段使用小驼峰名称 |
| `ParameterName` | warning | 统一方法和构造方法参数使用小驼峰名称 |
| `LocalVariableName` | warning | 统一普通局部变量使用小驼峰名称 |
| `LocalFinalVariableName` | warning | `final` 局部变量和 `try` 的资源变量使用小驼峰 |
| `PatternVariableName` | warning | 模式匹配变量使用小驼峰 |
| `LambdaParameterName` | warning | Lambda 参数使用小驼峰 |
| `CatchParameterName` | warning | 异常捕获参数使用小驼峰 |
| `RecordComponentName` | warning | record 组件使用小驼峰 |
| `ConstantName` | warning | 常量名以大写英文字母开头，由大写字母和数字组成，可用单个下划线分段 |
| `ClassTypeParameterName` | warning | 统一类泛型参数的命名方式 |
| `InterfaceTypeParameterName` | warning | 统一接口泛型参数的命名方式 |
| `MethodTypeParameterName` | warning | 统一方法和构造方法泛型参数的命名方式 |
| `RecordTypeParameterName` | warning | 统一 record 泛型参数的命名方式 |

### 1.2 导入、排版与声明

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `UnusedImports` | warning | 报告未使用的显式导入 |
| `RedundantImport` | warning | 报告重复导入、同包导入和其他可由语法确定的冗余导入 |
| `IllegalImport` | error | 禁止导入 `sun.*` 和 `jdk.internal.*` 下的类型或成员 |
| `CustomImportOrder` | warning | 导入按其他导入、`javax`、`java`、静态导入的顺序排列，组内按字母排序 |
| `imports.ImportGroupSpacingCheck` | warning | `javax` 与 `java` 组之间无空行，其余相邻组间留一空行。两组间有注释等非空白行时跳过 |
| `Indentation` | warning | 基本缩进为 4 空格，数组初始化和注解数组初始化的缩进为 8 空格 |
| `NeedBraces` | error | 要求 if、else、for、while 和 do 的受控语句使用花括号 |
| `LeftCurly` | warning | 左花括号放在语句或声明行的行尾。只含常量的单行枚举允许左花括号后不换行 |
| `RightCurly` | warning | 统一 if/else 和 try/catch/finally 的右花括号及后续关键字位置 |
| `WhitespaceAfter` | warning | 检查逗号、分号、类型转换及指定关键字之后的必要空白 |
| `WhitespaceAround` | warning | 检查运算符、控制流关键字和花括号周围的空白。空构造器、空方法、空类型和空 Lambda 允许紧贴的 `{}`，只含常量的单行枚举允许常量紧贴花括号内侧 |
| `WhitespaceBeforeEmptyBody` | warning | 要求空方法、空类型和其他空体的左花括号前保留空白 |
| `GenericWhitespace` | warning | 检查泛型尖括号周围的空白 |
| `SingleSpaceSeparator` | warning | 限制同一行相邻语法元素之间出现多个分隔空格 |
| `MethodParamPad` | warning | 要求方法名、构造调用等名称与随后的左圆括号直接相接 |
| `ParenPad` | warning | 去掉普通圆括号内侧的多余空格 |
| `TypecastParenPad` | warning | 去掉强制类型转换圆括号内侧的多余空格 |
| `EmptyForIteratorPad` | warning | `for` 省略迭代表达式时，末尾分号与同行右括号之间留空格 |
| `NoWhitespaceBefore` | warning | 去掉逗号、分号、后置递增递减、可变参数标记及标签冒号前的多余空白 |
| `NoWhitespaceBeforeCaseDefaultColon` | warning | 去掉传统 switch 中 case 和 default 冒号前的多余空格 |
| `EmptyLineSeparator` | warning | 用空行分隔包声明、导入、类型、方法、构造器和初始化块等结构。相邻字段可连续排列，允许多个空行 |
| `OneStatementPerLine` | warning | 每行书写一条语句 |
| `MultipleVariableDeclarations` | warning | 每个变量单独声明 |
| `ArrayTypeStyle` | warning | 统一采用 String[] names 形式的 Java 数组声明风格 |
| `UpperEll` | warning | `long` 字面量使用大写 `L` 后缀 |
| `ModifierOrder` | warning | 让 Java 修饰符遵循语言规范建议顺序，注解置于修饰符之前 |
| `RedundantModifier` | info | 提示语言语义中已经隐含的多余修饰符 |
| `DeclarationOrder` | warning | 依次排列静态字段、实例字段、构造器和方法。同一类成员的可见性顺序不限 |
| `VisibilityModifier` | warning | 普通字段采用 private 封装，带 JUnit `@Rule`、`@ClassRule`、`@TempDir` 或 Guava `@VisibleForTesting` 的字段除外 |
| `VariableDeclarationUsageDistance` | info | 局部变量声明与首次使用的距离超过 12 时提示，默认跳过 `final` 变量 |
| `MissingOverrideOnRecordAccessor` | warning | 要求显式编写的 record 组件访问器标注 @Override |
| `HideUtilityClassConstructor` | warning | 报告工具类的隐式默认构造器和公开构造器，跳过带有 XML 所列 Spring 注解的类 |
| `FinalClass` | warning | 提示只有私有构造器、且当前文件中找不到子类的非抽象类声明 `final` |

### 1.3 代码正确性与简化

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `NoFinalizer` | error | 禁止声明无参 `finalize` 方法 |
| `concurrent.ExecutorFactoryCheck` | warning | 报告 `Executors` 的 `newFixedThreadPool`、`newSingleThreadExecutor`、`newCachedThreadPool`、`newScheduledThreadPool` 和 `newSingleThreadScheduledExecutor` |
| `numeric.BigDecimalFloatingConstructorCheck` | warning | 报告 `BigDecimal` 构造器首个参数可确认是浮点值的调用 |
| `EmptyStatement` | error | 报告单独作为语句的分号，包括空的循环体或条件分支 |
| `InnerAssignment` | warning | 提示子表达式中的赋值 |
| `EqualsHashCode` | error | `equals(Object)` 与 `hashCode()` 成对覆写 |
| `StringLiteralEquality` | error | 报告使用相等或不等运算符比较字符串字面量的写法 |
| `CovariantEquals` | error | 报告只有窄类型参数的 `equals` 重载、缺少 `equals(Object)` 的类型 |
| `ModifiedControlVariable` | error | 检查循环体内对控制变量的修改 |
| `AvoidDoubleBraceInitialization` | warning | 提示通过匿名类加实例初始化块完成的双大括号初始化 |
| `collections.CollectionCapacityCheck` | info | `ArrayList`、`ArrayDeque`、`HashMap`、`HashSet`、`LinkedHashMap`、`LinkedHashSet`、`PriorityQueue` 和 `ConcurrentHashMap` 使用无参构造时提示预设容量 |
| `SimplifyBooleanExpression` | info | 提示和 true、false 组合后可直接简化的布尔表达式 |
| `SimplifyBooleanReturn` | info | `if/else` 分别返回 `true` 和 `false` 时，提示直接返回条件或其否定 |
| `UnusedLocalVariable` | warning | 报告未读取的局部变量 |
| `UnusedPrivateField` | warning | 报告未读取的 `private` 字段 |
| `EmptyCatchBlock` | error | 无语句的 `catch` 块需要有含非空白字符的注释 |
| `FallThrough` | error | 报告传统 `switch` 中有语句、会继续执行下一分支且缺少贯穿标记的分支 |
| `DefaultComesLast` | warning | 要求已有的 default 分支位于 switch 最后 |
| `MissingSwitchDefault` | error | `switch` 语句须有 `default` 分支，跳过 `switch` 表达式及含模式匹配或 `case null` 的语句 |
| `EmptyBlock` | error | `try`、`finally`、`if`、`else`、`switch` 的块内须有代码或注释 |

能预估数量时，列表可用 `new ArrayList<>(expectedSize)`，JDK 21 的哈希映射可用 `HashMap.newHashMap(expectedSize)`。容量参数、复制构造和工厂调用均通过此项检查。容量检查跳过匿名子类，双大括号初始化由 `AvoidDoubleBraceInitialization` 报告。[阿里巴巴集合规约](https://github.com/alibaba/p3c/blob/master/p3c-pmd/README.md#collection) · [JDK 21 HashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/HashMap.html#newHashMap(int))

线程池规则识别普通导入、全限定调用、可确认的静态导入和方法引用。传统线程池可按负载显式配置线程数与队列。`newVirtualThreadPerTaskExecutor()` 跳过此项检查。[阿里巴巴并发规约](https://github.com/alibaba/p3c/blob/master/p3c-pmd/README.md#concurrency) · [JDK 21 Executors](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Executors.html)

十进制数规则识别浮点字面量和强转、同一文件中显式声明的浮点变量，以及操作数类别均可确认的 `+`、`-`、`*`、`/`、`%` 表达式。数值类别无法确认时跳过。十进制输入可直接用字符串构造，已有 `double` 值可用 `BigDecimal.valueOf`。[JDK 21 BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

### 1.4 注解、注释与 Javadoc

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `AnnotationLocation` | warning | 类型、方法、普通构造器和变量声明上的注解各占一行，缩进与声明对齐 |
| `imports.QualifiedAnnotationCheck` | warning | 无同名或通配导入歧义时，导入注解类型并使用短名 |
| `TrailingComment` | warning | 检查代码行尾的注释 |
| `TodoComment#noCommentDivider` | warning | 报告只含三个或更多连续等号的注释内容，忽略首尾空格和 Tab |
| `TodoComment#todoMarker` | info | 注释内容以待办标记开头时，要求写成大写的 `TODO:` |
| `MissingDeprecated` | warning | `@Deprecated` 注解和 Javadoc `@deprecated` 成对出现，已有标签的说明由 `NonEmptyAtclauseDescription` 检查 |
| `MissingJavadocType` | warning | 检查公开类型是否有 Javadoc |
| `JavadocType` | warning | 核对已有类型 Javadoc 中与泛型参数、record 组件对应的 `@param` 标签 |
| `MissingJavadocMethod` | warning | 公开方法和构造器应有文档，框架例外由过滤器处理 |
| `JavadocMethod` | error | 核对已有方法和构造器 Javadoc 中的 `@param` 和 `@return` 标签，适用于各访问级别 |
| `InvalidJavadocPosition` | warning | 提示放在无效位置、无法正确关联声明的 Javadoc |
| `JavadocMissingLeadingAsterisk` | warning | 要求多行 Javadoc 的正文行保留行首星号 |
| `JavadocMissingWhitespaceAfterAsterisk` | warning | 要求 Javadoc 行首星号与后续文字之间有空白 |
| `JavadocLeadingAsteriskAlign` | warning | 让多行 Javadoc 的行首星号和结束标记垂直对齐 |
| `JavadocContentLocation` | warning | 多行 Javadoc 的正文从第二行开始 |
| `JavadocParagraph` | warning | 检查文档段落与 `<p>` 标签的位置。标签可独占一行，也可在空行后与下一段正文同行 |
| `NonEmptyAtclauseDescription` | error | 已有 `@param`、`@throws`、`@exception`、`@deprecated`、`@since` 标签须有说明 |
| `RequireEmptyLineBeforeBlockTagGroup` | warning | 要求 Javadoc 正文与块标签组之间空一行 |
| `AtclauseOrder` | warning | 检查类型、字段、方法和构造器的 Javadoc。出现 `@param`、`@return`、`@throws`、`@deprecated` 时，按此顺序排列 |

## 2. 项目约定

### 2.1 命名

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `naming.VariableTypeNameCheck` | warning | 对象变量名使用类型名的小驼峰形式，或小驼峰前缀加原类型名 |
| `naming.TypeSuffixCheck` | warning | 按配置检查类、接口、record 和枚举的名称后缀。默认无映射 |
| `naming.TestMethodNameCheck` | warning | JUnit 方法采用 动作_条件_预期 的命名 |
| `framework.BooleanFieldNameCheck` | warning | 报告 REST 请求或响应模型中以 `is` 加大写字母开头的 `boolean`、`Boolean` 实例字段和 record 组件，例如 `isReady` |

`VariableTypeNameCheck` 检查显式声明对象类型的实例字段、参数、局部变量和 record 组件。它跳过基本类型、数组、泛型类型参数（如 `T`）、`var`、静态字段和 `ignoredTypes` 中的类型。例如 `Request<User> request` 按 `Request` 检查。下表使用默认配置。

| 声明 | 结果 |
| --- | --- |
| `UserDTO userDTO` | 通过 |
| `UserDTO sourceUserDTO` | 通过 |
| `UserDTO abcUserDTO` | 通过，按字符形式判断前缀 |
| `UserDTO request` | warning |
| `ObjectMapper mapper` | warning |
| `ObjectMapper objectMapper` | 通过 |

`ignoredTypes` 使用全限定类型名，`aliases` 使用 `全限定类型名=变量名`。`allowPrefixes=false` 关闭前缀匹配。列表属性按完整列表配置。

```xml
<property name="aliases" value="com.fasterxml.jackson.databind.ObjectMapper=mapper" />
```

`lonelight.typeSuffixMappings` 接收 `全限定类型名正则=后缀|后缀` 格式的映射。逗号分隔映射，正则中的逗号也会被当作分隔符。正则匹配完整的类型名，命中的每条映射分别检查。属性留空时跳过后缀检查。

例如在 IDEA 的 `Edit Properties` 中，将 `lonelight.typeSuffixMappings` 设为 `com\.example\.dto\..*=DTO|Request|Query`，即可检查 `com.example.dto` 下的类型。

JUnit 方法名由下划线分隔为三段，每段使用英文字母和数字，第一段以小写字母开头，例如 `save_validOrder_returnsId`。

布尔属性检查从 Controller 的入参和出参找同一源码根下的模型，沿泛型、数组、父类和嵌套属性查找。检查跳过静态字段、数组字段、显式隐藏的模型或字段以及可识别的持久化实体。在 Controller 报告，并显示模型名、字段名和字段行号。

### 2.2 Javadoc

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `javadoc.MultilineJavadocCheck` | warning | 块式 Javadoc 至少占三行，`*/` 单独一行 |
| `javadoc.MethodJavadocPeriodCheck` | warning | 报告方法和构造器 Javadoc 正文行末的 `。` 或 `.` |
| `javadoc.MethodJavadocNumberingCheck` | warning | 报告方法和构造器 Javadoc 正文行首的步骤编号，例如 `1.`、`1、` 或 `一、` |
| `javadoc.FieldJavadocCheck` | warning | 实例字段和 record 组件提供说明，显式隐藏字段除外 |
| `javadoc.TypeJavadocCheck` | warning | 类型 Javadoc 的职责说明占一行，`@author` 填写非空内容。通用配置报告公开类型缺少 Javadoc，项目配置报告非公开类型缺少 Javadoc |

- 单句 Javadoc 的 `/**`、正文和 `*/` 各占一行。句号和步骤编号检查跳过 `{@code}`、`{@literal}`、HTML `code`、`pre` 和 HTML 注释。
- 字段说明可用非空 Javadoc 或 `@Schema.description`。record 组件也可用所属类型 Javadoc 中带说明的同名 `@param`。`@JsonIgnore` 启用时和 `@Schema(hidden = true)` 均可豁免字段说明检查。

### 2.3 校验注解

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `framework.AnnotationAttributeCheck` | error | 检查 XML 所列 Jakarta 和 `javax` 校验注解的 `message` |
| `framework.RequestValidationCheck` | warning | REST 请求参数的模型声明了可识别的 Bean Validation 约束或级联校验时，参数标注 `@Valid` 或 `@Validated` |

- 缺少 `message` 或静态可判定为空白时报告。检查范围包括字符串字面量、字面量拼接和文本块。常量引用按已填写处理。
- `attributes` 使用 `注解全限定名=属性`，例如 `com.example.Log=value,com.example.Log=category`。同一注解可配置多个属性，外层与嵌套注解分别检查。
- 请求校验检查 `@RequestBody`、`@ModelAttribute`、`@RequestPart` 和可解析的隐式对象参数。检查模型及其父类型、实例字段、record 组件和 getter 上的约束。支持标准约束和能定位到源码的 `@Constraint` 自定义注解。集合、数组和框架注入参数跳过。[Spring MVC 校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

### 2.4 接口注解

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `framework.ControllerApiDocumentationCheck` | warning | 从 REST 方法的签名检查接口摘要、公开参数和请求响应模型的说明注解 |

检查 Spring 的 HTTP 映射方法。涵盖 `@RestController` 中的映射，以及 `@Controller` 中具有类级或方法级 `@ResponseBody`、或声明返回类型为 `ResponseEntity`、`HttpEntity` 的映射。

| 位置 | 检查内容 |
| --- | --- |
| 接口方法 | 填写非空 `@Operation.summary` |
| 路径、查询、请求头、Cookie 和矩阵参数 | 填写 `@Parameter.description` 或 `ref`。方法级说明按参数绑定名称和位置匹配，位置也可省略 |
| 隐式查询参数 | 可识别的标量、枚举及部分容器类型按查询参数检查，例如 `String`、`String[]`、`List<String>`、`Optional<Integer>` |
| 请求体 `@RequestBody`、表单 `@ModelAttribute` 和上传分段 `@RequestPart` | 检查可解析模型的字段说明。请求体无可解析模型时检查 Swagger 的 `@RequestBody.description` 或 `ref`，上传分段无可解析模型时检查 `@Parameter` |
| 响应 | 检查可解析返回模型的实例字段和 record 组件 |

请求和响应模型的字段说明可写在 `@Schema.description` 或 `@ArraySchema` 内嵌的 `arraySchema.description`。检查器沿泛型、数组、嵌套字段和父类查找声明。跨文件查找限于同一源码根，要求包名与目录对应。字段缺少说明时，检查结果定位到 Controller 的参数或方法，并列出模型名、字段名和字段行号。同一 Controller 文件中同一字段只报告一次。

检查跳过可识别的框架注入参数、静态字段和显式隐藏的接口、参数或字段。只读字段只检查响应侧，只写字段只检查请求侧。说明属性使用常量引用时按已填写处理。

### 2.5 分层与对象转换

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `framework.ControllerEntityReturnCheck` | warning | 报告 REST 方法返回类型中命中 `entityPattern` 的类型，默认匹配全限定名中的 `.entity.` 包段 |
| `framework.LayerDependencyCheck` | warning | 报告 Controller 中的 `.mapper.` 声明类型，以及 Service 中的 Servlet 或 `ResponseEntity` 声明类型 |
| `framework.ConstructorInjectionCheck` | warning | 报告实例字段上的 `@Autowired`、`@Inject` 和 `@Resource`，以及可识别的构造器依赖未保存到 `private final` 字段的情况 |
| `framework.EntityBuilderCheck` | warning | 在实体声明之外直接 `new` 已公开标准 `builder()` 的持久化实体时提示 |
| `IllegalImport#reflectionCopyImports` | error | 禁止普通或静态导入 Spring 和 Apache Commons 的 `BeanUtils` |
| `framework.BeanUtilsCallCheck` | error | 禁止用全限定类名调用 Spring 和 Apache Commons 的 `BeanUtils` |

两项分层检查都读取声明类型，包括数组元素和泛型参数。`LayerDependencyCheck` 检查字段、局部变量、参数、返回值和 record 组件。Controller 和 Service 的类型限制分别通过 `controllerForbidden` 和 `serviceForbidden` 配置。

构造器检查支持明确标注的注入构造器、可识别 Spring 组件中唯一的手写构造器，以及 `@RequiredArgsConstructor` 覆盖的未初始化 `final` 或 `@NonNull` 字段。手写构造器检查直接将参数赋给字段的语句。同一字段同时使用字段注入和构造器赋值时，优先报告字段注入。`@Value` 配置值可单独注入。[Lombok 构造器](https://projectlombok.org/features/constructor) · [Spring 构造器注入](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)

持久化实体按 `.entity.` 包段或已知持久化注解识别，包括 JPA `@Entity`、MyBatis-Plus `@TableName`、Spring Data `@Table` 和 `@Document`。构建器检查识别公开的无参静态 `builder()`，以及类型或构造器上的 Lombok `@Builder` 和类型上的 `@SuperBuilder`。检查跳过实体内部实现和匿名子类。

### 2.6 字面量

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `MagicNumber` | warning | 报告未用于常量定义的数字字面量，跳过 `-1`、`0`、`1`、`2`，以及注解、注解属性默认值和 `hashCode()` 中的数字 |
| `MultipleStringLiterals` | info | 同一文件中相同的字符串字面量出现两次及以上时提示，跳过注解中的字面量和引号内仅零个或一个源码字符的字面量 |

### 2.7 日志

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `framework.LoggingPlaceholderCheck` | warning | 报告 SLF4J 消息参数中含字符串字面量和标识符的 `+` 拼接 |

检查 `trace`、`debug`、`info`、`warn`、`error` 的直接调用。识别当前文件中的 SLF4J `Logger` 字段、参数、局部变量及 Lombok `@Slf4j` 生成的 `log`，并按变量作用域处理同名声明。首个实参是可识别的 `Marker` 时，检查下一个实参。

## 3. 检查范围与豁免

### 3.1 源码分析范围

两份配置都扫描 `.java` 文件。自定义检查使用 Java 语法树，依据包声明、导入和源码中的类型声明识别名称。接口注解检查识别直接声明在方法上的标准 Spring 映射注解。Spring 映射、JUnit 测试方法和配置中的校验注解均支持普通导入及通配导入。

### 3.2 过滤与局部豁免

| 辅助模块 | 生效范围 |
| --- | --- |
| `CompactEnumSpacingFilter` | 只含常量的单行枚举豁免花括号内侧的空格和换行检查 |
| `JavadocParagraphSpacingFilter` | 独占一行的 `<p>` 标签可直接分隔正文 |
| `SuppressWarningsHolder` 与 `SuppressWarningsFilter` | 两份配置均支持按检查名称或 id 声明局部豁免 |
| `SuppressionCommentFilter` | 两份配置均支持用成对的 `CHECKSTYLE.OFF` 和 `CHECKSTYLE.ON` 按检查名称局部豁免。通用配置也支持按 ID 匹配 |
| `SuppressWithPlainTextCommentFilter` | `// CHECKSTYLE.OFF: LineLength` 和 `// CHECKSTYLE.ON: LineLength` 之间豁免行宽检查 |
| `BeforeExecutionExclusionFileFilter` | 跳过路径中的 `target`、`build`、`generated-sources`、`generated-test-sources` 目录。路径含 `src/main/java` 或 `src/test/java` 的文件仍参与扫描 |
| `SuppressionSingleFilter` | 项目配置在 `src/test/java` 下豁免 `MagicNumber`、`MultipleStringLiterals`、`FieldJavadocCheck` 和 `ControllerApiDocumentationCheck` |
| `FrameworkMethodFilter` | JUnit 测试方法豁免 `MethodName`、`MissingJavadocMethod` 和 `JavadocMethod`。`@Override` 方法豁免 `MissingJavadocMethod` 和 `JavadocMethod`。HTTP 映射方法豁免 `MissingJavadocMethod` |
| `FrameworkFieldUsageFilter` | 使用配置中的框架注解或 `@SuppressWarnings("unused")` 豁免 `UnusedPrivateField` |

```java
@SuppressWarnings("checkstyle:VariableTypeName")
UserDTO request;
```

字段豁免使用 `fieldAnnotations`，所属类型的豁免使用 `typeAnnotations`。指定 `AccessLevel.NONE` 的 Lombok `@Getter` 或 `@Setter` 不提供豁免。
