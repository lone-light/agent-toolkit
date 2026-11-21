# 格式化与前端检查模板

| 模板                                                   | 使用位置                             | 用途                                     |
| ------------------------------------------------------ | ------------------------------------ | ---------------------------------------- |
| [`.editorconfig`](.editorconfig)                       | 项目根目录                           | 统一编码、换行、缩进和行尾空白           |
| [`.prettierrc.json`](.prettierrc.json)                 | 前端包根目录，和 `package.json` 同级 | 格式化 Prettier 支持的文件               |
| [`eslint.config.mjs`](eslint.config.mjs)               | Vue 3 + TypeScript 包根目录          | 检查 Vue、TypeScript、导入顺序和组件命名 |
| [`eslint.project-rules.mjs`](eslint.project-rules.mjs) | 与 ESLint 配置同级                   | 提供可选的 API、Pinia 和 Promise 检查    |

已有配置的项目可合并所需选项。前端包也可以沿用上层项目的 `.editorconfig`。

检查工具和锁文件位于 `test-project/`。在 `agent-toolkit` 根目录安装一次依赖，再验证模板。

```sh
(cd formatting/test-project && npm ci)
node --test formatting/tests/verify.mjs
```

规则自测脚本和[预期诊断](tests/cases.mjs)位于 `tests/`。自测读取独立的 Vue 3 和 TypeScript [测试项目](test-project/README.md)中的样例，验证默认规则、可选规则和 Prettier 格式化后的 ESLint 结果。更新模板规则时，同步更新测试项目中的配置。

## 格式化

`.editorconfig` 统一使用 UTF-8、LF 和文件末尾换行。默认 2 空格缩进，Java 和 XML 用 4 空格。Markdown 保留行尾空格，便于书写硬换行。

Prettier 对 JS 和 TS 使用单引号，省略可省略的分号。JS、TS、Vue、样式、HTML 和 JSON 的建议行宽为 100，由 `.editorconfig` 的 `max_line_length` 提供。Prettier 配置中显式设置的 `printWidth` 优先。行宽是换行参考，长字符串等内容仍可能超过 100 字符。 [Prettier 配置说明](https://prettier.io/docs/configuration) · [Prettier 行宽说明](https://prettier.io/docs/options#print-width)

在前端包安装 Prettier 后，可检查整个包或指定文件和目录。

```sh
npm install --save-dev prettier@^3
npm exec -- prettier --check .
```

生成目录按项目需要写入 `.prettierignore`。

## Vue 3 和 TypeScript 检查

在已有 TypeScript 依赖的 Vue 3 包中安装 ESLint 及所需插件。其他技术栈沿用自己的 ESLint 配置。

- 已有的组件块按 `script`、`template`、`style` 排列。 [组件块顺序](https://eslint.vuejs.org/rules/block-order)
- Vue 脚本块使用 `lang="ts"`，组件逻辑使用 `setup`。纯模板组件可以省略脚本。 [组件 API 风格](https://eslint.vuejs.org/rules/component-api-style)
- 有样式块时使用 `<style scoped>`。使用 `module` 的样式块会报错。 [样式块属性](https://eslint.vuejs.org/rules/enforce-style-attribute)
- 用到 `defineProps` 或 `defineEmits` 时，将它们放在导入和类型声明之后、普通语句之前。两者都用时先写 `defineProps`。 [宏声明顺序](https://eslint.vuejs.org/rules/define-macros-order)
- 带脚本的组件用 `defineEmits` 声明对外事件。ESLint 检查可静态识别的事件名，`vue-tsc` 检查参数类型。 [Vue 事件声明](https://eslint.vuejs.org/rules/require-explicit-emits)
- 导入按内置模块、第三方依赖、项目内部模块、相对路径排列。纯类型导入随模块来源分组，`@/` 识别为项目内部别名。自动修复保留副作用导入的位置，例如 `import './styles/main.css'`。 [导入排序](https://github.com/un-ts/eslint-plugin-import-x/blob/master/docs/rules/order.md)
- `src/` 下普通 `.vue` 文件使用 PascalCase，例如 `HomeView.vue`。有文件路由或生成目录的项目可给命名检查配置路径例外。 [Vue 文件命名](https://vuejs.org/style-guide/rules-strongly-recommended#single-file-component-filename-casing)

```sh
npm install --save-dev eslint@^10 eslint-plugin-vue@^10 vue-eslint-parser@^10 \
  @vue/eslint-config-typescript@^14.9 eslint-config-prettier@^10 \
  eslint-plugin-import-x@^4.18.1 eslint-plugin-check-file@^3.3.2
npm exec -- eslint .
```

ESLint 10 支持 Node.js `^20.19.0 || ^22.13.0 || >=24`。模板忽略包根目录的 `dist/` 和 `coverage/`。排版由 Prettier 检查，Vue 类型检查使用项目的 `vue-tsc` 命令。 [Vue 规则配置](https://eslint.vuejs.org/user-guide/) · [Vue TypeScript 配置](https://github.com/vuejs/eslint-config-typescript) · [格式规则兼容配置](https://github.com/prettier/eslint-config-prettier)

### 可选项目规则

将 `eslint.project-rules.mjs` 放在 ESLint 配置旁。按需导入规则，在 `eslint.config.mjs` 底部将它们传给 `createVueTsConfig`。例如，只启用 Pinia 命名检查时，导入 `piniaStoreNaming`，再将默认导出改为 `export default createVueTsConfig({ projectRules: [piniaStoreNaming] })`。

- `apiRequestBoundary` 检查 `src/` 下的 `.ts`、`.tsx` 和 `.vue` 文件。全局 `fetch`、Axios 静态导入和字面量动态导入限于 `src/api/`。页面和 Store 通过 API 模块发起业务请求。
- `piniaStoreNaming` 检查 `src/` 中直接调用从 `pinia` 导入的 `defineStore()` 来初始化的变量，名称采用 `useUserStore` 这类写法。 [Pinia 命名建议](https://pinia.vuejs.org/core-concepts/#defining-a-store)
- `promiseHandling` 检查 `src/` 的 `.ts` 文件和 Vue 的 TypeScript 脚本中未处理的 Promise，单独加 `void` 也会报错。检查范围须纳入 `tsconfig`。 [Promise 检查](https://typescript-eslint.io/rules/no-floating-promises/)

`promiseHandling` 将类型服务限定在检查范围内。项目已有类型感知规则时，按各自的文件范围合并配置。

## TypeScript 注释

- 对外业务 API 和供其他模块调用的函数在定义处写 JSDoc。字段和 Props 有特殊含义、单位或默认行为时补充说明。组件内部用代码上方的 `//` 解释容易误解的逻辑和原因。
- 只写一句用途的 JSDoc 固定三行，`/**` 和 `*/` 各占一行，说明写在中间。
- 需要标签时，在用途说明后空一行，再按需写 `@param 名称 - 说明`、`@returns` 或 `@throws`。每个标签及其说明占一行，补充类型签名看不出的信息。
- 简短摘要和单行标签说明省略行尾句号。多句说明照常使用标点。字段名和函数名用反引号标出。

```ts
/**
 * 探测后端连通性
 *
 * @returns 响应对象，`data` 是首页显示的问候语
 * @throws 超时（5 秒）、网络错误、HTTP 非 2xx 响应或 JSON 解析失败时，Promise 会拒绝
 */
```
