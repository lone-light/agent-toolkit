# Java 检查规则清单

[通用规则](#通用规则) · [项目约定](#项目约定) · [接口注解](#接口注解) · [过滤与局部豁免](#过滤与局部豁免)

[通用配置](config/lonelight-base.xml) 定义 94 项检查，[项目配置](config/lonelight-project.xml) 定义 17 项检查。表格逐项列出模块名或 ID、严重等级和具体约束。

构建和导入步骤见 [README](README.md)，正反例见 [测试项目](test-project/README.md)。

## 通用规则

使用 4 空格缩进，行宽建议为 120 字符。导入按 IDEA 默认布局排列，依次是其他导入、`javax`、`java` 和静态导入。除 `javax` 与 `java` 连续排列外，其余组间留一空行。Checkstyle 检查顺序和空行，IDEA 的「优化导入」可整理导入。

### 文件结构与命名

泛型参数名可用 `T`、`T1` 或 `RequestT`。同一命名规则适用于类、接口、方法和 record 的类型参数。

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `FileTabCharacter` | warning | 禁止 Tab 字符 |
| `NewlineAtEndOfFile` | warning | 非空文件末尾保留换行 |
| `LineLength` | info | 提示超过 120 字符的行 |
| `RegexpSingleline#noTrailingWhitespace` | warning | 禁止行尾空白 |
| `OuterTypeFilename` | error | 顶层类型名与文件名一致 |
| `PackageDeclaration` | error | 要求包声明存在并与源码所在目录对应 |
| `OneTopLevelClass` | warning | 每个文件声明一个顶层类型 |
| `NoLineWrap` | warning | 让 package、各类 import 和模块声明头保持在同一行 |
| `ModuleDirectiveOrder` | warning | 按 requires、exports、opens、uses、provides 排列 Java 模块指令 |
| `PackageName` | warning | 统一包名为小写分段形式 |
| `TypeName` | warning | 统一具名类型使用大驼峰名称 |
| `MethodName#productionMethodName` | warning | 统一普通方法使用以小写字母开头的驼峰名称 |
| `MemberName` | warning | 统一普通实例字段使用小驼峰名称 |
| `StaticVariableName` | warning | 统一可变静态字段使用小驼峰名称 |
| `ParameterName` | warning | 统一方法和构造方法参数使用小驼峰名称 |
| `LocalVariableName` | warning | 统一普通局部变量使用小驼峰名称 |
| `LocalFinalVariableName` | warning | 检查 final 局部变量和资源变量，catch 参数由 CatchParameterName 检查 |
| `PatternVariableName` | warning | 模式匹配变量使用小驼峰 |
| `LambdaParameterName` | warning | Lambda 参数使用小驼峰 |
| `CatchParameterName` | warning | 异常捕获参数使用小驼峰 |
| `RecordComponentName` | warning | record 组件使用小驼峰 |
| `ConstantName` | warning | 统一常量字段使用大写字母、数字和下划线 |
| `ClassTypeParameterName` | warning | 统一类泛型参数的命名方式 |
| `InterfaceTypeParameterName` | warning | 统一接口泛型参数的命名方式 |
| `MethodTypeParameterName` | warning | 统一方法和构造方法泛型参数的命名方式 |
| `RecordTypeParameterName` | warning | 统一 record 泛型参数的命名方式 |

### 导入、排版与声明

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `UnusedImports` | warning | 报告未使用的显式导入 |
| `RedundantImport` | warning | 报告重复导入、同包导入和其他可由语法确定的冗余导入 |
| `IllegalImport` | error | 禁止使用 JDK 内部实现包 |
| `CustomImportOrder` | warning | 按其他包、javax、java、静态导入分组并检查组内排序 |
| `imports.ImportGroupSpacingCheck` | warning | 检查导入组间空行，组间有注释时跳过 |
| `Indentation` | warning | 统一 Java 语法结构的缩进层级 |
| `NeedBraces` | error | 要求 if、else、for、while 和 do 的受控语句使用花括号 |
| `LeftCurly` | warning | 左花括号采用行尾风格 |
| `RightCurly` | warning | 统一 if/else 和 try/catch/finally 的右花括号及后续关键字位置 |
| `WhitespaceAfter` | warning | 检查逗号、分号、类型转换及指定关键字之后的必要空白 |
| `WhitespaceAround` | warning | 检查赋值、运算符、控制流关键字和花括号周围的空白 |
| `WhitespaceBeforeEmptyBody` | warning | 要求空方法、空类型和其他空体的左花括号前保留空白 |
| `GenericWhitespace` | warning | 检查泛型尖括号周围的空白 |
| `SingleSpaceSeparator` | warning | 限制同一行相邻语法元素之间出现多个分隔空格 |
| `MethodParamPad` | warning | 要求方法名、构造调用等名称与随后的左圆括号直接相接 |
| `ParenPad` | warning | 去掉普通圆括号内侧的多余空格 |
| `TypecastParenPad` | warning | 去掉强制类型转换圆括号内侧的多余空格 |
| `EmptyForIteratorPad` | warning | 统一传统 for 中省略迭代表达式时的空白 |
| `NoWhitespaceBefore` | warning | 去掉逗号、分号、后置递增递减、可变参数标记及标签冒号前的多余空白 |
| `NoWhitespaceBeforeCaseDefaultColon` | warning | 去掉传统 switch 中 case 和 default 冒号前的多余空格 |
| `EmptyLineSeparator` | warning | 用空行分隔包、导入、类型、方法、构造器和初始化块等结构 |
| `OneStatementPerLine` | warning | 每行书写一条语句 |
| `MultipleVariableDeclarations` | warning | 每个变量单独声明 |
| `ArrayTypeStyle` | warning | 统一采用 String[] names 形式的 Java 数组声明风格 |
| `UpperEll` | warning | `long` 字面量使用大写 `L` 后缀 |
| `ModifierOrder` | warning | 让 Java 修饰符遵循语言规范建议顺序，注解置于修饰符之前 |
| `RedundantModifier` | info | 提示语言语义中已经隐含的多余修饰符 |
| `DeclarationOrder` | warning | 检查静态字段、实例字段、构造器和方法的大类排列 |
| `VisibilityModifier` | warning | 约束字段可见性，普通字段默认采用 private 封装 |
| `VariableDeclarationUsageDistance` | info | 提示声明与首次使用相距较远的局部变量，帮助缩小阅读范围 |
| `MissingOverrideOnRecordAccessor` | warning | 要求显式编写的 record 组件访问器标注 @Override |
| `HideUtilityClassConstructor` | warning | 限制纯静态工具类构造器的可见性 |
| `FinalClass` | warning | 提示只有私有构造器等适合封闭继承的类显式声明 final |

### 代码正确性与简化

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `NoFinalizer` | error | 禁止声明无参 `finalize` 方法 |
| `EmptyStatement` | error | 报告无意保留的空语句，防止循环或条件后多出的分号改变行为 |
| `InnerAssignment` | warning | 提示子表达式中的赋值 |
| `EqualsHashCode` | error | 要求 equals 与 hashCode 成对覆写，维护散列集合使用契约 |
| `StringLiteralEquality` | error | 报告使用相等或不等运算符比较字符串字面量的写法 |
| `CovariantEquals` | error | 识别只定义 equals(本类型) 而缺少 equals(Object) 的错误重载 |
| `ModifiedControlVariable` | error | 检查循环体内对控制变量的修改 |
| `AvoidDoubleBraceInitialization` | warning | 提示通过匿名类加实例初始化块完成的双大括号初始化 |
| `SimplifyBooleanExpression` | info | 提示和 true、false 组合后可直接简化的布尔表达式 |
| `SimplifyBooleanReturn` | info | 提示 if/else 分别返回 true 和 false 时可直接返回条件 |
| `UnusedLocalVariable` | warning | 报告未读取的局部变量 |
| `UnusedPrivateField` | warning | 报告未读取的 `private` 字段 |
| `EmptyCatchBlock` | error | 要求空 catch 至少写明忽略异常的原因 |
| `FallThrough` | error | 报告传统 switch 中缺少终止语句且没有贯穿说明的分支 |
| `DefaultComesLast` | warning | 要求已有的 default 分支位于 switch 最后 |
| `MissingSwitchDefault` | error | 要求传统 switch 语句显式处理默认分支 |
| `EmptyBlock` | error | 避免 try、finally、if、else 和 switch 中出现无内容的代码块 |

### 注解、注释与 Javadoc

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `AnnotationLocation` | warning | 让类型、方法、构造器和字段的声明注解各占一行 |
| `imports.QualifiedAnnotationCheck` | warning | 无同名或通配导入歧义时，导入注解类型并使用短名 |
| `TrailingComment` | warning | 检查代码行尾的注释 |
| `TodoComment#noCommentDivider` | warning | 禁止用只有等号组成的注释行充当分隔线 |
| `TodoComment#todoMarker` | info | 提示将待办注释统一写成 TODO: 形式 |
| `MissingJavadocType` | warning | 公开类型应有职责说明 |
| `JavadocType` | warning | 检查已有类型文档的泛型参数标签 |
| `MissingJavadocMethod` | warning | 公开方法和构造器应有文档，框架例外由过滤器处理 |
| `JavadocMethod` | error | 核对方法参数与返回值标签，异常标签说明由块标签规则检查 |
| `InvalidJavadocPosition` | warning | 提示放在无效位置、无法正确关联声明的 Javadoc |
| `JavadocMissingLeadingAsterisk` | warning | 要求多行 Javadoc 的正文行保留行首星号 |
| `JavadocMissingWhitespaceAfterAsterisk` | warning | 要求 Javadoc 行首星号与后续文字之间有空白 |
| `JavadocLeadingAsteriskAlign` | warning | 让多行 Javadoc 的行首星号和结束标记垂直对齐 |
| `JavadocContentLocation` | warning | 多行 Javadoc 的正文从第二行开始 |
| `JavadocParagraph` | warning | 检查文档段落之间的空行及 p 标签使用位置 |
| `NonEmptyAtclauseDescription` | error | 检查 param、throws、exception、deprecated、since 的说明，方法返回值由 JavadocMethod 检查 |
| `RequireEmptyLineBeforeBlockTagGroup` | warning | 要求 Javadoc 正文与块标签组之间空一行 |
| `AtclauseOrder` | warning | 检查方法 Javadoc 标签是否按 param、return、throws、deprecated 的相对顺序排列 |

## 项目约定

| 检查模块或 id | 等级 | 约束 |
| --- | --- | --- |
| `MagicNumber` | warning | 提示含义不明确的数字字面量，鼓励使用有名称的常量 |
| `MultipleStringLiterals` | info | 提示同一文件中重复的字符串字面量 |
| `naming.VariableTypeNameCheck` | warning | 对象变量采用类型名小驼峰，可加上下文前缀 |
| `naming.TypeSuffixCheck` | warning | 仅检查显式配置的类型后缀映射，默认无映射 |
| `naming.TestMethodNameCheck` | warning | JUnit 方法采用 动作_条件_预期 的命名 |
| `naming.BooleanFieldNameCheck` | warning | 布尔实例字段和 record 组件省去 is 前缀 |
| `javadoc.MultilineJavadocCheck` | warning | 单句 Javadoc 也要展开，结束标记单独一行 |
| `javadoc.MethodJavadocPeriodCheck` | warning | 方法 Javadoc 正文行末省去句号，代码片段除外 |
| `javadoc.MethodJavadocNumberingCheck` | warning | 方法 Javadoc 功能说明省去步骤编号，代码片段除外 |
| `javadoc.FieldJavadocCheck` | warning | 实例字段和 record 组件提供说明，显式隐藏字段除外 |
| `framework.AnnotationAttributeCheck` | error | 标准校验注解填写非空 `message` |
| `framework.ControllerApiDocumentationCheck` | warning | 从 REST 方法的签名检查接口摘要、公开参数和请求响应模型的说明注解 |
| `framework.ControllerEntityReturnCheck` | warning | REST 映射返回可识别持久化类型时提示边界风险 |
| `framework.LayerDependencyCheck` | warning | Controller 避免持久层声明依赖，Service 避免 HTTP 声明依赖 |
| `IllegalImport#reflectionCopyImports` | error | 禁止普通或静态导入 Spring 和 Apache Commons 的 `BeanUtils` |
| `framework.BeanUtilsCallCheck` | error | 禁止用全限定类名调用 Spring 和 Apache Commons 的 `BeanUtils` |
| `framework.LoggingPlaceholderCheck` | warning | SLF4J 动态日志消息采用占位符 |

### 变量与类型名称

`VariableTypeNameCheck` 扫描显式对象类型的实例字段、参数、局部变量和 record 组件。基本类型、已配置的常用基础类型和容器、数组、真实泛型类型参数、`var` 推断和静态字段有例外。泛型对象本身仍受检查，例如 `Request<User> request`。

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

### 类型后缀

项目配置通过 `lonelight.typeSuffixMappings` 接收后缀映射。映射格式为 `全限定类型正则=后缀|后缀`，多组用逗号分隔。正则匹配整个类型名，量词中的逗号需要改写。属性留空时，后缀检查跳过所有类型。

例如在 IDEA 的 `Edit Properties` 中，将 `lonelight.typeSuffixMappings` 设为 `com\.example\.dto\..*=DTO|Request|Query`，即可检查 `com.example.dto` 下的类型。

### 文档和注解

项目约定要求传统 Javadoc 展开书写。单句说明也将 `/**`、正文和 `*/` 各放一行，按需添加 `@param` 和 `@return`。

- 方法 Javadoc 的句号、步骤编号检查会跳过 `{@code}`、`{@literal}`、HTML `code`、`pre` 和 HTML 注释。
- 实例字段说明可以用展开的 Javadoc 或非空 Schema 描述。record 组件也可以用类型 Javadoc 中对应的 `@param`。显式隐藏的 Schema 或 JsonIgnore 字段有例外。
- `AnnotationAttributeCheck` 默认检查 Jakarta 和 javax 校验注解是否提供 `message`。空白字符串、可确定为空白的字面量拼接和文本块均会报告。
- `attributes` 使用 `注解全限定名=属性`，例如 `com.example.Log=value,com.example.Log=category`。同一注解可配置多个属性，外层与嵌套注解分别检查。

### 接口注解

检查 Spring 的 HTTP 映射方法，覆盖 `@RestController`，以及带 `@ResponseBody` 或返回 `ResponseEntity`、`HttpEntity` 的 `@Controller` 接口。

| 位置 | 检查内容 |
| --- | --- |
| 接口方法 | 填写非空 `@Operation.summary` |
| 路径、查询、请求头、Cookie 等公开参数 | 填写 `@Parameter.description` 或 `ref`，支持方法上按绑定名称和位置声明的参数说明 |
| 请求体与表单模型 | 检查本地模型字段的 `@Schema.description`。无法读取模型的请求体用 Swagger 的 `@RequestBody.description` 或 `ref` 说明 |
| 返回模型 | 检查模型实例字段和 record 组件的 `@Schema.description`，无需为基础类型、文件或 `void` 额外加模型注解 |

规则沿泛型、数组、嵌套字段和父类查找同一源码根下的模型，按类型声明识别。字段缺少说明时，检查在 Controller 的参数或方法上报告模型名、字段和行号。同一 Controller 文件中的重复引用只报告一次。

框架注入参数、静态字段和明确隐藏的接口或字段跳过。只读字段只检查响应侧，只写字段只检查请求侧。`@Schema` 字段说明可写成常量，检查器不求值。`@ArraySchema` 可用 `arraySchema.description` 说明数组字段。

跨文件查找要求源码目录与包名一致。规则的覆盖范围限于本地模型、直接声明的 HTTP 映射、实例字段和 record 组件。模型改动后，重新扫描引用它的 Controller。

### 分层、转换和日志

- `ControllerEntityReturnCheck` 通过 `entityPattern` 识别 REST 返回值中的持久化类型，默认匹配全限定名中的 `.entity.` 包段。
- `LayerDependencyCheck` 检查字段、变量、参数、返回值和 record 组件的声明类型，覆盖数组元素与泛型参数。`controllerForbidden` 默认匹配 `.mapper.` 包，`serviceForbidden` 默认匹配 Servlet 类型和 `ResponseEntity`。
- 日志检查识别单文件中可定位的 SLF4J Logger 字段、参数、局部变量和 Lombok `@Slf4j`，跟踪同名变量遮蔽。检查直接 `trace/debug/info/warn/error` 消息中的动态拼接，支持显式 Marker 变量。

## 过滤与局部豁免

| 辅助模块 | 生效范围 |
| --- | --- |
| `SuppressWarningsHolder` 与 `SuppressWarningsFilter` | 两份配置均支持按检查名称或 id 声明局部豁免 |
| `SuppressionCommentFilter` | 成对的 `CHECKSTYLE.OFF` 和 `CHECKSTYLE.ON` 标记，约束指定检查 |
| `SuppressWithPlainTextCommentFilter` | 通用配置中的 LineLength 成对豁免 |
| `BeforeExecutionExclusionFileFilter` | 跳过构建生成目录，保留源码目录 |
| `SuppressionSingleFilter` | 项目约定中放宽测试目录的 MagicNumber、重复字符串、字段说明和接口注解 |
| `FrameworkMethodFilter` | 通用配置中处理实际 JUnit、Override 和 HTTP 映射方法的例外 |
| `FrameworkFieldUsageFilter` | 通用配置中按明确的 Lombok、注入、ORM、Jackson、测试注解与 unused 标签处理字段读取例外 |

```java
@SuppressWarnings("checkstyle:VariableTypeName")
UserDTO request;
```

字段过滤器的 `fieldAnnotations` 与 `typeAnnotations` 可以独立配置。`@SuppressWarnings("unchecked")` 和单独的 `@Getter(AccessLevel.NONE)` 不构成未使用字段的豁免。

## 分析边界

自定义检查以 Java 语法树为依据。注解优先匹配全限定名和显式导入，接口注解检查也识别已知框架包的通配导入。同名类型用显式导入消除歧义。
