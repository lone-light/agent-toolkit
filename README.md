# Agent Toolkit

个人全局 Agent 规则与 Skills 的维护、备份仓库。当前为本地仓库，尚未创建 GitHub 远程仓库，也未公开发布。

## 范围

- `instructions/codex/AGENTS.md`：Codex 全局规则的原样快照。
- `instructions/opencode/AGENTS.md`：OpenCode 全局规则的原样快照。
- `skills/engineering-workflow/`：自维护工程工作流，按用户约定保持定稿内容。
- `skills/grilling/`、`skills/archify/`：第三方 Skill 的已安装版本快照；许可证和来源见 `THIRD_PARTY.md`。
- `snapshot.json`：创建时的版本信息与文件 SHA-256。仅证明已记录文件的完整性，不代表功能测试通过。
- `scripts/verify.py`：只读检查仓库快照完整性，可选检查当前机器安装副本是否漂移。

项目专用规则和 Skill 继续随各自项目管理。Obsidian Knowledge 的 `.agents/skills/` 不收录在此；本仓库不包含任何笔记正文、对话记录、测试报告、工具配置或账号凭据。

## 文件角色

仓库用于维护和版本管理，实际安装位置不变：

| 内容 | 实际使用位置 |
| --- | --- |
| 公共 Skills | `~/.agents/skills/<skill>/` |
| Codex 全局规则 | `~/.codex/AGENTS.md` |
| OpenCode 全局规则 | `~/.config/opencode/AGENTS.md` |

不要把整个 `agent-toolkit` 目录安装为一个 Skill；不要把 `instructions` 放进 Skills 根目录。本仓库不创建 symlink，不在 Codex/OpenCode 专用 Skill 目录重复安装，不增加自动同步或定时任务。

## 日常维护

1. 自维护规则优先在仓库修改并审查；定稿 Skill 只在明确决定变更后再修改。
2. 检查差异、完成必要验证，再单独确认同步到实际使用位置；本仓库没有自动覆盖安装文件的脚本。
3. 第三方更新先下载到临时目录，核对上游、许可证和差异，保留本地自定义内容；仅更新指定 Skill，不批量覆盖整个 `~/.agents/skills/`。
4. 若已在安装目录手动改动，先比较差异并带回仓库，避免两边各改各的。
5. 变更被接受后更新 `snapshot.json` 对应摘要，完成 Git 提交；不能通过更新摘要隐藏未审查差异。

## 检查

在仓库目录运行：

```sh
python3 scripts/verify.py
python3 scripts/verify.py --installed
```

第一条核对仓库内受管文件。第二条还比较本机安装文件，只读，不安装、不删除、不覆盖。安装环境未准备好时第二条报缺失是预期结果。

## 换电脑恢复

1. 克隆本仓库，先运行仓库完整性检查。
2. 若目标已有同名文件或 Skill，先备份并比较，不直接覆盖。
3. 按上表分别恢复规则文件和选定的 Skill 目录；保持第三方运行文件与许可证完整。
4. 安装工具所需的运行环境。当前 Archify package 要求 Node.js >= 18；其安装包不需要盲目运行 `npm install`，以锁定版本的说明为准。
5. 检查安装副本并在新会话中确认可发现性。校验文件相同不等于验证所有工具行为。

## GitHub 发布边界

建议先使用私有仓库。创建或推送远程仓库前，再确认账号、名称、可见性并审查隐私信息。这里只收录第三方授权允许保存的内容，保留原作者及许可声明；自维护内容尚未选择开源许可证，不对整仓库套用第三方 MIT 许可。

已知限制：副本不会自动跟随上游更新；本仓库不是插件管理器，也不保证所有模型每次都遵循 Skill。现有历史测试结论不在此重新背书。
