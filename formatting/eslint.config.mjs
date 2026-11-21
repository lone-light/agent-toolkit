import { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { globalIgnores } from 'eslint/config'
import { withVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from 'eslint-config-prettier/flat'
import checkFile from 'eslint-plugin-check-file'
import { importX } from 'eslint-plugin-import-x'
import pluginVue from 'eslint-plugin-vue'

const configDirectory = dirname(fileURLToPath(import.meta.url))

/**
 * 创建 Vue 3 和 TypeScript 项目的 ESLint 配置
 *
 * @param {{ rootDir?: string, projectRules?: Array<Parameters<typeof withVueTs>[1]> }} [options] - 前端项目根目录和按需启用的规则
 * @returns 传给 ESLint 的配置
 */
export function createVueTsConfig({ rootDir = configDirectory, projectRules = [] } = {}) {
  const configureVueTs = withVueTs.bind(undefined, { rootDir })
  return configureVueTs(
    globalIgnores(['dist/**', 'coverage/**']),
    pluginVue.configs['flat/essential'],
    vueTsConfigs.recommended,
    {
      name: 'lonelight/import-order',
      files: ['**/*.{js,mjs,cjs,ts,mts,cts,tsx,vue}'],
      plugins: { 'import-x': importX },
      settings: { 'import-x/internal-regex': '^@/' },
      rules: {
        'import-x/order': [
          'error',
          {
            groups: ['builtin', 'external', 'internal', ['parent', 'sibling', 'index']],
            warnOnUnassignedImports: false,
          },
        ],
      },
    },
    {
      files: ['**/*.vue'],
      rules: {
        'vue/block-order': ['error', { order: ['script', 'template', 'style'] }],
        'vue/component-api-style': ['error', ['script-setup']],
        'vue/enforce-style-attribute': ['error', { allow: ['scoped'] }],
        'vue/define-macros-order': ['error', { order: ['defineProps', 'defineEmits'] }],
        'vue/require-explicit-emits': 'error',
      },
    },
    {
      name: 'lonelight/vue-filenames',
      files: ['src/**/*.vue'],
      plugins: { 'check-file': checkFile },
      rules: {
        'check-file/filename-naming-convention': ['error', { '**/*.vue': 'PASCAL_CASE' }],
      },
    },
    ...projectRules,
    // 放在最后，关闭与 Prettier 冲突的格式规则
    skipFormatting,
  )
}

export default createVueTsConfig()
