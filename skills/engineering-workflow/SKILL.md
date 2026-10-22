---
name: engineering-workflow
description: Choose a lightweight, verifiable workflow for software development by project state, ambiguity, change type, and risk, including interrupted work.
---

# 工程工作流

用于新建、修改、修复、重构、迁移、性能优化、技术探索或接续未完成的软件开发工作。适用于 Codex 和 OpenCode；不依赖特定命令或工具。

## 选择路线

先判断仓库上下文、目标组件是否已有稳定实现、目标是否清晰、主要变化类型，以及影响面与风险。仅当改动明确、局部、低风险且可直接完成并验证时，可直接调查、修改并验证，不必先建文档。

其余任务必须先读 [任务路由](references/routing.md)，确定主路线并读取对应参考文件后再执行；不得仅凭本页摘要重建专项流程。只读取选中的主流程和必要相关参考文件，不一次加载全部文件。中断接续先读 [恢复](references/recovery.md)，核对现场后回到原任务路线。

关键行为或边界仍不明确时按 [需求澄清](references/clarification.md) 处理。新建工作读 [新项目](references/greenfield.md)，延续既有行为的工作读 [存量项目](references/existing-project.md)。具体变化类型由任务路由指向相应参考文件。复合任务选择主路线，只补必要步骤；执行中发现新的复杂度或风险，再升级流程。

## 工作产物

只在确实需要锁定范围、描述复杂目标行为、解释技术方案、安排多阶段步骤、保留长期决定或交接未完工作时，使用相应 [模板](assets/)；模板不构成固定套餐，也不要求填满所有章节。当前实现事实、任务目标行为、执行计划、长期决定和临时恢复状态应分别归位，失效时更新或清理。

声称任务完成前，必须执行与本次风险和修改范围相匹配的验证，且结果支持完成结论。无法执行的验证必须明确报告“未验证”及原因；不得仅因代码已修改就宣称任务完成。
