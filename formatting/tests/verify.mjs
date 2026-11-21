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
    await Promise.all(
      results.map((result) => {
        const file = relative(samples, result.filePath).split(sep).join('/')
        const sample = cases[file]
        const expected = [...(sample?.base ?? []), ...(projectRules ? (sample?.project ?? []) : [])]
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

    for (const file of Object.keys(cases)) {
      assert.ok(
        results.some(({ filePath }) => filePath === join(samples, file)),
        file,
      )
    }

    await context.test('Prettier 读取两份配置，格式化后 ESLint 通过', async () => {
      const filePath = join(samples, 'src/FormatExample.ts')
      const expected = (await readFile(filePath)).toString()
      const unformatted =
        'export function getLabel(){\r\nreturn {label: "格式化规则", description: "这行超过 80 列，但没有超过项目配置的 100 列"};\r\n}'
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
