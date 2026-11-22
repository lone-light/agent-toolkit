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
    base: [
      'vue/define-macros-order',
      'vue/require-explicit-emits',
      'lonelight-docs/vue-contract',
      'lonelight-docs/vue-contract',
    ],
  },
  'src/ImportOrder.ts': {
    base: ['import-x/order', 'lonelight-docs/required', 'lonelight-docs/required'],
  },
  'src/ApiBoundary.ts': {
    base: ['lonelight-docs/required', 'lonelight-docs/required', 'lonelight-docs/required'],
    project: ['no-restricted-globals', 'no-restricted-imports', 'no-restricted-syntax'],
  },
  'src/api/AxiosClient.ts': { base: ['lonelight-docs/required', 'lonelight-docs/required'] },
  'src/StoreNames.ts': {
    base: ['lonelight-docs/required'],
    project: ['lonelight/pinia-store-naming', 'lonelight/pinia-store-naming'],
  },
  'src/PromiseHandling.ts': {
    project: ['@typescript-eslint/no-floating-promises', '@typescript-eslint/no-floating-promises'],
  },
  'src/components/UnhandledRequest.vue': {
    project: ['@typescript-eslint/no-floating-promises'],
  },
  'src/DocumentationIssues.ts': {
    base: [
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/required',
      'lonelight-docs/layout',
    ],
  },
  'src/components/UndocumentedActions.vue': {
    base: ['lonelight-docs/required', 'lonelight-docs/required'],
  },
  'src/FormatExample.ts': {},
  'src/api/status.ts': {},
  'src/components/DraftEditor.vue': {},
  'src/components/EmptyState.vue': {},
  'src/components/StatusCard.vue': {},
  'src/components/draft.ts': {},
  'tools/OutsideProject.ts': {},
}
