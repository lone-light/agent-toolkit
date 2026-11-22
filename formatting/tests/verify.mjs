import assert from 'node:assert/strict'
import { execFile } from 'node:child_process'
import { copyFile, mkdtemp, readFile, rm, symlink } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { tmpdir } from 'node:os'
import { join, relative, sep } from 'node:path'
import { after, before, test } from 'node:test'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { promisify } from 'node:util'
import { cases } from './cases.mjs'

const templates = fileURLToPath(new URL('../', import.meta.url))
const samples = join(templates, 'test-project')
const loadTool = createRequire(join(samples, 'package.json'))
const { ESLint } = loadTool('eslint')
const { check, format, resolveConfig } = loadTool('prettier')
const run = promisify(execFile)
const eslintFiles = ['eslint.config.mjs', 'eslint.project-rules.mjs']

let templateDirectory
let createVueTsConfig
let projectRuleOptions

before(async () => {
  // 在临时目录加载模板原文件，依赖由样例项目提供
  templateDirectory = await mkdtemp(join(tmpdir(), 'formatting-template-'))
  await Promise.all([
    ...eslintFiles.map((file) => copyFile(join(templates, file), join(templateDirectory, file))),
    symlink(join(samples, 'node_modules'), join(templateDirectory, 'node_modules'), 'junction'),
  ])
  const [templateConfig, projectRules] = await Promise.all(
    eslintFiles.map((file) => import(pathToFileURL(join(templateDirectory, file)).href)),
  )
  createVueTsConfig = templateConfig.createVueTsConfig
  const { apiRequestBoundary, piniaStoreNaming, promiseHandling } = projectRules
  projectRuleOptions = [apiRequestBoundary, piniaStoreNaming, promiseHandling]
})

after(async () => {
  if (templateDirectory) {
    await rm(templateDirectory, { recursive: true, force: true })
  }
})

/**
 * 统计每条规则出现的次数，方便按实际诊断比对
 *
 * @param {(string | null)[]} ruleIds - ESLint 规则名，解析失败时可能为 `null`
 * @returns {Map<string | null, number>} 各规则对应的诊断次数
 */
function countRuleIds(ruleIds) {
  const counts = new Map()
  for (const ruleId of ruleIds) {
    counts.set(ruleId, (counts.get(ruleId) ?? 0) + 1)
  }
  return counts
}

test('模板源码通过 ESLint、TypeScript 和 Prettier 检查', async () => {
  const eslint = new ESLint({
    cwd: templates,
    overrideConfigFile: true,
    overrideConfig: await createVueTsConfig({ rootDir: templates }),
  })
  const [results] = await Promise.all([
    eslint.lintFiles(['eslint*.mjs', 'test-project/*.mjs', 'tests/*.mjs']),
    run(
      process.execPath,
      [
        loadTool.resolve('typescript/lib/tsc.js'),
        '--allowJs',
        '--checkJs',
        '--noEmit',
        '--strict',
        '--skipLibCheck',
        '--module',
        'nodenext',
        '--types',
        'node',
        ...eslintFiles,
      ],
      { cwd: templateDirectory },
    ),
    run(
      process.execPath,
      [
        loadTool.resolve('prettier/bin/prettier.cjs'),
        '--check',
        'README.md',
        '.prettierrc.json',
        ...eslintFiles,
        'test-project',
        'tests',
      ],
      { cwd: templates },
    ),
  ])
  const issues = results.filter(({ messages }) => messages.length > 0)
  const formatter = await eslint.loadFormatter()
  assert.equal(issues.length, 0, await formatter.format(issues))
})

test('样例项目的配置与模板一致', async () => {
  const copiedConfigs = ['.editorconfig', '.prettierrc.json', 'eslint.project-rules.mjs']
  await Promise.all(
    copiedConfigs.map(async (file) => {
      const [sample, template] = await Promise.all([
        readFile(join(samples, file)),
        readFile(new URL(`../${file}`, import.meta.url)),
      ])
      assert.deepEqual(sample, template, file)
    }),
  )

  const reference = new ESLint({
    cwd: samples,
    overrideConfigFile: true,
    overrideConfig: await createVueTsConfig({
      rootDir: samples,
      projectRules: projectRuleOptions,
    }),
  })
  const installed = new ESLint({ cwd: samples })
  const exampleFiles = [
    'src/components/StatusCard.vue',
    'src/ApiBoundary.ts',
    'src/api/status.ts',
    'tools/OutsideProject.ts',
  ]
  await Promise.all(
    exampleFiles.map(async (file) => {
      const [expected, actual] = await Promise.all([
        reference.calculateConfigForFile(file),
        installed.calculateConfigForFile(file),
      ])
      assert.deepEqual(actual.rules, expected.rules, file)
      assert.deepEqual(actual.settings, expected.settings, file)
    }),
  )
})

for (const projectRules of [false, true]) {
  const configuration = projectRules ? '启用 API、Pinia 和 Promise 规则' : '基础配置'
  test(configuration, async (context) => {
    const eslint = projectRules
      ? new ESLint({ cwd: samples })
      : new ESLint({
          cwd: samples,
          overrideConfigFile: true,
          overrideConfig: await createVueTsConfig({ rootDir: samples }),
        })
    const results = await eslint.lintFiles(['src', 'tools'])
    const scanned = new Map(
      results.map((result) => [relative(samples, result.filePath).split(sep).join('/'), result]),
    )
    assert.deepEqual(
      new Set(scanned.keys()),
      new Set(Object.keys(cases)),
      '扫描文件应与样例清单一致',
    )

    await Promise.all(
      Array.from(scanned, ([file, result]) => {
        const { base = [], project = [] } = cases[file]
        const expected = projectRules ? [...base, ...project] : base
        return context.test(file, () => {
          assert.deepEqual(
            countRuleIds(result.messages.map(({ ruleId }) => ruleId)),
            countRuleIds(expected),
            JSON.stringify(result.messages, null, 2),
          )
          assert.equal(result.errorCount, expected.length)
        })
      }),
    )

    await context.test('检查 TypeScript、JavaScript 和 Vue 脚本的 JSDoc 版式', async () => {
      const samplesByCase = [
        {
          file: 'src/FormatExample.ts',
          expectedLines: [1, 3, 7, 10, 13, 16, 19, 23],
          source: `/** 单行说明 */
export const inline = 1
/** 开始标记后的说明
 * 第二行
 */
export const opener = 2
/**
 * 结束标记前的说明 */
export const closer = 3
export const before = 4 /**
 * 开始标记之前有代码
 */
/**
 * 结束标记之后有代码
 */ export const after = 5
/**
 */
export const empty = 6
/**
正文缺少星号
 */
export const missingAsterisk = 7
/**
 *
 * 单句摘要
 *
 */
export const extraLines = 8`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [],
          source: `// /** 普通行注释 */
/* 普通块注释 */
/*** 装饰性分隔线 ***/
/**
 * 生成标签
 *
 * @param value - 标签文本
 * @returns 待显示的文本
 */
export function label(value: string) { return value }
/**
 * 提供默认名称
 */
export const defaultLabel = '示例'`.replaceAll('\n', '\r\n'),
        },
        {
          file: 'eslint.config.mjs',
          expectedLines: [4, 6],
          source: `/** @type {number} */
export const count = 0
/** @license MIT */
/** 单行说明仍需分行 */
export const label = '示例'
/** @returns 初始标签 */
export function initialLabel() { return label }`,
        },
        {
          file: 'src/components/StatusCard.vue',
          expectedLines: [2, 5],
          source: `<script lang="ts">
/** 普通脚本的说明 */
export const heading = '示例'
</script>
<script setup lang="ts">/** setup 脚本的说明 */
const label = heading
</script>
<template>
  <!-- /** 模板注释 */ -->
  <p>{{ label }}</p>
</template>
<style scoped>
/** 样式注释 */
p { color: inherit; }
</style>`,
        },
      ]
      await Promise.all(
        samplesByCase.map(async ({ file, source, expectedLines }) => {
          const [result] = await eslint.lintText(source, { filePath: join(samples, file) })
          assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
          const reports = result.messages.filter(({ ruleId }) => ruleId === 'lonelight-docs/layout')
          assert.deepEqual(
            reports.map(({ line, severity }) => [line, severity]),
            expectedLines.map((line) => [line, 2]),
            JSON.stringify(result.messages),
          )
        }),
      )
    })

    await context.test('检查导出函数、类声明和方法是否缺少 JSDoc', async () => {
      const samplesByCase = [
        {
          file: 'src/FormatExample.ts',
          expectedLines: [10],
          source: `const key = () => String(Math.random())
/**
 * 支持按键访问的方法集
 */
class Computed {
  /**
   * 处理首个键
   */
  [key()]() {}
  [key()]() {}
}`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [1, 3, 5, 6, 11, 15],
          source: `export function withoutDoc() { return 1 }
// 普通行注释
export class OrdinaryComment {}
/** */
export const arrow = () => 2
export const expression = function () { return 3 }
/**
 * 提供默认标签
 */
export class Documented {
  label() { return '示例' }
}
export const limit = 42
function formatPrivately() { return limit }
class PrivateClass {}`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [1, 3, 8],
          source: `const local = () => '示例'
export { local as publicLabel }
function fallback() { return local() }
export default fallback
export { externalLabel } from './ExternalLabels'
export * from './ExternalFormat'
const limit = 42 /** 这条注释属于常量 */
export function missingAfterTrailingComment() { return limit }`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [],
          source: `/**
 * 将标签转换成可显示的文本
 *
 * @param label - 待展示的标签值
 * @returns 标签的文本表示
 */
export function show(label: string): string
export function show(label: number): string
export function show(label: string | number): string { return String(label) }
/** 单行说明由版式规则检查 */
export function present(): void { show(42) }`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [2],
          source: `/* 这里只使用了普通块注释 */
export default function () { return '示例' }`,
        },
        {
          file: 'tools/OutsideProject.ts',
          expectedLines: [],
          source: `export function helper() { return '示例' }`,
        },
        {
          file: 'src/components/StatusCard.vue',
          expectedLines: [2, 5, 6],
          source: `<script lang="ts">
export function publicLabel() { return '示例' }
</script>
<script setup lang="ts">
function internalLabel() { return publicLabel() }
class PrivateClass {}
const label = internalLabel()
</script>
<template><p>{{ label }}</p></template>`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [1, 2, 3, 4, 5, 6, 7, 8, 9, 16],
          source: `class LocalFormatter {
  constructor() {}
  static reset() {}
  private format() {}
  #initialize() {}
  get value(): number { return 1 }
  set value(value: number) {}
  get #state(): number { return 1 }
  set #state(value: number) {}
  /**
   * 组合展示内容
   *
   * @returns 待显示的示例文本
   */
  present() { return '示例' }
  data = { default: 3 }
}`,
        },
        {
          file: 'src/FormatExample.ts',
          expectedLines: [1, 2],
          source: `const unnamed = class {
  label() { return '示例' }
}
/**
 * 提供标签转换方法
 */
const documented = class {
  /**
   * 将数字或字符串转换为可显示的标签
   *
   * @param value - 待转换的内容
   * @returns 转换后的标签文本
   */
  format(value: string): string
  format(value: number): string
  format(value: string | number): string { return String(value) }
}`,
        },
      ]
      await Promise.all(
        samplesByCase.map(async ({ file, source, expectedLines }) => {
          const [result] = await eslint.lintText(source, { filePath: join(samples, file) })
          assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
          const reports = result.messages.filter(
            ({ ruleId }) => ruleId === 'lonelight-docs/required',
          )
          assert.deepEqual(
            reports.map(({ line, severity }) => [line, severity]),
            expectedLines.map((line) => [line, 2]),
            JSON.stringify(result.messages),
          )
        }),
      )
    })

    await context.test('检查 JavaScript 类字段和 TypeScript 类型声明', async () => {
      const declarations = [
        {
          file: 'src/DocumentationProbe.js',
          source: 'class Session {\n  #closed = false\n  close() {}\n}',
          expected: [
            [1, 'missingClass'],
            [2, 'missingField'],
            [3, 'missingMethod'],
          ],
        },
        {
          file: 'src/DocumentationProbe.cjs',
          source: 'const read = () => 1\nmodule.exports.read = read',
          expected: [[1, 'missing']],
        },
        {
          file: 'src/FormatExample.ts',
          source: 'const read = () => 1\nexport const getValue = read\nexport default getValue',
          expected: [[1, 'missing']],
        },
        {
          file: 'src/FormatExample.ts',
          source: `interface Session {
  encoding?: string
  read(offset: number): string
}
export type Feedback = -1 | 0 | 1
type Buffered = Session & { size: number }`,
          expected: [
            [1, 'missingInterface'],
            [2, 'missingInterfaceMember'],
            [3, 'missingInterfaceMember'],
            [5, 'missingTypeAlias'],
            [6, 'missingTypeAlias'],
            [6, 'missingTypeMember'],
          ],
        },
        {
          file: 'src/FormatExample.ts',
          source: `/**
 * 定义文件会话
 */
interface Session {
  /**
   * 可选的会话名称
   */
  name?: string
  /**
   * 读取指定偏移位置的文本
   *
   * @param offset - 从零开始的偏移量
   * @returns 该位置的文本
   */
  read(offset: number): string
}
/**
 * 提供默认的读取实现
 */
class Base {
  /**
   * 读取会话文本
   *
   * @returns 当前会话的文本
   */
  read(): string { return '示例' }
}
/**
 * 在父类基础上扩展会话
 */
class Derived extends Base {
  override read(): string { return super.read() }
}
/**
 * 表示回复反馈的编号
 */
type Feedback = -1 | 0 | 1`,
          expected: [],
        },
      ]

      for (const { file, source, expected } of declarations) {
        const [result] = await eslint.lintText(source, { filePath: join(samples, file) })
        assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
        const reports = result.messages.filter(({ ruleId }) => ruleId === 'lonelight-docs/required')
        assert.deepEqual(
          reports.map(({ line, messageId }) => [line, messageId]),
          expected,
          JSON.stringify(result.messages),
        )
      }
    })

    await context.test('检查导出对象方法、嵌套类型与各自编写的重载文档', async () => {
      const source = `function refresh() {}
const api = {
  refresh,
  ...{ reset() {}, overwritten: () => 0 },
  overwritten: null,
  save: () => 1,
  nested: { dispose() {} },
}
export { api as client }
export default { close() {} }
/**
 * 记录载荷中的对象元素
 */
type Records = Readonly<[entry: { id: string }]>
/**
 * 创建处理结果
 *
 * @param options - 输入选项
 * @returns 处理结果
 */
type Builder = (options: { input: string }) => { result: string }
/**
 * 提供可清理的会话
 */
interface Session {
  /**
   * 清理当前会话
   */
  clear: ((keepHistory: boolean) => void) | null
}
/**
 * 将文本命令转换为字符串
 *
 * @param cmd - 文本命令
 * @returns 命令的字符串形式
 */
export function command(cmd: string): string
/**
 * 将数字命令转换为字符串
 */
export function command(cmd: number): string
export function command(cmd: number | string): string { return String(cmd) }
export const makeLabel =
  /**
   * 构造标签
   *
   * @returns 默认标签
   */
  function () { return '示例' }`
      const filePath = join(samples, 'src/FormatExample.ts')
      const [result] = await eslint.lintText(source, { filePath })
      assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
      const issues = result.messages.filter(({ ruleId }) => ruleId === 'lonelight-docs/required')
      assert.deepEqual(
        issues
          .filter(({ messageId }) => messageId)
          .map(({ messageId, message }) => [messageId, message.split(' ')[1]]),
        [
          ['missing', 'refresh'],
          ['missing', 'reset'],
          ['missing', 'save'],
          ['missing', 'dispose'],
          ['missing', 'close'],
          ['missingTypeMember', 'id'],
          ['missingTypeMember', 'input'],
          ['missingTypeMember', 'result'],
        ],
        JSON.stringify(issues),
      )
      assert.deepEqual(
        issues.filter(({ messageId }) => !messageId).map(({ message }) => message),
        [
          'JSDoc 缺少 @param keepHistory',
          'JSDoc 缺少 @param cmd',
          'JSDoc 缺少 @returns 返回值说明',
        ],
        JSON.stringify(issues),
      )
      const [commonJs] = await eslint.lintText('module.exports = { ...other, close() {} }', {
        filePath: join(samples, 'src/DocumentationProbe.cjs'),
      })
      assert.equal(commonJs.fatalErrorCount, 0, JSON.stringify(commonJs.messages))
      assert.equal(
        commonJs.messages.filter(({ messageId }) => messageId === 'missing').length,
        1,
        JSON.stringify(commonJs.messages),
      )
    })

    await context.test('核对解构路径和明确的返回值，保留多句摘要及示例', async () => {
      const filePath = join(samples, 'src/FormatExample.ts')
      const source = `/**
 * 读取记录。读取失败时保留原内容。
 *
 * @param opts - 读取选项
 * @param opts["nested.value"] - 带点的字段
 * @param opts.nested.typo - 拼写有误的字段
 */
export function read({ 'nested.value': value, nested: { valid }, ...rest }) {}

/**
 * 核对数组参数
 *
 * @param opts[0] - 第一项
 * @param opts - 数组选项
 * @param opts.0 - 重复的第一项
 * @param second - 后续参数
 */
export function compare([item], second) {}

/**
 * 检查声明的对象字段
 *
 * @param opts - 保存选项
 * @param opts.missing - 未声明字段
 */
export function update(opts: { size: number }) {}

/**
 * 构造站点地址
 *
 * @example
 *     @param example - 这是示例文本
 */
export function url() { return new URL('https://example.test') }

/**
 * 清除参数
 *
 * @returns 无返回值
 */
export function clear() { return undefined }`
      const [result] = await eslint.lintText(source, { filePath })
      assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
      assert.deepEqual(
        result.messages.filter(({ ruleId }) => ruleId === 'lonelight-docs/layout'),
        [],
      )
      const issues = result.messages
        .filter(({ ruleId }) => ruleId === 'lonelight-docs/required')
        .map(({ message }) => message)
      assert.equal(issues.length, 6, JSON.stringify(issues))
      for (const evidence of [
        /opts\.nested\.typo/,
        /opts\.0/,
        /@param 顺序/,
        /opts\.missing/,
        /缺少 @returns/,
        /无需添加 @returns/,
      ]) {
        assert(
          issues.some((message) => evidence.test(message)),
          JSON.stringify(issues),
        )
      }
    })

    await context.test('核对调用签名和参数、结果及异常标签', async () => {
      const filePath = join(samples, 'src/FormatExample.ts')
      const invalid = `/**
 * 将数值换算为秒。
 * @throws {Error}
 * @param value - 原始数值
 */
export function seconds(milliseconds: number): number { return milliseconds / 1000 }`
      const [bad] = await eslint.lintText(invalid, { filePath })
      assert.equal(bad.fatalErrorCount, 0, JSON.stringify(bad.messages))
      const issues = bad.messages.filter(({ ruleId }) => ruleId?.startsWith('lonelight-docs/'))
      assert.deepEqual(
        issues
          .filter(({ ruleId }) => ruleId === 'lonelight-docs/layout')
          .map(({ messageId, line }) => [messageId, line]),
        [
          ['punctuation', 1],
          ['tagSpacing', 1],
          ['emptyThrows', 3],
          ['tagOrder', 4],
        ],
        JSON.stringify(issues),
      )
      assert.deepEqual(
        issues
          .filter(({ ruleId }) => ruleId === 'lonelight-docs/required')
          .map(({ message, line }) => [message, line]),
        [
          ['JSDoc 缺少 @param milliseconds', 1],
          ['JSDoc 的 @param value 无法对应函数签名中的参数', 1],
          ['JSDoc 缺少 @returns 返回值说明', 1],
        ],
        JSON.stringify(issues),
      )

      const valid = `/**
 * 将文本写入目标
 *
 * @param text - 要写入的文本
 * @throws {Error} 文本为空时拒绝写入
 */
export async function writeText(text: string): Promise<void> {
  if (!text) throw new Error('文本为空')
}`
      const [good] = await eslint.lintText(valid, { filePath })
      assert.equal(good.fatalErrorCount, 0, JSON.stringify(good.messages))
      assert.deepEqual(
        good.messages.filter(({ ruleId }) => ruleId?.startsWith('lonelight-docs/')),
        [],
      )
    })

    await context.test('有类型信息时检查间接调用的推断返回值', async () => {
      const source = `function calculate(): number { return 1 }

/**
 * 读取计算值
 */
export function read() { return calculate() }`
      const [result] = await eslint.lintText(source, {
        filePath: join(samples, 'src/FormatExample.ts'),
      })
      assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
      const issues = result.messages.filter(({ ruleId }) => ruleId?.startsWith('lonelight-docs/'))
      assert.deepEqual(
        issues.map(({ ruleId, message, line }) => [ruleId, message, line]),
        projectRules ? [['lonelight-docs/required', 'JSDoc 缺少 @returns 返回值说明', 3]] : [],
        JSON.stringify(issues),
      )
    })

    await context.test('草稿组件逐项检查 Props、事件和具名函数', async () => {
      const filePath = join(samples, 'src/components/DraftEditor.vue')
      const original = await readFile(filePath, 'utf8')
      const source = original.replace(/^[ \t]*\/\*\*[\s\S]*?\*\/\r?\n/gm, '')
      assert.notEqual(source, original)
      const [result] = await eslint.lintText(source, { filePath })
      assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
      const issues = result.messages.filter(({ ruleId }) => ruleId?.startsWith('lonelight-docs/'))
      const expected = [
        ['maxLength', 'lonelight-docs/vue-contract'],
        ['initialText', 'lonelight-docs/vue-contract'],
        ['saved', 'lonelight-docs/vue-contract'],
        ['save', 'lonelight-docs/required'],
        ['reset', 'lonelight-docs/required'],
        ['focus', 'lonelight-docs/required'],
      ]
      assert.equal(issues.length, expected.length, JSON.stringify(issues))
      for (const [name, ruleId] of expected) {
        const matches = issues.filter(
          (issue) => issue.ruleId === ruleId && issue.message.includes(` ${name} `),
        )
        assert.equal(matches.length, 1, JSON.stringify(issues))
        assert.equal(matches[0].severity, 2)
        assert.match(source.split('\n')[matches[0].line - 1], new RegExp(`\\b${name}\\b`))
      }

      const inlineExposure = original.replace(
        'defineExpose({ focus })',
        'defineExpose({ focus, ...{ clear: () => { draft.value = "" }, ignored: () => {} }, ignored: null })',
      )
      const [inline] = await eslint.lintText(inlineExposure, { filePath })
      assert.equal(inline.fatalErrorCount, 0, JSON.stringify(inline.messages))
      const inlineIssues = inline.messages.filter(({ ruleId }) =>
        ruleId?.startsWith('lonelight-docs/'),
      )
      assert.deepEqual(
        inlineIssues.map(({ messageId }) => messageId),
        ['inlineVueExposure'],
        JSON.stringify(inline.messages),
      )
    })

    await context.test('Vue 运行时与类型声明都检查组件契约', async () => {
      const cases = [
        {
          script: `const props = defineProps({
  timeoutMs: {
    type: Number,
    /**
     * 默认等待三秒
     */
    default: 3000,
  },
  title: String,
})
const emit = defineEmits(['saved', 'closed'])`,
          expected: [
            ['missingProp', 'timeoutMs'],
            ['missingProp', 'title'],
            ['missingEmit', 'saved'],
            ['missingEmit', 'closed'],
          ],
        },
        {
          script: `const { title = '示例' } = defineProps(['title', 'name'])
const emit = defineEmits<{
  /**
   * 保存成功时通知父组件
   */
  (event: 'saved'): void
  (event: 'closed'): void
}>()`,
          expected: [
            ['missingProp', 'title'],
            ['missingProp', 'name'],
            ['missingEmit', 'closed'],
          ],
        },
        {
          script: `/**
 * 文本计数，省略时从零开始
 */
const count = defineModel<number>({ default: 0 })
const draft = defineModel<string>('draft')`,
          expected: [['missingProp', 'draft']],
        },
        {
          script: `import type { SharedProps } from './SharedProps'
const props = withDefaults(defineProps<SharedProps & { extra: string }>(), { extra: '' })
const ordinary = { default: 3 }`,
          expected: [['missingProp', 'extra']],
        },
        {
          script: `import { external, externalEvents } from "./contracts"
const props = defineProps({
  ...{ [\`title\`]: String },
  /**
   * 项目的数量
   */
  count: Number,
  ...external,
})
const emit = defineEmits([...['saved'], \`closed\`, ...externalEvents])
const draft = defineModel((\`draft\` as const))`,
          expected: [
            ['missingProp', 'title'],
            ['missingEmit', 'saved'],
            ['missingEmit', 'closed'],
            ['missingProp', 'draft'],
          ],
        },
        {
          script: `const props = defineProps<{ [\`title\`]: string }>()
const emit = defineEmits<{ (event: \`saved\`): void }>()`,
          expected: [
            ['missingProp', 'title'],
            ['missingEmit', 'saved'],
          ],
        },
        {
          script: `/**
 * 用于选取组件标题的键
 */
type Picked = 'title'
const props = defineProps<Pick<{ title: string; count: number }, Picked>>()
const emit = defineEmits<Readonly<{ saved: [] }>>()`,
          expected: [
            ['missingProp', 'title'],
            ['missingEmit', 'saved'],
          ],
        },
        {
          script: `import type { Keys } from './contracts'
type Props = Partial<{ local: string }>
const props = defineProps<Props & Omit<{ unknown: string }, Keys>>()`,
          expected: [],
          expectedRequired: ['missingTypeAlias', 'missingTypeMember'],
        },
        {
          script: `import type { ExternalEvent } from './contracts'
type EventName = 'phantom'
interface Emits<EventName extends string> {
  (event: EventName): void
}
const emit = defineEmits<Emits<ExternalEvent>>()`,
          expected: [],
          expectedRequired: ['missingTypeAlias', 'missingInterface', 'missingInterfaceMember'],
        },
        {
          script: `const emit = defineEmits<(event: 'saved', count: number) => void>()`,
          expected: [['missingEmit', 'saved']],
        },
      ]
      const filePath = join(samples, 'src/components/StatusCard.vue')
      for (const { script, expected, expectedRequired = [] } of cases) {
        const source = `<script setup lang="ts">\n${script}\n</script>\n<template><p>示例</p></template>`
        const [result] = await eslint.lintText(source, { filePath })
        assert.equal(result.fatalErrorCount, 0, JSON.stringify(result.messages))
        const issues = result.messages.filter(
          ({ ruleId }) => ruleId === 'lonelight-docs/vue-contract',
        )
        assert.equal(issues.length, expected.length, JSON.stringify(issues))
        const documentation = result.messages.filter(
          ({ ruleId }) =>
            ruleId?.startsWith('lonelight-docs/') && ruleId !== 'lonelight-docs/vue-contract',
        )
        assert.deepEqual(
          documentation.map(({ messageId }) => messageId),
          expectedRequired,
          JSON.stringify(result.messages),
        )
        expected.forEach(([messageId, name], index) => {
          assert.equal(issues[index].messageId, messageId, JSON.stringify(issues))
          assert(issues[index].message.includes(` ${name} `), issues[index].message)
          assert.equal(issues[index].severity, 2)
        })
      }
    })

    await context.test('Prettier 读取两份配置，格式化后 ESLint 通过', async () => {
      const filePath = join(samples, 'src/FormatExample.ts')
      const expected = (await readFile(filePath)).toString()
      const unformatted = expected
        .replace(
          /export function getLabel\(\) .*/s,
          'export function getLabel(){\nreturn {label: "格式化规则", description: "这行超过 80 列，但没有超过项目配置的 100 列"};\n}',
        )
        .replaceAll('\n', '\r\n')
      const options = await resolveConfig(filePath, { editorconfig: true })
      assert.equal(options.printWidth, 100)
      assert.equal(options.tabWidth, 2)
      assert.equal(await check(unformatted, { ...options, filepath: filePath }), false)
      const formatted = await format(unformatted, { ...options, filepath: filePath })
      assert.equal(formatted, expected)
      assert.equal(await check(formatted, { ...options, filepath: filePath }), true)
      const [result] = await eslint.lintText(formatted, { filePath })
      assert.deepEqual(result.messages, [])
    })

    await context.test('Vue 组件格式化后与正例一致', async () => {
      const filePath = join(samples, 'src/components/StatusCard.vue')
      const expected = (await readFile(filePath)).toString()
      const dirty = expected
        .replace("emit('checked', await fetchStatus())", 'emit( "checked",await fetchStatus());')
        .replace('padding: 0.5rem;', 'padding:0.5rem;')
      const options = await resolveConfig(filePath, { editorconfig: true })
      assert.notEqual(dirty, expected)
      const formatted = await format(dirty, { ...options, filepath: filePath })
      assert.equal(formatted, expected)
      const [result] = await eslint.lintText(formatted, { filePath })
      assert.deepEqual(result.messages, [])
    })
  })
}
