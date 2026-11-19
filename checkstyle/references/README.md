# Checkstyle 参考规则

本目录保存三份上游配置的原文。

| 文件 | 来源 |
| --- | --- |
| `sun_checks.xml`、`google_checks.xml` | [Checkstyle 14.4.0 内置配置](https://github.com/checkstyle/checkstyle/tree/checkstyle-14.4.0/src/main/resources) |
| `NacosCheckStyle.xml` | [Nacos `49f645b` 的配置](https://github.com/alibaba/nacos/blob/49f645b28a8cbfc6cd63f40359fe2c0401d953b1/style/NacosCheckStyle.xml) |

Nacos 配置的 `javadocTokens` 包含 `PARAM_LITERAL`。版权和许可见 [第三方来源](../../THIRD_PARTY.md)。

个人规则的导入入口为 [checkstyle.xml](../checkstyle.xml)，启用项、严重等级和运行方法见 [规则说明](../README.md)。
