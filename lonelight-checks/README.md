# Lonelight Checks

Java 源码检查规则。检查器用 JDK **21** 构建，运行检查器的 JVM 至少为 Java 21，验证所用的 Checkstyle 版本为 **14.4.0**。

[完整规则清单](RULES.md) · [构建与验证](#构建与验证) · [在 IDEA 导入](#在-idea-导入) · [交付与接入](#交付与接入) · [手动扫描样例](#手动扫描样例)

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

产物为 `target/lonelight-checks-<版本>.jar`，版本见 [pom.xml](pom.xml)。JAR 包含两份 XML、自定义检查、过滤器和提示文本。Checkstyle 引擎由使用方提供。

自动测试验证规则的正反例和重复诊断。打包测试用生成的 JAR 扫描样例，核对全部 119 个模块的预期诊断与等级、完整正例的通过情况和自定义提示文本。

## 在 IDEA 导入

1. 打开设置中的 `工具 → Checkstyle → Checkstyle Project`。
2. 将 `Checkstyle version` 设为 `14.4.0`。
3. 在 `Third-Party Checks` 添加构建好的 JAR。项目须在 IDEA 中受信任。
4. 在 `Rules Files` 用 `+` 按类路径添加所需规则，路径见下表。勾选 `Active` 并应用。
5. 在 CheckStyle 工具窗口选择 `<active rules>`，扫描当前 Java 文件或选定目录。只想核对某一份配置时，在 `Rules` 下拉框直接选它。

按 XML 等级显示结果时，取消勾选 `Treat Checkstyle errors as warnings`。扫描测试源码时选择 `Only Java sources (including tests)`。

| 配置 | JAR 内的类路径 |
| --- | --- |
| 通用规则 | `lonelight/checks/config/lonelight-base.xml` |
| 项目约定 | `lonelight/checks/config/lonelight-project.xml` |

检查规则实现和测试代码时，可分别选中 `src/main/java` 和 `src/test/java` 扫描。测试资源里的 Java 反例用于验证规则行为。

升级规则时，替换为新版本 JAR 并执行 `Reload Rules Files`。如果规则找不到 `lonelight.checks.*`，检查 JAR 路径和项目的信任状态。[CheckStyle-IDEA 官方说明](https://github.com/jshiell/checkstyle-idea#configuration)

## 交付与接入

Git 保存源码和 XML。发布时从版本标签构建，将 JAR 和 SHA-256 校验文件附到对应的 GitHub Release。使用方固定发布版本和 Checkstyle 引擎版本。[GitHub Release 说明](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)

- IDEA 从私有 Release 下载 JAR 后按上面的步骤导入。可匿名下载的 HTTPS JAR 也可以按版本 URL 添加。[CheckStyle-IDEA 版本说明](https://github.com/jshiell/checkstyle-idea/blob/main/CHANGELOG.md)
- CI 使用具备发布仓库读取权限的凭据下载私有 Release 的附件，并校验 SHA-256。[GitHub Release 附件](https://docs.github.com/en/rest/releases/assets#get-a-release-asset)
- Maven 项目可从 Maven 制品库把检查器加入 `maven-checkstyle-plugin` 的依赖，并显式指定 Checkstyle 引擎版本。`configLocation` 填写 JAR 内的类路径。两份规则都有启用需求时，分别配置两次执行。IDEA 从 Maven 导入一份插件级配置后，可以手动添加第二份规则。[自定义检查器接入](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/custom-developed-checkstyle.html) · [引擎版本设置](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html) · [IDEA Maven 配置导入](https://github.com/jshiell/checkstyle-idea/blob/26.20.0/src/main/java/org/infernus/idea/checkstyle/maven/MavenCheckstyleConfigurator.java#L505-L568)

## 手动扫描样例

在 [test-project](test-project/README.md) 按主题选择可编译的样例，对照 IDEA 检查和 Checkstyle 结果。
