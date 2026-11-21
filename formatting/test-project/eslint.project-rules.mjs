const sourceFiles = ['src/**/*.{ts,tsx,vue}']
// 转义子路径的斜杠，避免它结束 ESLint 选择器中的正则表达式
const axiosSpecifier = String.raw`^axios(?:\u002F|$)`
const axiosMessage = '只能在 src/api/ 下导入 Axios'

/**
 * 将全局 `fetch` 和 Axios 导入限制在 `src/api/`
 */
export const apiRequestBoundary = {
  name: 'lonelight/api-request-boundary',
  files: sourceFiles,
  ignores: ['src/api/**'],
  languageOptions: {
    globals: { self: 'readonly', window: 'readonly' },
  },
  rules: {
    'no-restricted-globals': [
      'error',
      {
        globals: [{ name: 'fetch', message: '只能在 src/api/ 下调用全局 fetch' }],
        checkGlobalObject: true,
      },
    ],
    'no-restricted-imports': [
      'error',
      { patterns: [{ regex: axiosSpecifier, caseSensitive: true, message: axiosMessage }] },
    ],
    'no-restricted-syntax': [
      'error',
      {
        selector: `ImportExpression > Literal.source[value=/${axiosSpecifier}/]`,
        message: axiosMessage,
      },
      {
        selector: `ImportExpression > TemplateLiteral.source[expressions.length=0] > TemplateElement[value.cooked=/${axiosSpecifier}/]`,
        message: axiosMessage,
      },
    ],
  },
}

/**
 * @typedef {Parameters<import('eslint').SourceCode['getScope']>[0]} AstNode
 */
/**
 * @typedef {Extract<import('eslint').Scope.Variable['defs'][number], { type: 'ImportBinding' }>['node']} ImportNode
 */

/**
 * 沿值作用域查找 Pinia 导入，同名局部变量会遮蔽导入
 *
 * @param {import('eslint').SourceCode} sourceCode - 用于查询节点作用域的 ESLint 源码对象
 * @param {AstNode} identifier - 待查找来源的语法节点，例如导入别名 `makeStore`
 * @returns {ImportNode | undefined} 对应的 Pinia 值导入项，找不到时返回 `undefined`
 */
function findPiniaImport(sourceCode, identifier) {
  if (identifier.type !== 'Identifier') {
    return undefined
  }

  /**
   * @type {import('eslint').Scope.Scope | null}
   */
  let scope = sourceCode.getScope(identifier)
  for (; scope; scope = scope.upper) {
    /**
     * @type {{ defs: import('eslint').Scope.Variable['defs'], isValueVariable?: boolean } | undefined}
     */
    const variable = scope.set.get(identifier.name)
    if (!variable || variable.isValueVariable === false) {
      continue
    }

    const definition = variable.defs[0]
    if (
      definition?.type === 'ImportBinding' &&
      definition.parent?.type === 'ImportDeclaration' &&
      definition.parent.source.value === 'pinia' &&
      !('importKind' in definition.parent && definition.parent.importKind === 'type') &&
      !('importKind' in definition.node && definition.node.importKind === 'type')
    ) {
      return definition.node
    }
    return undefined
  }
}

/**
 * 识别命名导入和命名空间导入中的 `defineStore` 调用
 *
 * @param {import('eslint').SourceCode} sourceCode - 用于追踪导入来源的 ESLint 源码对象
 * @param {AstNode} callee - 被调用的语法节点，例如 `makeStore` 或 `pinia.defineStore`
 * @returns {boolean} 调用 Pinia 的 `defineStore` 时返回 `true`，其余情况返回 `false`
 */
function callsPiniaDefineStore(sourceCode, callee) {
  if (callee.type === 'Identifier') {
    const specifier = findPiniaImport(sourceCode, callee)
    return (
      specifier?.type === 'ImportSpecifier' &&
      (specifier.imported.type === 'Identifier'
        ? specifier.imported.name
        : specifier.imported.value) === 'defineStore'
    )
  }

  if (callee.type === 'MemberExpression') {
    const { computed, property } = callee
    const isDefineStore = computed
      ? property.type === 'Literal' && property.value === 'defineStore'
      : 'name' in property && property.name === 'defineStore'
    return (
      isDefineStore &&
      findPiniaImport(sourceCode, callee.object)?.type === 'ImportNamespaceSpecifier'
    )
  }

  return false
}

/**
 * @type {import('eslint').Rule.RuleModule}
 */
const piniaNamingRule = {
  meta: {
    type: 'suggestion',
    schema: [],
    messages: { storeName: 'Pinia Store 变量名应采用 use<业务名>Store 格式，例如 useUserStore' },
  },
  /**
   * 为 ESLint 注册 Pinia Store 命名检查
   *
   * @param {import('eslint').Rule.RuleContext} context - ESLint 提供的规则上下文，用于读取源码和报告违规
   * @returns {import('eslint').Rule.RuleListener} 包含变量声明检查的节点访问器
   */
  create(context) {
    /**
     * 检查直接接收 Pinia `defineStore()` 返回值的变量名
     *
     * @param {AstNode} node - ESLint 传入的语法节点
     */
    function checkVariableDeclaration(node) {
      if (node.type !== 'VariableDeclarator') {
        return
      }

      const { id, init } = node
      if (
        id.type === 'Identifier' &&
        init?.type === 'CallExpression' &&
        !/^use[A-Z][A-Za-z0-9]*Store$/.test(id.name) &&
        callsPiniaDefineStore(context.sourceCode, init.callee)
      ) {
        context.report({ node: id, messageId: 'storeName' })
      }
    }

    return { VariableDeclarator: checkVariableDeclaration }
  },
}

/**
 * 检查直接接收 `defineStore()` 返回值的变量名
 */
export const piniaStoreNaming = {
  name: 'lonelight/pinia-store-naming',
  files: sourceFiles,
  plugins: { lonelight: { rules: { 'pinia-store-naming': piniaNamingRule } } },
  rules: { 'lonelight/pinia-store-naming': 'error' },
}

const promiseFiles = ['src/**/*.{ts,vue}']

/**
 * 使用项目的 TypeScript 类型信息检查未处理的 Promise
 */
export const promiseHandling = [
  {
    name: 'lonelight/promise-handling',
    files: promiseFiles,
    rules: { '@typescript-eslint/no-floating-promises': ['error', { ignoreVoid: false }] },
  },
  // 规则范围外的文件沿用普通解析，避免要求它们纳入 `tsconfig`
  {
    name: 'lonelight/promise-handling/limit-project-service',
    files: ['**/*.{ts,tsx,mts,vue}'],
    ignores: promiseFiles,
    languageOptions: { parserOptions: { projectService: false } },
  },
]
