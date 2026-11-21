/**
 * 测试项目的样例在基础配置和项目规则下应报告的 ESLint 规则
 */
export const cases = {
  'src/components/legacy-panel.vue': {
    base: [
      'check-file/filename-naming-convention',
      'vue/block-order',
      'vue/component-api-style',
      'vue/enforce-style-attribute',
    ],
  },
  'src/components/ScriptLanguage.vue': {
    base: ['vue/block-lang'],
  },
  'src/components/InvalidEvent.vue': {
    base: ['vue/define-macros-order', 'vue/require-explicit-emits'],
  },
  'src/ImportOrder.ts': {
    base: ['import-x/order'],
  },
  'src/ApiBoundary.ts': {
    project: ['no-restricted-globals', 'no-restricted-imports', 'no-restricted-syntax'],
  },
  'src/api/AxiosClient.ts': {},
  'src/StoreNames.ts': {
    project: ['lonelight/pinia-store-naming', 'lonelight/pinia-store-naming'],
  },
  'src/PromiseHandling.ts': {
    project: ['@typescript-eslint/no-floating-promises', '@typescript-eslint/no-floating-promises'],
  },
  'src/components/UnhandledRequest.vue': {
    project: ['@typescript-eslint/no-floating-promises'],
  },
  'tools/OutsideProject.ts': {},
}
