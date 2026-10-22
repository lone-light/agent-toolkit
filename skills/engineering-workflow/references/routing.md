# 任务路由

确定主流程和是否需要升级；具体操作只读选中的参考文件。

## 上下文与歧义

明确、局部、低风险的直接修改，无需专用流程或 Artifact，调查相关现状后修改并验证。其他任务先区分仓库上下文与目标组件状态：

- **存量仓库**：已有代码、接口、数据、部署或用户行为需要延续。读 [existing-project.md](existing-project.md)。
- **新系统或新组件**：目标系统或独立组件尚未形成稳定实现。读 [greenfield.md](greenfield.md)。

两者可以组合：在已有大型仓库中首次建设独立组件时，先用 `existing-project` 调查宿主系统的约束、集成边界和现有行为，再用 `greenfield` 处理新组件的目标、骨架和迭代；不要把它们强制当作二选一。

若关键需求仍存在影响交付的实质歧义，读 [clarification.md](clarification.md)。

## 中断恢复

任务尚未完成且发生会话、模型、Agent、Provider 或上下文切换时，先读 [recovery.md](recovery.md) 核对工作现场。Recovery 是横切恢复流程，可以与 Feature、Bugfix、Refactor、Migration、Performance 或 Exploration 组合；它不替代原任务类型。恢复可信后，回到原来的主路线继续工作。

## 主要变化

| 主要变化 | 读取 | 典型目标 |
| --- | --- | --- |
| 当前行为与确定的预期不一致 | [bugfix.md](bugfix.md) | 复现、定位根因、窄范围修复 |
| 新增或改变系统的能力、契约或可观察行为 | [feature.md](feature.md) | 定义行为与验收后实现 |
| 改善内部结构且保持外部行为 | [refactor.md](refactor.md) | 小步改变并保护既有行为 |
| 转换数据、接口、运行时、框架或基础设施 | [migration.md](migration.md) | 管理兼容、切换、失败恢复 |
| 改善延迟、吞吐或资源用量 | [performance.md](performance.md) | 建基线、测量、比较结果 |
| 关键技术事实或方案尚未确定 | [exploration.md](exploration.md) | 用最小实验回答问题 |

复合任务选一个主路线，再补必要步骤。兼有行为变化的重构按功能变化处理；涉及转换及切换风险时按迁移处理。中断只改变进入路线的顺序，不改变主要变化类型。

## 按问题选择 Artifact

Artifact 是可选的工作产物，放在项目约定或用户指定的位置；`assets/` 只存模板。

| Artifact | 解决的问题 | 适用信号 |
| --- | --- | --- |
| [Scope Contract](../assets/scope-contract.md) | 本次范围、验收和约束 | 边界需要显式锁定 |
| [Spec](../assets/spec.md) | 本次要达到的目标行为 | 规则、状态或边界相互作用 |
| [Design](../assets/design.md) | 当前任务的实现方案及权衡 | 存在实际技术复杂度 |
| [Plan](../assets/plan.md) | 当前执行步骤 | 步骤有依赖或需分阶段验证 |
| [ADR](../assets/adr.md) | 长期决策的原因 | 决定未来仍需解释 |
| [Checkpoint](../assets/checkpoint.md) | 未完任务的恢复入口 | 存在上下文连续性风险 |

只读取需要的模板，不因表中存在某项而创建它。Scope Contract 只约束本次任务。Spec 默认是任务级目标行为：开发前描述完成后应达到的结果，开发中随目标或证据更新；完成后，若项目维护长期规格，将已实现行为同步进去，否则按项目约定归档或删除，代码、测试和运行契约作为主要事实来源。Design 是任务级当前方案，通常在实现后失效；只有稳定的长期架构事实才同步到项目文档，未来仍需解释的决策原因才进入 ADR。Plan 和 Checkpoint 失效后按项目约定清理，Git 承担普通历史。

## 调整与完成

分类是暂定的。发现状态复杂、跨模块依赖、兼容或数据风险时，补必要的澄清或 Artifact；复杂性消失时省略尚未产出的文档。以实际验证结果判定完成，不无条件跑全量检查。
