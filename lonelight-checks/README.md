# Lonelight Checks

Java 源码检查规则 **0.1.0**。检查器用 JDK **21** 构建，使用 Checkstyle **14.4.0** 验证。

[完整规则清单](RULES.md) · [构建与验证](#构建与验证) · [在 IDEA 导入](#在-idea-导入) · [手动扫描样例](#手动扫描样例)

## 使用范围

| 文件 | 用途 | 检查模块 | error | warning | info |
| --- | --- | --- | --- | --- | --- |
| [lonelight-base.xml](config/lonelight-base.xml) | 通用 Java 风格和常见错误检查 | 98 | 16 | 75 | 7 |
| [lonelight-project.xml](config/lonelight-project.xml) | 命名、文档、依赖注入、分层和接口约定 | 21 | 3 | 17 | 1 |

两份配置共用一个检查器 JAR，可以单独或同时启用。严重等级由 XML 配置。

- `error` 用于明确的错误风险或启用后的强制策略。
- `warning` 用于风格、命名、文档和需要人工判断的边界提示。
- `info` 用于行宽、简化和重复文本建议。

## 目录

```text
lonelight-checks/
├── pom.xml                      检查器构建配置
├── RULES.md                     检查模块、等级与约束
├── config/                      通用规则和项目约定
├── src/main/java/               自定义检查和过滤器
├── src/main/resources/          检查提示
├── src/test/java/               自动测试
├── src/test/resources/fixtures/ 自动测试的扫描输入
└── test-project/                可编译的手动扫描样例
```

## 构建与验证

用 JDK 21 在 `lonelight-checks/` 执行构建。

```bash
mvn clean verify
```

产物为 `target/lonelight-checks-0.1.0.jar`，包含自定义检查、过滤器和提示文本。Checkstyle 引擎由 IDEA 插件或命令行提供。

自动测试验证规则的正反例和重复诊断。打包测试用生成的 JAR 扫描样例，核对全部 119 个模块的预期诊断与等级、完整正例的通过情况和自定义提示文本。

## 在 IDEA 导入

1. 打开设置中的 `工具 → Checkstyle → Checkstyle Project`。
2. 将 `Checkstyle version` 设为 `14.4.0`。
3. 在 `Third-Party Checks` 添加 `target/lonelight-checks-0.1.0.jar`。
4. 在 `Rules Files` 用 `+` 添加所需的 XML，然后勾选 `Active` 并应用。
5. 在 CheckStyle 工具窗口选择 `<active rules>`，扫描当前 Java 文件或选定目录。只想核对某一份配置时，在 `Rules` 下拉框直接选它。

按 XML 等级显示结果时，取消勾选 `Treat Checkstyle errors as warnings`。扫描测试源码时选择 `Only Java sources (including tests)`。

检查规则实现和测试代码时，可分别选中 `src/main/java` 和 `src/test/java` 扫描。测试资源里的 Java 反例用于验证规则行为。

如果验证 XML 时找不到 `lonelight.checks.*`，先确认 JAR 路径存在、项目已被 IDEA 标为受信任。更新 JAR 后，在 CheckStyle 工具窗口执行 `Reload Rules Files` 再扫描。[CheckStyle-IDEA 官方说明](https://github.com/jshiell/checkstyle-idea#configuration)

## 手动扫描样例

在 [test-project](test-project/README.md) 按主题选择可编译的样例，对照 IDEA 检查和 Checkstyle 结果。
