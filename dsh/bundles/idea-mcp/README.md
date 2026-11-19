# IntelliJ IDEA MCP

把 IntelliJ IDEA 的本地 MCP Server 接入 DSH。启动 IDEA 并打开项目，在 `Settings → AI → MCP Server` 确认服务已启用，然后在 DSH 会话中请助手用 `lint_files` 检查文件，或用 `get_file_problems` 查询单个文件的诊断。

本地地址与本机 Codex、OpenCode 的设置一致：`http://127.0.0.1:64342/stream`。换机器或改端口时，先核对 IDEA 的 `Copy HTTP Stream Config`，再更新配置包。配置不含凭据，不会更改 IDEA 暴露的工具或命令确认设置。仅作代码检查时，不需要启用 IDEA 的免确认命令执行。

连通后，工具会以 `mcp__idea__` 开头出现。IDEA 未启动或服务未开放时，配置包可以保持启用，但会话暂时没有这组工具。DSH 会收到 IDEA 当前开放的工具，接入本身不是只读隔离。DSH 链接本目录，安装后不要移动或删除它。要断开连接，在 DSH 的 **Plugins** 页面禁用 **IntelliJ IDEA MCP** 配置包，不影响 Codex 或 OpenCode。
