# Agent Toolkit

Agent 规则、Skills 与工具配置集合。

## 内容

| 路径 | 用途 |
| --- | --- |
| [instructions/AGENTS.md](instructions/AGENTS.md) | 通用全局规则 |
| [skills/engineering-workflow](skills/engineering-workflow/) | 工程开发工作流 |
| [skills/grilling](skills/grilling/) | 需求澄清与追问 |
| [opencode](opencode/) | OpenCode TUI 配置与界面插件 |
| [dsh](dsh/) | DSH 插件清单与界面偏好 |

项目级规则与 Skills 随各自项目管理。第三方来源和许可证见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 工具配置

- OpenCode 配置适用于 v2；界面插件使用 OpenCode 提供的插件接口。
- DSH Profile 基于当前桌面端环境，插件版本由锁文件固定；内置 bundle 仍由 DSH 提供。私人 Provider、默认模型选择和登录凭据不收录，恢复后单独配置。
- `dsh/appearance/wallpaper-settings.json` 是外观设置片段，恢复时合并 `settings`，不整份覆盖壁纸插件配置；壁纸和头像资源另行选择。
