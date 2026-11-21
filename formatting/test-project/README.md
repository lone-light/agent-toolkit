# Vue 和 TypeScript 规则样例

`src/` 和 `tools/` 保存正反例。项目目录包含自己的 EditorConfig、Prettier、ESLint 配置和依赖锁文件。ESLint 在基础配置上启用 API、Pinia 和 Promise 规则。

进入样例项目目录，安装依赖并运行检查。

```sh
npm ci
npm run lint
npm run format:check
```

`npm run lint` 显示每条违规的文件、行列号和规则名。项目里有预期违规，扫描会返回非零退出码。Axios 和 Pinia 导入用于静态检查。

## 在 IDEA 对照

直接打开本目录，启用自动 ESLint 配置。比较 `src/components/StatusCard.vue` 和 `src/components/legacy-panel.vue`，再用终端输出核对诊断。Prettier 使用项目根目录的配置，可用 `npm run format:check` 检查样例排版。
