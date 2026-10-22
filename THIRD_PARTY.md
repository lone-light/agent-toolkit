# 第三方来源

记录日期：2026-10-22（Asia/Shanghai）。收录的是当前本机已安装快照，不声称来自已核对的上游 commit，未记录的 commit 不猜测。

| Skill | 作者 / 上游 | 收录版本 | 许可证 | 本次处理 |
| --- | --- | --- | --- | --- |
| grilling | Matt Pocock · https://github.com/mattpocock/skills | 无本地版本号；SKILL.md 在 2026-10-22 与官方当前文件核对并更新，使用 snapshot.json 锁定内容 | MIT | 原样复制安装文件；仅仓库副本补上官方 LICENSE |
| archify | tt-a1i · https://github.com/tt-a1i/archify | 本地 package.json：3.2.0；保留 skill-release.json | MIT，含其他内嵌资源许可 | 原样复制安装包；保留 LICENSE 与 THIRD_PARTY_NOTICES.md |

## grilling

- 当前上游文件：https://github.com/mattpocock/skills/blob/main/skills/productivity/grilling/SKILL.md
- 补充许可证来源：https://raw.githubusercontent.com/mattpocock/skills/main/LICENSE
- 本仓库中的许可证：`skills/grilling/LICENSE`。
- 本机 `agents/openai.yaml` 元数据原样保留，本次没有独立确认它是否与当前上游一致。
- 不收录 Matt Pocock 整套 Skills，不添加其他 Skill。

## archify

- 正式版本页面：https://github.com/tt-a1i/archify/releases/tag/v3.2.0
- 授权文件：`skills/archify/LICENSE`、`skills/archify/THIRD_PARTY_NOTICES.md`。
- 保留安装包中的 renderer、schema、CLI、示例、资源和内嵌许可内容，避免只备份 SKILL.md 导致恢复后不可运行。
- 本次未对整个安装包与上游 release ZIP 做逐字节同源认证；文件哈希锁定的是本地实际安装内容。

## 自维护内容

`engineering-workflow` 和全局规则作为用户维护内容保存；本次不改变内容，不新增其开源授权声明。若未来公开，另行确定授权方式。
