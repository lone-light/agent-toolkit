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

## JavaScript 和 TypeScript 注释

- `lonelight-docs/required` 检查 `src/` 下 JS、TS 和 Vue 脚本中的导出函数、类、类字段、类方法、TS 接口和类型别名。导出对象中的函数成员也在定义处写说明。接口成员和类型别名中写出的对象成员各自写 JSDoc。构造器、访问器、静态和私有成员也要写说明。同组重载至少写一段说明，各段标签按所在签名核对。显式 `override` 的类方法可以沿用父方法文档。类型声明文件和测试文件排除在缺失检查范围外。
- 只写一行说明的 JSDoc 固定三行。`lonelight-docs/layout` 检查 `/**`、`*/` 是否各占一行，非空正文行是否以 `*` 开头。`@type`、`@typedef` 和 `@license` 等单行类型标注、工具指令及许可声明可以保留单行。
- 标签按 `@param`、`@returns`、`@throws` 排列，正文与标签之间空一行。每个标签及其说明占一行，简短摘要和标签说明省略行尾句号。`@throws` 写明抛错或 Promise 拒绝的条件，扫描检查已有标签的说明。
- 对可调用声明，逐个参数写 `@param 名称 - 说明`，检查参数名、顺序、缺漏、重复和说明。TS 构造器参数属性用构造器的 `@param` 说明。解构参数的根标签按签名位置对应，子标签可记录能定位的属性。
- 返回结果能从签名、类型信息或明确的表达式确定时检查 `@returns`。已知无结果的构造器、setter、`void`、`Promise<void>` 和 `never` 省略此标签。项目提供 ESLint 类型信息时，也检查可推断的返回类型。签名和代码均无法确定时，校验已有标签的说明和重复情况。

```ts
/**
 * 探测后端连通性
 *
 * @returns 响应对象，`data` 是首页显示的问候语
 * @throws 超时（5 秒）、网络错误、HTTP 非 2xx 响应或 JSON 解析失败时，Promise 会拒绝
 */
```

## Vue 组件注释

- `lonelight-docs/vue-contract` 检查 `src/` 下 `<script setup>` 中可定位的 Prop 和事件声明。每个 Prop 和事件各写一段 JSDoc，字段是否有默认值遵循同一条规则。`withDefaults` 的说明写在 Prop 声明处，事件说明写在 `defineEmits` 的声明处。支持从本文件的类型和运行时字面量定位声明。`defineModel` 的 Prop 和自动生成的更新事件共用宏定义处的说明。
- `lonelight-docs/required` 检查组件脚本中有名字的函数，包括函数声明、赋给变量的箭头函数和函数表达式。组件模板使用的内部函数也在定义处写说明，参数和返回值沿用 JS、TS 规则。
- 对外暴露的函数先单独定义，再用 `defineExpose({ focus })` 引用。JSDoc 写在 `focus` 的定义处一次。

完整示例见 [DraftEditor.vue](test-project/src/components/DraftEditor.vue) 和 [draft.ts](test-project/src/components/draft.ts)。[Vue 组件宏](https://vuejs.org/api/sfc-script-setup.html#defineprops-defineemits)
