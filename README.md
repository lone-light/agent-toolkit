# Agent Toolkit

Agent 规则、Skills、代码检查规则与工具配置集合。

## 内容

| 路径 | 用途 |
| --- | --- |
| [instructions/AGENTS.md](instructions/AGENTS.md) | 通用全局规则 |
| [skills/engineering-workflow](skills/engineering-workflow/) | 工程开发工作流 |
| [skills/grilling](skills/grilling/) | 需求澄清与追问 |
| [skills/apifox-cli](skills/apifox-cli/) | Apifox CLI 通用入口 |
| [skills/apifox-cli-checkup](skills/apifox-cli-checkup/) | CLI 故障排查与版本确认 |
| [skills/apifox-branch](skills/apifox-branch/) | Apifox 分支与合并协作 |
| [skills/apifox-import-export](skills/apifox-import-export/) | 导入导出与导入质量检查 |
| [skills/apifox-test-case](skills/apifox-test-case/) | 单接口测试用例、数据与断言 |
| [skills/apifox-test-scenario](skills/apifox-test-scenario/) | 测试场景编排与变量衔接 |
| [skills/apifox-test-automation](skills/apifox-test-automation/) | 测试套件、执行、报告与 CI |
| [skills/apifox-workflow-api-lifecycle](skills/apifox-workflow-api-lifecycle/) | API 设计、Mock、测试与文档工作流 |
| [checkstyle](checkstyle/) | Java Checkstyle 规则、展示样例和回归测试 |
| [lonelight-checks](lonelight-checks/) | Java 通用规则、项目约定和测试样例 |
| [opencode](opencode/) | OpenCode TUI 配置与界面插件 |
| [dsh](dsh/) | DSH 插件清单与界面偏好 |

项目级规则与 Skills 随各自项目管理。第三方来源和许可证见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 工具配置

- Apifox Skills 依赖本机安装的 `apifox` CLI，通用入口为 `apifox-cli`，具体业务按需加载对应专项 Skill。
- OpenCode 配置适用于 v2；界面插件使用 OpenCode 提供的插件接口。
- DSH Profile 基于当前桌面端环境，插件版本由锁文件固定；内置 bundle 仍由 DSH 提供。私人 Provider、默认模型选择和登录凭据不收录，恢复后单独配置。
- `dsh/appearance/wallpaper-settings.json` 是外观设置片段，恢复时合并 `settings`，不整份覆盖壁纸插件配置；壁纸和头像资源另行选择。
