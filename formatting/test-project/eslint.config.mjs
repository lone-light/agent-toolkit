import { dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { globalIgnores } from 'eslint/config'
import { withVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from 'eslint-config-prettier/flat'
import checkFile from 'eslint-plugin-check-file'
import { importX } from 'eslint-plugin-import-x'
import pluginVue from 'eslint-plugin-vue'
import ts from 'typescript'
import { apiRequestBoundary, piniaStoreNaming, promiseHandling } from './eslint.project-rules.mjs'

const configDirectory = dirname(fileURLToPath(import.meta.url))
const scriptFiles = ['**/*.{js,mjs,cjs,jsx,ts,mts,cts,tsx,vue}']
const documentationSourceIgnores = [
  '**/*.d.{ts,mts,cts}',
  '**/*.{spec,test}.{js,mjs,cjs,jsx,ts,mts,cts,tsx,vue}',
  '**/{test,tests,__tests__}/**',
]
const documentationTagOrder = ['param', 'returns', 'throws']
const documentationTagAliases = new Map([
  ['arg', 'param'],
  ['argument', 'param'],
  ['return', 'returns'],
  ['exception', 'throws'],
])
const inlineJSDocDirective =
  /^\*\s*@(type|typedef|satisfies|import|license|preserve|copyright|jsx|jsxFrag|jsxRuntime|jsxImportSource|ts-check|ts-nocheck)(?=\s|$)/

/**
 * 读取 JSDoc 正文，保留换行对应的源码位置
 *
 * @param {string} value - ESLint 的块注释内容
 */
function documentationLines(value) {
  return value
    .slice(1)
    .split(/\r\n|[\r\n\u2028\u2029]/)
    .map((line) =>
      line
        .trim()
        .replace(/^\*\s?/, '')
        .trimEnd(),
    )
}

/**
 * 读取实际的 JSDoc 标签，跳过 Markdown 示例中的内容
 *
 * @param {string} value - ESLint 的块注释内容
 */
function documentationTags(value) {
  /** @type {{ name: string; text: string; line: number; continued: boolean }[]} */
  const tags = []
  let fence = ''
  for (const [line, content] of documentationLines(value).entries()) {
    const marker = content.match(/^\s*(`{3,}|~{3,})(.*)$/)
    if (fence) {
      if (marker?.[1][0] === fence[0] && marker[1].length >= fence.length && !marker[2].trim())
        fence = ''
      continue
    }
    if (marker) {
      fence = marker[1]
      continue
    }
    if (tags.at(-1)?.name === 'example' && /^(?: {4}|\t)/.test(content)) continue
    const tag = content.match(/^\s*@([A-Za-z][\w-]*)(?:\s+(.*))?$/)
    if (tag) {
      const name = documentationTagAliases.get(tag[1]) ?? tag[1]
      tags.push({ name, text: tag[2]?.trim() ?? '', line, continued: false })
    } else if (content.trim() && tags.length) {
      tags[tags.length - 1].continued = true
    }
  }
  return tags
}

/**
 * 取标签的非类型部分，识别对象类型和字符串中的括号
 *
 * @param {string} text - `@param`、`@returns` 或 `@throws` 的内容
 */
function withoutDocumentationTagType(text) {
  if (!text.startsWith('{')) return text.trim()
  let depth = 0
  let quote = ''
  for (let index = 0; index < text.length; index += 1) {
    const character = text[index]
    if (quote) {
      if (character === '\\') index += 1
      else if (character === quote) quote = ''
    } else if (character === '"' || character === "'" || character === '`') {
      quote = character
    } else if (character === '{') {
      depth += 1
    } else if (character === '}') {
      depth -= 1
      if (depth === 0) return text.slice(index + 1).trim()
    }
  }
  return ''
}

/**
 * 忽略标签中用于占位的标点和连接符
 *
 * @param {string} text - 去除参数名或类型后的标签内容
 * @returns {string} 标签说明，仅含占位字符时返回空字符串
 */
function documentationDescription(text) {
  const description = text.trim().replace(/^-\s*/, '').trim()
  return /^[\s\p{P}\u2212]*$/u.test(description) ? '' : description
}

/**
 * 判断单句说明是否带有应省略的句号
 *
 * @param {string} text - 摘要或标签说明
 * @returns {boolean} 单句说明以句号结尾时返回 true
 */
function hasShortDocumentationPeriod(text) {
  const prose = text.replace(/(`+)[\s\S]*?\1/g, '代码').trim()
  return (
    /[。.]$/.test(prose) &&
    !prose.endsWith('...') &&
    !/[。！？]|[.?!]["'”’）)]*\s+\S/.test(prose.slice(0, -1))
  )
}

/**
 * 检查文档注释的起止分隔符是否各占一行
 *
 * @type {import('eslint').Rule.RuleModule}
 */
const jsdocLayoutRule = {
  meta: {
    type: 'layout',
    schema: [],
    messages: {
      delimiters: 'JSDoc 的 /** 和 */ 必须各占一行，注释正文写在中间',
      linePrefix: 'JSDoc 正文行应以 * 开头',
      singleSummary: '只有一行正文的 JSDoc 应写成三行',
      tagSpacing: 'JSDoc 正文和标签之间应空一行',
      tagOrder: 'JSDoc 标签应按 @param、@returns、@throws 排列',
      tagOneLine: 'JSDoc 的 @{{tag}} 标签和说明应写在同一行',
      emptyThrows: 'JSDoc 的 @throws 应说明触发异常或拒绝的条件',
      punctuation: 'JSDoc 的简短说明行末省略句号',
    },
  },
  create(context) {
    const { sourceCode } = context
    return {
      Program() {
        for (const { type, value, loc } of sourceCode.getAllComments()) {
          if (type !== 'Block' || !loc || !value.startsWith('*') || value.startsWith('**')) continue
          const { start, end } = loc
          if (start.line === end.line && inlineJSDocDirective.test(value)) continue

          if (
            end.line - start.line < 2 ||
            sourceCode.lines[start.line - 1].trim() !== '/**' ||
            sourceCode.lines[end.line - 1].trim() !== '*/'
          ) {
            context.report({ loc, messageId: 'delimiters' })
            continue
          }

          const body = sourceCode.lines.slice(start.line, end.line - 1)
          const nonemptyLines = body.map((line) => line.trim()).filter(Boolean)
          if (nonemptyLines.some((line) => !line.startsWith('*'))) {
            context.report({ loc, messageId: 'linePrefix' })
            continue
          }

          const content = nonemptyLines.map((line) => line.slice(1).trim()).filter(Boolean)
          if (body.length > 1 && content.length === 1 && !content[0].startsWith('@')) {
            context.report({ loc, messageId: 'singleSummary' })
          }

          const lines = documentationLines(value)
          const tags = documentationTags(value)
          const summary = lines
            .slice(0, tags[0]?.line)
            .map((line) => line.trim())
            .filter(Boolean)
          if (summary.length === 1 && hasShortDocumentationPeriod(summary[0])) {
            context.report({ loc, messageId: 'punctuation' })
          }
          if (tags.some(({ name }) => documentationTagOrder.includes(name))) {
            const firstTagLine = tags[0].line
            if (
              summary.length &&
              (lines[firstTagLine - 1].trim() || !lines[firstTagLine - 2]?.trim())
            ) {
              context.report({ loc, messageId: 'tagSpacing' })
            }
          }

          let lastTag = -1
          for (const tag of tags) {
            const tagLine = start.line + tag.line
            const atTag = { line: tagLine, column: sourceCode.lines[tagLine - 1].indexOf('@') }
            const order = documentationTagOrder.indexOf(tag.name)
            if (order === -1) {
              if (
                (tag.name === 'summary' || tag.name === 'description') &&
                !tag.continued &&
                hasShortDocumentationPeriod(tag.text)
              ) {
                context.report({ loc: atTag, messageId: 'punctuation' })
              }
              continue
            }
            if (order < lastTag) context.report({ loc: atTag, messageId: 'tagOrder' })
            lastTag = Math.max(lastTag, order)
            if (tag.continued) {
              context.report({ loc: atTag, messageId: 'tagOneLine', data: { tag: tag.name } })
            }
            const description =
              tag.name === 'param'
                ? readDocumentationParameter(tag.text).description
                : documentationDescription(withoutDocumentationTagType(tag.text))
            if (tag.name === 'throws' && !description) {
              context.report({ loc: atTag, messageId: 'emptyThrows' })
            }
            if (hasShortDocumentationPeriod(description) && !tag.continued) {
              context.report({ loc: atTag, messageId: 'punctuation' })
            }
          }
        }
      },
    }
  },
}

/**
 * @typedef {import('@typescript-eslint/types').TSESTree.Node} DocumentationNode
 * @typedef {import('@typescript-eslint/types').TSESTree.ClassDeclaration | import('@typescript-eslint/types').TSESTree.ClassExpression} DocumentationClass
 * @typedef {{ name?: string; node: DocumentationNode; anchors: number[]; callable: DocumentationNode }} DocumentedBinding
 */

/**
 * 去除不改变函数或类形态的 TypeScript 表达式包装
 *
 * @param {DocumentationNode} expression - 变量初始值或默认导出的表达式
 */
function unwrapDocumentationExpression(expression) {
  switch (expression.type) {
    case 'TSAsExpression':
    case 'TSInstantiationExpression':
    case 'TSNonNullExpression':
    case 'TSSatisfiesExpression':
    case 'TSTypeAssertion':
      return unwrapDocumentationExpression(expression.expression)
    default:
      return expression
  }
}

/**
 * 取得函数表达式及其定义位置
 *
 * @param {DocumentationNode} expression - 函数初始值或导出值
 * @param {number[]} anchors - 定义或导出开头的位置
 * @param {DocumentationNode} [nameNode] - 具有独立名称的定义
 */
function documentationFunctionExpression(expression, anchors, nameNode) {
  const callable = unwrapDocumentationExpression(expression)
  if (callable.type !== 'ArrowFunctionExpression' && callable.type !== 'FunctionExpression')
    return undefined
  return { node: nameNode ?? callable, anchors: [...anchors, callable.range[0]], callable }
}

/**
 * 取得变量定义中的函数
 *
 * @param {import('@typescript-eslint/types').TSESTree.VariableDeclarator} node - 变量定义
 * @param {number} [exportStart] - 导出语句起始位置
 * @returns {DocumentedBinding | undefined} 具名函数及其文档位置
 */
function documentationVariableBinding(node, exportStart) {
  if (node.id.type !== 'Identifier' || !node.init) return undefined
  const statement = node.parent
  const anchors = [node.range[0]]
  if (statement.declarations[0] === node) {
    anchors.unshift(
      exportStart ?? documentationDeclarationAnchors(statement)[0],
      statement.range[0],
    )
  }
  const binding = documentationFunctionExpression(node.init, anchors, node.id)
  return binding && { ...binding, name: node.id.name }
}

/**
 * 识别当前模块直接定义的函数
 *
 * @param {DocumentationNode} declaration - 顶层声明或默认导出表达式
 * @param {number} exportStart - 导出语句或声明开头的位置
 * @returns {DocumentedBinding[]} 有独立文档位置的声明
 */
function documentationBindings(declaration, exportStart) {
  switch (declaration.type) {
    case 'VariableDeclaration':
      return declaration.declarations
        .map((node) => documentationVariableBinding(node, exportStart))
        .filter((binding) => binding !== undefined)
    case 'FunctionDeclaration':
    case 'TSDeclareFunction':
      return [
        {
          name: declaration.id?.name,
          node: declaration.id ?? declaration,
          anchors: [exportStart, declaration.range[0]],
          callable: declaration,
        },
      ]
    default: {
      const binding = documentationFunctionExpression(declaration, [exportStart])
      return binding ? [binding] : []
    }
  }
}

/**
 * 查找定义前带有用途摘要的 JSDoc
 *
 * @param {import('eslint').SourceCode} sourceCode - ESLint 解析后的源码
 * @param {number} start - 定义起始令牌的偏移量
 */
function findDocumentationSummary(sourceCode, start) {
  const token = sourceCode.getTokenByRangeStart(start)
  if (!token) return undefined
  const doc = sourceCode
    .getCommentsBefore(token)
    .findLast(({ type, value }) => type === 'Block' && /^\*(?!\*)/.test(value))
  if (!doc?.loc) return undefined
  const previous = sourceCode.getTokenBefore(token)
  const openingLine = doc.loc.start.line
  if (openingLine !== token.loc.start.line && previous?.loc.end.line === openingLine)
    return undefined

  const summary = doc.value
    .slice(1)
    .replace(/^[\t ]*\*[\t ]?/gm, '')
    .trim()
  if (summary && (!summary.startsWith('@') || /^@(?:summary|description)\s+[^@\s]/.test(summary)))
    return doc
}

/**
 * 判断声明是否已有用途说明
 *
 * @param {import('eslint').SourceCode} sourceCode - ESLint 解析后的源码
 * @param {number} start - 声明起始令牌的偏移量
 */
function hasDocumentationSummary(sourceCode, start) {
  return Boolean(findDocumentationSummary(sourceCode, start))
}

/**
 * 解开参数上的默认值、剩余参数和 TypeScript 参数属性
 *
 * @param {DocumentationNode} node - 签名或解构模式中的参数
 * @returns {DocumentationNode} 参数或解构模式
 */
function unwrapDocumentationParameter(node) {
  switch (node.type) {
    case 'AssignmentPattern':
      return unwrapDocumentationParameter(node.left)
    case 'RestElement':
      return unwrapDocumentationParameter(node.argument)
    case 'TSParameterProperty':
      return unwrapDocumentationParameter(node.parameter)
    default:
      return node
  }
}

/**
 * 验证 JSDoc 子路径是否对应解构模式中的属性，而非解构后的本地变量名
 *
 * @param {DocumentationNode} parameter - 解构模式或其中一个子属性的值
 * @param {string[]} path - 根参数名之后的属性路径
 * @returns {boolean} 路径可与模式对应时返回 true
 */
function matchesDocumentationParameterPath(parameter, path) {
  if (path.length === 0) return true
  const pattern = unwrapDocumentationParameter(parameter)
  const [part, ...remaining] = path
  const type =
    'typeAnnotation' in pattern && pattern.typeAnnotation?.type === 'TSTypeAnnotation'
      ? pattern.typeAnnotation.typeAnnotation
      : pattern
  if (type.type === 'TSNamedTupleMember') {
    return matchesDocumentationParameterPath(type.elementType, path)
  }
  if (
    type.type === 'TSOptionalType' ||
    (type.type === 'TSTypeOperator' && type.operator === 'readonly')
  ) {
    return !type.typeAnnotation || matchesDocumentationParameterPath(type.typeAnnotation, path)
  }
  if (type.type === 'TSTypeLiteral') {
    let open = false
    for (const member of type.members) {
      if (member.type === 'TSIndexSignature') open = true
      if (member.type !== 'TSPropertySignature' && member.type !== 'TSMethodSignature') continue
      const name = documentationMemberName(member)
      if (name === undefined) open = true
      else if (name === part) return matchesDocumentationParameterPath(member, remaining)
    }
    return open
  }
  if (type.type === 'TSArrayType') {
    return (
      (part === '*' || /^(?:0|[1-9]\d*)$/.test(part)) &&
      matchesDocumentationParameterPath(type.elementType, remaining)
    )
  }
  if (type.type === 'TSTupleType') {
    const { elementTypes } = type
    if (part === '*') {
      return elementTypes.some((element) => matchesDocumentationParameterPath(element, remaining))
    }
    if (!/^(?:0|[1-9]\d*)$/.test(part)) return false
    const index = Number(part)
    const rest = elementTypes.findIndex(
      (element) =>
        (element.type === 'TSNamedTupleMember' ? element.elementType : element).type ===
        'TSRestType',
    )
    if (rest !== -1 && index >= rest) return true
    return Boolean(
      elementTypes[index] && matchesDocumentationParameterPath(elementTypes[index], remaining),
    )
  }
  if (pattern.type === 'ObjectPattern') {
    let open = false
    for (const property of pattern.properties) {
      const name = property.type === 'Property' ? documentationMemberName(property) : undefined
      if (name === undefined) open = true
      else if (name === part && property.type === 'Property') {
        return matchesDocumentationParameterPath(property.value, remaining)
      }
    }
    return open
  }
  if (pattern.type === 'ArrayPattern') {
    if (part === '*') {
      return pattern.elements.some(
        (entry) => entry && matchesDocumentationParameterPath(entry, remaining),
      )
    }
    if (!/^(?:0|[1-9]\d*)$/.test(part)) return false
    const index = Number(part)
    const rest = pattern.elements.findIndex((entry) => entry?.type === 'RestElement')
    const element = pattern.elements[rest !== -1 && index >= rest ? rest : index]
    return Boolean(element && matchesDocumentationParameterPath(element, remaining))
  }
  return true
}

/**
 * 将文档中点号和方括号写法解析为同一组属性路径
 *
 * @param {string} name - 参数或参数子属性的文档名称
 * @returns {string[] | undefined} 参数名称和属性路径，无效写法返回 undefined
 */
function documentationParameterPath(name) {
  const root = /^[$_\p{ID_Start}][$\p{ID_Continue}\u200c\u200d]*/u.exec(name)?.[0]
  if (!root) return undefined
  const path = [root]
  const part = /\.([^.[\]\s]+)|\[(?:(\d*)|"((?:[^"\\]|\\.)*)"|'((?:[^'\\]|\\.)*)')\]/uy
  part.lastIndex = root.length
  while (part.lastIndex < name.length) {
    const next = part.exec(name)
    if (!next) return undefined
    const [, dotted, index, doubleQuoted, singleQuoted] = next
    const quoted = doubleQuoted ?? singleQuoted
    path.push(dotted ?? (quoted !== undefined ? quoted.replace(/\\(["'\\])/g, '$1') : index || '*'))
  }
  return path
}

/**
 * 解析参数标签中可选的类型、带默认值的名称以及用途
 *
 * @param {string} text - JSDoc 标签内容
 * @returns {{ name: string; description: string; separated: boolean }} 参数路径、说明和分隔符
 */
function readDocumentationParameter(text) {
  const body = withoutDocumentationTagType(text)
  const optional = body.startsWith('[')
  let endOfName = optional ? 0 : body.length
  let defaultStart
  let depth = 0
  let quote = ''
  for (let index = 0; index < body.length; index++) {
    const character = body[index]
    if (quote) {
      if (character === '\\') index++
      else if (character === quote) quote = ''
    } else if (depth && (character === '"' || character === "'" || character === '\x60')) {
      quote = character
    } else if (character === '[') {
      depth++
    } else if (character === ']') {
      depth--
      if (optional && depth === 0) {
        endOfName = index + 1
        break
      }
    } else if (optional && depth === 1 && character === '=') {
      defaultStart ??= index
    } else if (!depth && /\s/.test(character)) {
      endOfName = index
      break
    }
  }
  if (!endOfName) return { name: '', description: '', separated: false }
  const afterName = body.slice(endOfName)
  return {
    name: optional ? body.slice(1, defaultStart ?? endOfName - 1).trim() : body.slice(0, endOfName),
    description: documentationDescription(afterName),
    separated: /^[ \t]+-[ \t]+/.test(afterName),
  }
}

/**
 * 根据显式返回类型判断签名是否提供返回值
 *
 * @param {DocumentationNode} node - TypeScript 返回类型或其包装节点
 * @param {import('eslint').SourceCode} sourceCode - 当前源码及变量作用域
 * @returns {boolean | undefined} 未知类型交给类型服务或后续语法分析
 */
function documentationReturnAnnotation(node, sourceCode) {
  switch (node.type) {
    case 'TSTypeAnnotation':
      return documentationReturnAnnotation(node.typeAnnotation, sourceCode)
    case 'TSVoidKeyword':
    case 'TSUndefinedKeyword':
    case 'TSNeverKeyword':
      return false
    case 'TSUnionType': {
      const variants = node.types.map((type) => documentationReturnAnnotation(type, sourceCode))
      return variants.includes(true)
        ? true
        : variants.every((result) => result === false)
          ? false
          : undefined
    }
    case 'TSTypeReference':
      if (
        node.typeName.type === 'Identifier' &&
        (node.typeName.name === 'Promise' || node.typeName.name === 'PromiseLike') &&
        node.typeArguments?.params[0] &&
        isDocumentationBuiltin(node.typeName, sourceCode, 'type')
      )
        return documentationReturnAnnotation(node.typeArguments.params[0], sourceCode)
      return undefined
    case 'TSBooleanKeyword':
    case 'TSBigIntKeyword':
    case 'TSNumberKeyword':
    case 'TSStringKeyword':
    case 'TSSymbolKeyword':
    case 'TSNullKeyword':
    case 'TSObjectKeyword':
    case 'TSLiteralType':
    case 'TSTypePredicate':
    case 'TSTemplateLiteralType':
    case 'TSTypeLiteral':
    case 'TSArrayType':
    case 'TSTupleType':
    case 'TSFunctionType':
    case 'TSConstructorType':
    case 'TSMappedType':
      return true
    default:
      return undefined
  }
}

/**
 * 判断标识符是否使用全局内建名称
 *
 * @param {import('@typescript-eslint/types').TSESTree.Identifier} node - 表达式中的标识符
 * @param {import('eslint').SourceCode} sourceCode - 当前源码及变量作用域
 * @param {'type' | 'value'} [namespace] - 查找 TypeScript 类型或运行时值绑定
 * @returns {boolean} 当前模块未为该名称定义其他绑定时返回 true
 */
function isDocumentationBuiltin(node, sourceCode, namespace = 'value') {
  const reference = /** @type {import('estree').Identifier} */ (/** @type {unknown} */ (node))
  /** @type {import('eslint').Scope.Scope | null} */
  let scope = sourceCode.getScope(reference)
  while (scope) {
    const shadowed = scope.set.get(node.name)?.defs.some((definition) => {
      if (namespace === 'type') {
        return !('isTypeDefinition' in definition) || definition.isTypeDefinition === true
      }
      if (definition.type === 'ImportBinding') {
        const { node: specifier, parent } = definition
        if (
          ('importKind' in parent && parent.importKind === 'type') ||
          ('importKind' in specifier && specifier.importKind === 'type')
        )
          return false
      }
      return !('isVariableDefinition' in definition) || definition.isVariableDefinition === true
    })
    if (shadowed) return false
    scope = scope.upper
  }
  return true
}

/**
 * 识别无类型服务时可以确定有无返回值的 JavaScript 表达式
 *
 * @param {DocumentationNode} node - return 的表达式或箭头函数的隐式返回值
 * @param {import('eslint').SourceCode} sourceCode - 当前源码及变量作用域
 * @param {boolean} unwrapPromise - 当前表达式的结果会被异步解包
 * @returns {boolean | undefined} 无法确认时返回 undefined
 */
function documentationValueExpression(node, sourceCode, unwrapPromise) {
  switch (node.type) {
    case 'Literal':
    case 'TemplateLiteral':
    case 'ArrayExpression':
    case 'FunctionExpression':
    case 'ArrowFunctionExpression':
    case 'ClassExpression':
    case 'BinaryExpression':
    case 'UpdateExpression':
    case 'JSXElement':
    case 'JSXFragment':
      return true
    case 'Identifier':
      return node.name === 'undefined' && isDocumentationBuiltin(node, sourceCode)
        ? false
        : undefined
    case 'ObjectExpression':
      return unwrapPromise &&
        node.properties.some((property) => {
          if (property.type !== 'Property') return true
          const name = documentationMemberName(property)
          return name === undefined || name === 'then'
        })
        ? undefined
        : true
    case 'NewExpression':
      if (
        node.callee.type === 'Identifier' &&
        node.callee.name === 'Promise' &&
        isDocumentationBuiltin(node.callee, sourceCode)
      ) {
        const fulfilledType = node.typeArguments?.params[0]
        return fulfilledType ? documentationReturnAnnotation(fulfilledType, sourceCode) : undefined
      }
      return unwrapPromise ? undefined : true
    case 'CallExpression': {
      const { callee } = node
      if (
        callee.type !== 'MemberExpression' ||
        callee.object.type !== 'Identifier' ||
        callee.object.name !== 'Promise' ||
        !isDocumentationBuiltin(callee.object, sourceCode)
      )
        return undefined
      const method = documentationMemberName({ key: callee.property, computed: callee.computed })
      if (method === 'reject') return false
      if (method !== 'resolve') return undefined
      const argument = node.arguments[0]
      if (!argument) return false
      return argument.type === 'SpreadElement'
        ? undefined
        : documentationValueExpression(argument, sourceCode, true)
    }
    case 'AwaitExpression':
      return documentationValueExpression(node.argument, sourceCode, true)
    case 'UnaryExpression':
      return node.operator !== 'void'
    case 'LogicalExpression': {
      const { left, operator, right } = node
      const leftIsUndefined =
        (left.type === 'UnaryExpression' && left.operator === 'void') ||
        (left.type === 'Identifier' &&
          left.name === 'undefined' &&
          isDocumentationBuiltin(left, sourceCode))
      if (left.type === 'Literal' || leftIsUndefined) {
        const value = left.type === 'Literal' ? left.value : undefined
        const useRight =
          operator === '??' ? value == null : operator === '&&' ? Boolean(value) : !value
        return documentationValueExpression(useRight ? right : left, sourceCode, unwrapPromise)
      }
      if (
        left.type === 'NewExpression' ||
        left.type === 'ObjectExpression' ||
        left.type === 'ArrayExpression' ||
        left.type === 'FunctionExpression' ||
        left.type === 'ArrowFunctionExpression' ||
        left.type === 'ClassExpression'
      ) {
        return documentationValueExpression(
          operator === '&&' ? right : left,
          sourceCode,
          unwrapPromise,
        )
      }
      const leftValue = documentationValueExpression(left, sourceCode, unwrapPromise)
      const rightValue = documentationValueExpression(right, sourceCode, unwrapPromise)
      if (rightValue === true && (leftValue === true || (operator !== '&&' && !unwrapPromise)))
        return true
      return undefined
    }
    case 'ConditionalExpression': {
      if (node.test.type === 'Literal') {
        return documentationValueExpression(
          node.test.value ? node.consequent : node.alternate,
          sourceCode,
          unwrapPromise,
        )
      }
      const consequent = documentationValueExpression(node.consequent, sourceCode, unwrapPromise)
      const alternate = documentationValueExpression(node.alternate, sourceCode, unwrapPromise)
      if (consequent === true || alternate === true) return true
      return consequent === false && alternate === false ? false : undefined
    }
    case 'SequenceExpression':
      return documentationValueExpression(
        node.expressions.at(-1) ?? node.expressions[0],
        sourceCode,
        unwrapPromise,
      )
    case 'TSSatisfiesExpression':
    case 'TSNonNullExpression':
      return documentationValueExpression(node.expression, sourceCode, unwrapPromise)
    case 'TSAsExpression':
    case 'TSTypeAssertion':
      return (
        documentationReturnAnnotation(node.typeAnnotation, sourceCode) ??
        documentationValueExpression(node.expression, sourceCode, unwrapPromise)
      )
    default:
      return undefined
  }
}

/**
 * 判断当前语句能否继续执行同一块中的后续语句
 *
 * @param {DocumentationNode} node - 当前语句或代码块
 * @returns {boolean} 可以确定无法执行后续语句时返回 true
 */
function documentationStatementStops(node) {
  switch (node.type) {
    case 'ReturnStatement':
    case 'ThrowStatement':
    case 'BreakStatement':
    case 'ContinueStatement':
      return true
    case 'BlockStatement':
      return node.body.some(documentationStatementStops)
    case 'IfStatement':
      if (node.test.type === 'Literal') {
        const branch = node.test.value ? node.consequent : node.alternate
        return Boolean(branch && documentationStatementStops(branch))
      }
      return Boolean(
        node.alternate &&
        documentationStatementStops(node.consequent) &&
        documentationStatementStops(node.alternate),
      )
    case 'TryStatement':
      return Boolean(node.finalizer && documentationStatementStops(node.finalizer))
    default:
      return false
  }
}

/**
 * 搜索当前函数的 return，跳过内层函数和类
 *
 * @param {DocumentationNode} node - 函数体或其子节点
 * @param {import('eslint').SourceCode} sourceCode - 当前源码、节点类型和变量作用域
 * @param {boolean} unwrapPromise - 当前表达式的结果会被异步解包
 * @returns {boolean | undefined} 无法静态确认返回值时返回 undefined
 */
function documentationBodyReturns(node, sourceCode, unwrapPromise) {
  switch (node.type) {
    case 'FunctionDeclaration':
    case 'FunctionExpression':
    case 'ArrowFunctionExpression':
    case 'ClassDeclaration':
    case 'ClassExpression':
      return false
    case 'ReturnStatement':
      return node.argument
        ? documentationValueExpression(node.argument, sourceCode, unwrapPromise)
        : false
    case 'BlockStatement': {
      let unknownValue = false
      for (const statement of node.body) {
        const result = documentationBodyReturns(statement, sourceCode, unwrapPromise)
        if (result === true) return true
        if (result === undefined) unknownValue = true
        if (documentationStatementStops(statement)) break
      }
      return unknownValue ? undefined : false
    }
    case 'IfStatement':
      if (node.test.type === 'Literal') {
        const branch = node.test.value ? node.consequent : node.alternate
        return branch ? documentationBodyReturns(branch, sourceCode, unwrapPromise) : false
      }
      break
    case 'TryStatement':
      if (node.finalizer && documentationStatementStops(node.finalizer)) {
        return documentationBodyReturns(node.finalizer, sourceCode, unwrapPromise)
      }
      break
  }
  const fields = /** @type {Record<string, unknown>} */ (/** @type {unknown} */ (node))
  let unknownValue = false
  for (const key of sourceCode.visitorKeys[node.type] ?? []) {
    const children = fields[key]
    for (const child of Array.isArray(children) ? children : [children]) {
      if (child && typeof child === 'object' && 'type' in child) {
        const result = documentationBodyReturns(
          /** @type {DocumentationNode} */ (child),
          sourceCode,
          unwrapPromise,
        )
        if (result === true) return true
        if (result === undefined) unknownValue = true
      }
    }
  }
  return unknownValue ? undefined : false
}

/**
 * 判断函数是否确定返回有意义的值，或确定没有返回值
 *
 * @param {DocumentationNode} node - 函数、访问器或类型签名
 * @param {import('eslint').SourceCode} sourceCode - 当前 ESLint 源码
 * @returns {boolean | undefined} 缺少足够信息时返回 undefined
 */
function documentationReturnValue(node, sourceCode) {
  const parent = node.parent
  if (
    node.type === 'TSConstructorType' ||
    node.type === 'TSConstructSignatureDeclaration' ||
    node.type === 'TSIndexSignature' ||
    ((parent?.type === 'MethodDefinition' || parent?.type === 'TSAbstractMethodDefinition') &&
      (parent.kind === 'constructor' || parent.kind === 'set')) ||
    (node.type === 'TSMethodSignature' && node.kind === 'set')
  )
    return false

  if ('generator' in node && node.generator) return true

  const definedReturnType = 'returnType' in node ? node.returnType : undefined
  const inferFromBody = () => {
    if (!('body' in node) || !node.body || Array.isArray(node.body)) return undefined
    const unwrapPromise = 'async' in node && node.async
    return node.body.type === 'BlockStatement'
      ? documentationBodyReturns(node.body, sourceCode, unwrapPromise)
      : documentationValueExpression(node.body, sourceCode, unwrapPromise)
  }
  const fromBody = definedReturnType ? undefined : inferFromBody()
  // TypeScript 的推断类型可能仍包含已被 finally 或常量分支屏蔽的值
  if (fromBody === false) return false

  const services =
    /** @type {Partial<import('@typescript-eslint/typescript-estree').ParserServices>} */ (
      sourceCode.parserServices
    )
  const checker = services.program?.getTypeChecker()
  const tsNode = services.esTreeNodeToTSNodeMap?.get(node)
  if (checker && ts.isFunctionLike(tsNode)) {
    const signature = checker.getSignatureFromDeclaration(tsNode)
    if (signature) {
      const type = checker.getReturnTypeOfSignature(signature)
      const value = checker.getAwaitedType(type) ?? type
      const variants = value.isUnion() ? value.types : [value]
      const noValue = ts.TypeFlags.Void | ts.TypeFlags.Undefined | ts.TypeFlags.Never
      const unknownValue = ts.TypeFlags.Any | ts.TypeFlags.Unknown | ts.TypeFlags.TypeParameter
      if (variants.every(({ flags }) => (flags & noValue) !== 0)) return false
      if (variants.some(({ flags }) => (flags & (noValue | unknownValue)) === 0)) return true
    }
  }

  return definedReturnType
    ? (documentationReturnAnnotation(definedReturnType, sourceCode) ?? inferFromBody())
    : fromBody
}

/**
 * 核对 JSDoc 参数顺序和名称，并检查可以确认有返回值的函数
 *
 * @param {import('eslint').Rule.RuleContext} context - 当前 ESLint 规则上下文
 * @param {DocumentationNode} callableNode - 函数、方法或签名
 * @param {import('estree').Comment} docComment - 已匹配的 JSDoc
 */
function checkSignatureDocumentation(context, callableNode, docComment) {
  if (!docComment.loc) return
  const loc = docComment.loc
  /**
   * @param {string} message - 诊断消息
   */
  const report = (message) => context.report({ loc, message })
  const node =
    callableNode.type === 'MethodDefinition' || callableNode.type === 'TSAbstractMethodDefinition'
      ? callableNode.value
      : callableNode
  const parameters = ('params' in node ? node.params : 'parameters' in node ? node.parameters : [])
    .map(unwrapDocumentationParameter)
    .filter((parameter) => parameter.type !== 'Identifier' || parameter.name !== 'this')
  const tags = documentationTags(docComment.value)
  const parameterTags = tags
    .filter(({ name }) => name === 'param')
    .map(({ text }) => readDocumentationParameter(text))
    .map((tag) => ({ ...tag, name: tag.name.replace(/^\.\.\./, '') }))
  const usedNames = new Set()
  const distinct = parameterTags.flatMap(({ name, description, separated }) => {
    if (!name || name === '-') {
      report('JSDoc 的 @param 缺少参数名')
      return []
    }
    const path = documentationParameterPath(name)
    if (!path) {
      report('JSDoc 的 @param ' + name + ' 参数路径无效')
      return []
    }
    const key = JSON.stringify(path)
    if (usedNames.has(key)) {
      report('JSDoc 重复说明参数 ' + name)
      return []
    }
    usedNames.add(key)
    if (!description) report('JSDoc 的 @param ' + name + ' 缺少说明')
    else if (!separated) report('JSDoc 的 @param ' + name + ' 应使用带空格的连字符分隔参数名和说明')
    return [{ name, path }]
  })
  const parameterNames = new Set(
    parameters.flatMap((parameter) => (parameter.type === 'Identifier' ? [parameter.name] : [])),
  )
  const roots = distinct.filter(({ path }) => path.length === 1)
  const rootsWithoutName = roots.filter(({ name }) => !parameterNames.has(name))
  /** @type {Map<string, DocumentationNode>} */
  const matched = new Map()
  let unnamed = 0
  for (const [index, parameter] of parameters.entries()) {
    const named = parameter.type === 'Identifier'
    const tag = named
      ? roots.find(({ name }) => name === parameter.name)
      : rootsWithoutName[unnamed++]
    if (tag) matched.set(tag.name, parameter)
    else
      report('JSDoc 缺少 @param ' + (named ? parameter.name : '第 ' + (index + 1) + ' 个解构参数'))
  }
  const expectedOrder = new Map(Array.from(matched.keys(), (name, index) => [name, index]))
  const seenRoots = new Set()
  let previousIndex = -1
  let orderMismatch = false
  for (const {
    name,
    path: [root, ...children],
  } of distinct) {
    const parameter = matched.get(root)
    const index = expectedOrder.get(root)
    if (
      !parameter ||
      index === undefined ||
      !matchesDocumentationParameterPath(parameter, children)
    ) {
      report('JSDoc 的 @param ' + name + ' 无法对应函数签名中的参数')
      continue
    }
    if (children.length === 0) seenRoots.add(root)
    if (!seenRoots.has(root) || index < previousIndex) orderMismatch = true
    previousIndex = Math.max(previousIndex, index)
  }
  if (orderMismatch) report('JSDoc 的 @param 顺序应与函数签名一致')

  const returnTags = tags.filter(({ name }) => name === 'returns')
  const hasReturnValue = documentationReturnValue(node, context.sourceCode)
  if (hasReturnValue === true && returnTags.length === 0) report('JSDoc 缺少 @returns 返回值说明')
  if (hasReturnValue === false && returnTags.length > 0) report('无返回值的签名无需添加 @returns')
  if (returnTags.length > 1) report('JSDoc 重复添加 @returns')
  if (returnTags.some(({ text }) => !documentationDescription(withoutDocumentationTagType(text)))) {
    report('JSDoc 的 @returns 缺少说明')
  }
}

/**
 * @typedef {import('@typescript-eslint/types').TSESTree.TypeElement} DocumentationTypeMember
 * @typedef {{ node: DocumentationNode; anchors: number[]; callable?: DocumentationNode; override?: boolean }} DeclarationDocumentation
 */

/**
 * 读取对象或类型成员的静态名称
 *
 * @param {{ key: DocumentationNode; computed: boolean }} member - 成员声明
 * @returns {string | undefined} 语法中可确定的名称
 */
function documentationMemberName({ key, computed }) {
  if (!computed && key.type === 'Identifier') return key.name
  if (key.type === 'PrivateIdentifier') return '#' + key.name
  if (key.type === 'Literal' && (typeof key.value === 'string' || typeof key.value === 'number'))
    return String(key.value)
  if (key.type === 'TemplateLiteral' && key.expressions.length === 0)
    return key.quasis[0]?.value.cooked ?? undefined
}

/**
 * 获取定义与导出包装的 JSDoc 位置
 *
 * @param {DocumentationNode} declaration - 定义节点
 */
function documentationDeclarationAnchors(declaration) {
  const parent = declaration.parent
  return parent?.type === 'ExportNamedDeclaration' ||
    parent?.type === 'ExportDefaultDeclaration' ||
    parent?.type === 'TSExportAssignment'
    ? [parent.range[0], declaration.range[0]]
    : [declaration.range[0]]
}

/**
 * 取得直接声明的函数类型，包括只额外允许空值的联合类型
 *
 * @param {DocumentationNode | undefined} type - 可能可调用的类型
 */
function documentationCallableType(type) {
  if (type?.type === 'TSUnionType') {
    const values = type.types.filter(
      ({ type }) => type !== 'TSNullKeyword' && type !== 'TSUndefinedKeyword',
    )
    if (values.length !== 1) return undefined
    type = values[0]
  }
  return type?.type === 'TSFunctionType' || type?.type === 'TSConstructorType' ? type : undefined
}

/**
 * @typedef {{ name?: string; value: DocumentationNode; anchor: number; accessor?: boolean }} CommonJsDocumentationExport
 * @typedef {Map<string | undefined, CommonJsDocumentationExport[]>} CommonJsExportObject
 */

/**
 * 识别作用域中未被遮蔽的 module.exports
 *
 * @param {DocumentationNode} node - 可能是 CommonJS 导出的引用
 * @param {Set<string>} localNames - 模块顶层的本地绑定
 */
function isDocumentationModuleExports(node, localNames) {
  return (
    node.type === 'MemberExpression' &&
    node.object.type === 'Identifier' &&
    node.object.name === 'module' &&
    !localNames.has('module') &&
    documentationMemberName({ key: node.property, computed: node.computed }) === 'exports'
  )
}

/**
 * 读取可见的 CommonJS 导出声明
 *
 * @param {DocumentationNode} value - 最终赋入模块或别名的值
 * @param {number} anchor - 赋值语句开头
 * @returns {CommonJsExportObject} 以属性名索引的定义
 */
function documentationExportObject(value, anchor) {
  if (value.type !== 'ObjectExpression') return new Map([[undefined, [{ value, anchor }]]])
  /** @type {CommonJsExportObject} */
  const properties = new Map()
  for (const property of literalObjectProperties(value)) {
    const name = documentationMemberName(property)
    if (name === undefined) continue
    const entries = properties.get(name) ?? []
    entries.push({
      name,
      value: unwrapDocumentationExpression(property.value),
      anchor: property.range[0],
      accessor: property.kind !== 'init',
    })
    properties.set(name, entries)
  }
  return properties
}

/**
 * 找出由顶层静态赋值确定的 CommonJS 导出
 *
 * @param {import('@typescript-eslint/types').TSESTree.Program['body']} statements - 模块顶层语句
 * @param {Set<string>} localNames - 模块顶层的本地绑定
 */
function commonJsDocumentationExports(statements, localNames) {
  /** @type {CommonJsExportObject} */
  let moduleExports = new Map()
  const hasAlias = !localNames.has('exports')
  let alias = hasAlias ? moduleExports : undefined

  /**
   * 查找声明中当前可定位的导出对象
   *
   * @param {DocumentationNode} node - 赋值表达式的对象或值
   */
  function reference(node) {
    if (hasAlias && node.type === 'Identifier' && node.name === 'exports') return alias
    if (isDocumentationModuleExports(node, localNames)) return moduleExports
  }

  for (const statement of statements) {
    if (statement.type !== 'ExpressionStatement') continue
    /** @type {{ target: DocumentationNode; object?: CommonJsExportObject }[]} */
    const assignments = []
    let value = unwrapDocumentationExpression(statement.expression)
    while (value.type === 'AssignmentExpression' && value.operator === '=') {
      const target = value.left
      // 先确定赋值左侧引用，右侧可能重新绑定 module.exports 或 exports
      const object = target.type === 'MemberExpression' ? reference(target.object) : undefined
      assignments.push({ target, object })
      value = unwrapDocumentationExpression(value.right)
    }

    let assigned = reference(value)
    for (const { target, object } of assignments.toReversed()) {
      if (isDocumentationModuleExports(target, localNames)) {
        assigned ??= documentationExportObject(value, statement.range[0])
        moduleExports = assigned
      } else if (hasAlias && target.type === 'Identifier' && target.name === 'exports') {
        assigned ??= documentationExportObject(value, statement.range[0])
        alias = assigned
      } else if (object && target.type === 'MemberExpression') {
        const name = documentationMemberName({ key: target.property, computed: target.computed })
        if (name !== undefined && !object.get(name)?.some(({ accessor }) => accessor)) {
          object.set(name, [{ name, value, anchor: statement.range[0] }])
        }
      }
    }
  }
  return [...moduleExports.values()].flat()
}

/**
 * 检查函数和类、接口及类型别名的 JSDoc
 *
 * @type {import('eslint').Rule.RuleModule}
 */
const jsdocRequiredRule = {
  meta: {
    type: 'suggestion',
    schema: [],
    messages: {
      missing: '导出的函数 {{name}} 缺少说明用途的 JSDoc',
      missingVueFunction: 'Vue 脚本函数 {{name}} 缺少说明用途的 JSDoc',
      inlineVueExposure: '请将 defineExpose 中的函数 {{name}} 定义为具名函数，并在定义处添加 JSDoc',
      missingClass: '类 {{name}} 缺少说明用途的 JSDoc',
      missingMethod: '类方法 {{name}} 缺少说明用途的 JSDoc',
      missingField: '类字段 {{name}} 缺少说明用途的 JSDoc',
      missingInterface: '接口 {{name}} 缺少说明用途的 JSDoc',
      missingInterfaceMember: '接口成员 {{name}} 缺少说明用途的 JSDoc',
      missingTypeAlias: '类型别名 {{name}} 缺少说明用途的 JSDoc',
      missingTypeMember: '类型成员 {{name}} 缺少说明用途的 JSDoc',
    },
  },
  create(context) {
    const { sourceCode } = context

    /**
     * 按定义位置查找说明，并使用拥有说明的签名校验标签
     *
     * @param {DeclarationDocumentation[]} declarations - 同一定义或重载组
     * @param {string} messageId - 缺少说明时使用的诊断
     * @param {string} name - 报告中显示的名称
     */
    function checkDeclarations(declarations, messageId, name) {
      const checked = new Set()
      for (const { anchors, callable } of declarations) {
        for (const start of new Set(anchors)) {
          const doc = findDocumentationSummary(sourceCode, start)
          if (!doc) continue
          if (!checked.has(doc) && callable) checkSignatureDocumentation(context, callable, doc)
          checked.add(doc)
          break
        }
      }
      if (!checked.size && !declarations.some(({ override }) => override)) {
        context.report({ loc: declarations[0].node.loc, messageId, data: { name } })
      }
    }

    /**
     * 检查类及显式声明的构造器、方法、访问器和字段
     *
     * @param {import('estree').ClassDeclaration | import('estree').ClassExpression} syntaxNode - ESLint 访问到的类节点
     */
    function checkClass(syntaxNode) {
      const node = /** @type {DocumentationClass} */ (/** @type {unknown} */ (syntaxNode))
      /** @type {DocumentationNode} */
      let declaration = node
      while (declaration.parent && unwrapDocumentationExpression(declaration.parent) === node) {
        declaration = declaration.parent
      }
      const anchors = [node.range[0], declaration.range[0]]
      let name = node.id?.name
      const parent = declaration.parent
      if (parent?.type === 'VariableDeclarator') {
        if (parent.id.type === 'Identifier') name ??= parent.id.name
        anchors.push(parent.range[0])
        const statement = parent.parent
        if (statement.type === 'VariableDeclaration' && statement.declarations[0] === parent) {
          declaration = statement
        }
      } else if (parent?.type === 'Property' && parent.value === declaration) {
        name ??= documentationMemberName(parent)
        anchors.push(parent.range[0])
      } else if (
        parent?.type === 'AssignmentExpression' &&
        parent.right === declaration &&
        parent.parent?.type === 'ExpressionStatement'
      ) {
        anchors.push(parent.parent.range[0])
      }
      anchors.push(...documentationDeclarationAnchors(declaration))
      checkDeclarations([{ node: node.id ?? node, anchors }], 'missingClass', name ?? '匿名类')

      /** @type {Map<string, { name: string; declarations: DeclarationDocumentation[]; overload: boolean }>} */
      const methods = new Map()
      for (const member of node.body.body) {
        if (member.type === 'StaticBlock') continue
        const override = 'override' in member && member.override === true
        const memberAnchors = [member.range[0]]
        if (member.type === 'TSIndexSignature') {
          checkDeclarations([{ node: member, anchors: memberAnchors }], 'missingField', '索引签名')
          continue
        }

        const staticName = documentationMemberName(member)
        const memberName = staticName ?? sourceCode.text.slice(...member.key.range)
        if (member.type === 'MethodDefinition' || member.type === 'TSAbstractMethodDefinition') {
          const identity = JSON.stringify([
            member.static,
            member.kind,
            staticName === undefined,
            memberName,
          ])
          const entry = { node: member, anchors: memberAnchors, callable: member.value, override }
          const overload = member.value.type === 'TSEmptyBodyFunctionExpression'
          const group = methods.get(identity)
          if (group) {
            group.declarations.push(entry)
            group.overload ||= overload
          } else {
            methods.set(identity, { name: memberName, declarations: [entry], overload })
          }
        } else {
          let signature
          if (member.value) {
            const value = unwrapDocumentationExpression(member.value)
            if (value.type === 'ArrowFunctionExpression' || value.type === 'FunctionExpression')
              signature = value
          }
          signature ??= documentationCallableType(member.typeAnnotation?.typeAnnotation)
          checkDeclarations(
            [{ node: member, anchors: memberAnchors, callable: signature }],
            'missingField',
            memberName,
          )
        }
      }

      for (const { name, declarations, overload } of methods.values()) {
        if (overload) checkDeclarations(declarations, 'missingMethod', name)
        else {
          for (const entry of declarations) checkDeclarations([entry], 'missingMethod', name)
        }
      }
    }

    /**
     * 按声明检查接口成员和对象类型成员
     *
     * @param {DocumentationTypeMember[]} members - 当前对象显式定义的成员
     * @param {string} messageId - 不同声明的缺失诊断
     */
    function checkTypeMembers(members, messageId) {
      /** @type {Map<string, { name: string; declarations: DeclarationDocumentation[] }>} */
      const overloads = new Map()
      for (const member of members) {
        const anchors = [member.range[0]]
        if (member.type === 'TSPropertySignature') {
          const declaredType = member.typeAnnotation?.typeAnnotation
          const name = documentationMemberName(member) ?? sourceCode.text.slice(...member.key.range)
          const callable = documentationCallableType(declaredType)
          checkDeclarations([{ node: member, anchors, callable }], messageId, name)
        } else if (member.type === 'TSIndexSignature') {
          checkDeclarations([{ node: member, anchors }], messageId, '索引签名')
        } else {
          const staticName =
            member.type === 'TSMethodSignature' ? documentationMemberName(member) : undefined
          const name =
            member.type === 'TSMethodSignature'
              ? (staticName ?? sourceCode.text.slice(...member.key.range))
              : member.type === 'TSCallSignatureDeclaration'
                ? '调用签名'
                : '构造签名'
          const entry = { node: member, anchors, callable: member }
          if (member.type === 'TSMethodSignature' && member.kind !== 'method') {
            checkDeclarations([entry], messageId, name)
            continue
          }
          const key = JSON.stringify([member.type, staticName, name])
          const group = overloads.get(key)
          if (group) group.declarations.push(entry)
          else overloads.set(key, { name, declarations: [entry] })
        }
      }
      for (const { name, declarations } of overloads.values()) {
        checkDeclarations(declarations, messageId, name)
      }
    }

    /**
     * 按作用域检查 Vue 脚本中已命名的函数，跳过已检查的导出或暴露定义
     *
     * @param {Set<DocumentationNode>} checked - 已处理的可调用节点
     */
    function checkVueFunctions(checked) {
      for (const { variables } of sourceCode.scopeManager.scopes) {
        for (const { name, defs } of variables) {
          /** @type {DeclarationDocumentation[]} */
          const declarations = []
          for (const { type, node: syntaxNode } of defs) {
            const node = /** @type {DocumentationNode} */ (/** @type {unknown} */ (syntaxNode))
            if (type === 'Variable' && node.type === 'VariableDeclarator') {
              const binding = documentationVariableBinding(node)
              if (!binding || checked.has(binding.callable)) continue
              checked.add(binding.callable)
              declarations.push(binding)
            } else if (
              type === 'FunctionName' &&
              (node.type === 'FunctionDeclaration' ||
                node.type === 'TSDeclareFunction' ||
                node.type === 'FunctionExpression') &&
              !checked.has(node)
            ) {
              const anchors = documentationDeclarationAnchors(node)
              if (node.parent?.type === 'Property' && node.parent.value === node)
                anchors.push(node.parent.range[0])
              checked.add(node)
              declarations.push({ node: node.id ?? node, anchors, callable: node })
            }
          }
          if (declarations.some(({ callable }) => callable?.type === 'TSDeclareFunction')) {
            checkDeclarations(declarations, 'missingVueFunction', name)
          } else {
            for (const declaration of declarations) {
              checkDeclarations([declaration], 'missingVueFunction', name)
            }
          }
        }
      }
    }

    /**
     * 要求显式暴露的内联函数移到可单独文档化的定义处
     *
     * @param {import('@typescript-eslint/types').TSESTree.Program} program - Vue 脚本节点
     * @param {Set<DocumentationNode>} checked - 已处理的可调用节点
     */
    function checkVueExposedFunctions(program, checked) {
      const range = vueScriptSetupRange(sourceCode)
      if (!range) return
      const [start, end] = range
      const localNames = vueMacroScope(sourceCode)

      for (const statement of program.body) {
        if (
          statement.type !== 'ExpressionStatement' ||
          statement.range[0] < start ||
          statement.range[1] > end
        )
          continue
        const call = vueContractMacro(statement.expression, localNames)
        if (call?.callee.name !== 'defineExpose') continue
        for (const property of literalObjectProperties(call.arguments[0])) {
          if (property.kind !== 'init') continue
          const callable = unwrapDocumentationExpression(property.value)
          if (callable.type !== 'ArrowFunctionExpression' && callable.type !== 'FunctionExpression')
            continue
          const name =
            documentationMemberName(property) ?? sourceCode.text.slice(...property.key.range)
          checked.add(callable)
          context.report({ loc: property.key.loc, messageId: 'inlineVueExposure', data: { name } })
        }
      }
    }

    return {
      ClassDeclaration: checkClass,
      ClassExpression: checkClass,
      /**
       * 检查接口定义和成员
       *
       * @param {unknown} syntaxNode - ESLint 访问到的 TypeScript 接口
       */
      TSInterfaceDeclaration(syntaxNode) {
        const node =
          /** @type {import('@typescript-eslint/types').TSESTree.TSInterfaceDeclaration} */ (
            /** @type {unknown} */ (syntaxNode)
          )
        checkDeclarations(
          [{ node: node.id, anchors: documentationDeclarationAnchors(node) }],
          'missingInterface',
          node.id.name,
        )
        checkTypeMembers(node.body.body, 'missingInterfaceMember')
      },
      /**
       * 检查类型别名和显式声明的对象成员
       *
       * @param {unknown} syntaxNode - ESLint 访问到的 TypeScript 类型别名
       */
      TSTypeAliasDeclaration(syntaxNode) {
        const node =
          /** @type {import('@typescript-eslint/types').TSESTree.TSTypeAliasDeclaration} */ (
            /** @type {unknown} */ (syntaxNode)
          )
        const callable = documentationCallableType(node.typeAnnotation)
        checkDeclarations(
          [{ node: node.id, anchors: documentationDeclarationAnchors(node), callable }],
          'missingTypeAlias',
          node.id.name,
        )
      },
      /**
       * 检查接口或类型别名内部显式写出的对象成员
       *
       * @param {import('estree').Node} syntaxNode - ESLint 访问到的对象类型
       */
      TSTypeLiteral(syntaxNode) {
        const node = /** @type {import('@typescript-eslint/types').TSESTree.TSTypeLiteral} */ (
          /** @type {unknown} */ (syntaxNode)
        )
        for (const parent of sourceCode.getAncestors(syntaxNode).toReversed()) {
          if (String(parent.type) === 'TSInterfaceDeclaration') {
            checkTypeMembers(node.members, 'missingInterfaceMember')
            break
          }
          if (String(parent.type) === 'TSTypeAliasDeclaration') {
            checkTypeMembers(node.members, 'missingTypeMember')
            break
          }
        }
      },
      Program(program) {
        const script = /** @type {import('@typescript-eslint/types').TSESTree.Program} */ (
          /** @type {unknown} */ (program)
        )
        const { body } = script
        const vue = context.filename.endsWith('.vue')
        /** @type {Map<string, DocumentedBinding[]>} */
        const bindings = new Map()
        /** @type {Map<string, string>} */
        const aliases = new Map()
        /** @type {Set<string>} */
        const exports = new Set()
        /** @type {DocumentedBinding[]} */
        const anonymous = []
        /** @type {Map<string, { node: import('@typescript-eslint/types').TSESTree.ObjectExpression; constant: boolean }>} */
        const objects = new Map()
        /** @type {Map<string, { source: DocumentationNode; key: string }>} */
        const extracted = new Map()
        const checkedObjects = new Set()
        const checkedProperties = new Set()

        /**
         * 跟随本模块的静态别名
         *
         * @param {string} name - 模块内的变量名
         */
        function resolveAlias(name) {
          const seen = new Set()
          while (!seen.has(name)) {
            seen.add(name)
            const next = aliases.get(name)
            if (!next) break
            name = next
          }
          return name
        }

        /**
         * 收集公开对象属性上的函数定义或同文件引用
         *
         * @param {import('@typescript-eslint/types').TSESTree.Property} property - 已定位的导出属性
         */
        function addObjectProperty(property) {
          if (checkedProperties.has(property)) return
          checkedProperties.add(property)
          const value = unwrapDocumentationExpression(property.value)
          if (property.kind === 'init') {
            if (value.type === 'Identifier') exports.add(value.name)
            if (value.type === 'ObjectExpression') addObjectMembers(value)
          }
          const name =
            documentationMemberName(property) ?? sourceCode.text.slice(...property.key.range)
          const binding = documentationFunctionExpression(value, [property.range[0]], property.key)
          if (binding) anonymous.push({ ...binding, name })
        }

        /**
         * 收集导出对象中显式声明的可调用成员
         *
         * @param {import('@typescript-eslint/types').TSESTree.ObjectExpression} object - 导出对象
         */
        function addObjectMembers(object) {
          if (checkedObjects.has(object)) return
          checkedObjects.add(object)
          for (const property of literalObjectProperties(object)) addObjectProperty(property)
        }

        for (const statement of body) {
          /** @type {DocumentationNode | null} */
          let declaration = statement
          const exported =
            statement.type === 'ExportDefaultDeclaration' ||
            statement.type === 'ExportNamedDeclaration' ||
            statement.type === 'TSExportAssignment'

          if (statement.type === 'ExportNamedDeclaration') {
            if (statement.source || statement.exportKind === 'type') continue
            for (const specifier of statement.specifiers) {
              if (specifier.exportKind !== 'type') exports.add(specifier.local.name)
            }
            declaration = statement.declaration
          } else if (
            statement.type === 'ExportDefaultDeclaration' ||
            statement.type === 'TSExportAssignment'
          ) {
            const expression =
              statement.type === 'ExportDefaultDeclaration'
                ? statement.declaration
                : statement.expression
            declaration = unwrapDocumentationExpression(expression)
            if (declaration.type === 'Identifier') exports.add(declaration.name)
          }

          if (!declaration) continue
          if (declaration.type === 'VariableDeclaration') {
            for (const { id, init } of declaration.declarations) {
              if (!init) continue
              const value = unwrapDocumentationExpression(init)
              const constant = declaration.kind === 'const'
              if (id.type === 'Identifier') {
                if (constant && value.type === 'Identifier') aliases.set(id.name, value.name)
                else if (!vue && value.type === 'ObjectExpression')
                  objects.set(id.name, { node: value, constant })
                if (exported) exports.add(id.name)
              } else if (!vue && constant && id.type === 'ObjectPattern') {
                for (const property of id.properties) {
                  if (property.type !== 'Property' || property.value.type !== 'Identifier') continue
                  const key = documentationMemberName(property)
                  if (key === undefined) continue
                  extracted.set(property.value.name, { source: value, key })
                  if (exported) exports.add(property.value.name)
                }
              }
            }
          } else if (exported && !vue && declaration.type === 'ObjectExpression') {
            addObjectMembers(declaration)
          }
          for (const binding of documentationBindings(declaration, statement.range[0])) {
            if (!binding.name) {
              if (exported) anonymous.push(binding)
              continue
            }
            const previous = bindings.get(binding.name)
            if (previous) previous.push(binding)
            else bindings.set(binding.name, [binding])
            if (exported) exports.add(binding.name)
          }
        }

        if (!vue) {
          const localNames = new Set(
            sourceCode.scopeManager.scopes
              .filter(({ block }) => block === program)
              .flatMap(({ variables }) =>
                variables.filter(({ defs }) => defs.length > 0).map(({ name }) => name),
              ),
          )
          for (const { name, value, anchor } of commonJsDocumentationExports(body, localNames)) {
            if (value.type === 'Identifier') exports.add(value.name)
            else if (value.type === 'ObjectExpression') addObjectMembers(value)
            else
              for (const binding of documentationBindings(value, anchor)) {
                anonymous.push({ ...binding, name })
              }
          }
        }

        const resolved = new Set()
        for (const name of exports) {
          const target = resolveAlias(name)
          if (bindings.has(target)) resolved.add(target)
          const object = objects.get(target)
          if (object) addObjectMembers(object.node)
          const selection = extracted.get(target)
          if (!selection) continue
          const { source, key } = selection
          const local =
            source.type === 'Identifier' ? objects.get(resolveAlias(source.name)) : undefined
          const member = literalObjectProperties(local?.constant ? local.node : source).find(
            (property) => property.kind === 'init' && documentationMemberName(property) === key,
          )
          if (member) addObjectProperty(member)
        }
        const groups = [
          ...Array.from(resolved, (name) => bindings.get(name) ?? []),
          ...anonymous.map((binding) => [binding]),
        ]
        /** @type {Set<DocumentationNode>} */
        const checked = new Set()
        for (const group of groups) {
          if (!group.length) continue
          if (vue) {
            for (const { callable } of group) checked.add(callable)
          }
          const name = group[0].name ?? 'default'
          if (vue && !group.some(({ callable }) => callable.type === 'TSDeclareFunction')) {
            for (const declaration of group) checkDeclarations([declaration], 'missing', name)
          } else {
            checkDeclarations(group, 'missing', name)
          }
        }
        if (vue) {
          checkVueExposedFunctions(script, checked)
          checkVueFunctions(checked)
        }
      },
    }
  },
}

/** @import { TSESTree as VueAst } from '@typescript-eslint/types' */

/**
 * @typedef {VueAst.TSInterfaceDeclaration | VueAst.TSTypeAliasDeclaration} VueContractType
 * @typedef {{ members: (VueAst.TypeElement | VueAst.TSFunctionType)[]; named: boolean }} VueContractMembers
 * @typedef {{ node: VueAst.Node; checkedByRequired: boolean }} VueContractEntry
 */

/**
 * 判断类型引用是否指向较近的泛型形参
 *
 * @param {VueAst.Node} reference - 当前类型引用
 * @param {string} name - 可能被遮蔽的本地类型名
 */
function vueTypeParameterShadows(reference, name) {
  for (let owner = reference.parent; owner && owner.type !== 'Program'; owner = owner.parent) {
    if (
      'typeParameters' in owner &&
      owner.typeParameters?.params.some((parameter) => parameter.name.name === name)
    )
      return true
  }
  return false
}

/**
 * 读取本文件中的 Vue 契约类型成员
 *
 * @param {VueAst.Node | undefined} type - 宏的类型参数或接口的父类型
 * @param {Map<string, VueContractType[]>} declarations - 当前文件声明的类型
 * @param {Set<string>} [seen] - 已展开的本地类型
 * @param {boolean} [named] - 成员是否归命名接口或类型别名所有
 * @returns {VueContractMembers[]} 可以定位源声明的成员组
 */
function vueContractMembers(type, declarations, seen = new Set(), named = false) {
  if (type?.type === 'TSTypeLiteral') return [{ members: type.members, named }]
  if (type?.type === 'TSFunctionType') return [{ members: [type], named }]
  if (type?.type === 'TSIntersectionType' || type?.type === 'TSUnionType') {
    return type.types.flatMap((branch) => vueContractMembers(branch, declarations, seen, named))
  }
  if (type?.type === 'TSTypeReference' && type.typeName.type === 'Identifier') {
    const name = type.typeName.name
    if (vueTypeParameterShadows(type, name)) return []
    if (!declarations.has(name)) {
      return vueUtilityContractMembers(
        name,
        type.typeArguments?.params ?? [],
        declarations,
        seen,
        named,
      )
    }
  }

  const target =
    type?.type === 'TSTypeReference'
      ? type.typeName
      : type?.type === 'TSInterfaceHeritage'
        ? type.expression
        : undefined
  if (target?.type !== 'Identifier' || seen.has(target.name)) return []
  seen.add(target.name)

  return (declarations.get(target.name) ?? []).flatMap((declaration) =>
    declaration.type === 'TSInterfaceDeclaration'
      ? [
          { members: declaration.body.body, named: true },
          ...declaration.extends.flatMap((parent) =>
            vueContractMembers(parent, declarations, seen, true),
          ),
        ]
      : vueContractMembers(declaration.typeAnnotation, declarations, seen, true),
  )
}

/**
 * 从 Vue 支持的工具类型还原实际选中的键
 *
 * @param {string} utility - 未被本地声明遮蔽的工具类型名
 * @param {VueAst.TypeNode[]} args - 工具类型的参数
 * @param {Map<string, VueContractType[]>} declarations - 本文件中的类型
 * @param {Set<string>} seen - 已展开的本地类型
 * @param {boolean} named - 类型参数是否已由命名类型声明覆盖
 * @returns {VueContractMembers[]} 选中合同成员的源声明
 */
function vueUtilityContractMembers(utility, args, declarations, seen, named) {
  const keepsAll = ['Partial', 'Readonly', 'Required'].includes(utility) && args.length === 1
  const selectsKeys = ['Pick', 'Omit'].includes(utility) && args.length === 2
  if (!keepsAll && !selectsKeys) return []
  const names = selectsKeys ? vueStringTypeNames(args[1], declarations) : []
  if (!names) return []
  const selected = new Set(names)
  return vueContractMembers(args[0], declarations, new Set(seen), named).map(
    ({ members, named }) => ({
      named,
      members: members.filter((member) => {
        if (member.type !== 'TSPropertySignature' && member.type !== 'TSMethodSignature')
          return false
        const name = documentationMemberName(member) ?? vueContractString(member.key)
        if (name === undefined) return false
        return keepsAll || (utility === 'Pick' ? selected.has(name) : !selected.has(name))
      }),
    }),
  )
}

/**
 * 读取已有字面量或本地别名中能静态确定的字符串
 *
 * @param {VueAst.TypeNode | undefined} type - 事件名或工具类型键的类型注解
 * @param {Map<string, VueContractType[]>} declarations - 本文件中的类型
 * @param {boolean} [partial] - 是否允许保留未知联合分支之外的确定名称
 * @param {Set<string>} [seen] - 已追踪的类型别名
 * @returns {string[] | undefined} 可定位的名称，无法确定时为 undefined
 */
function vueStringTypeNames(type, declarations, partial = false, seen = new Set()) {
  if (type?.type === 'TSLiteralType') {
    const name = vueContractString(type.literal)
    return name === undefined ? undefined : [name]
  }
  if (type?.type === 'TSNeverKeyword') return []
  if (type?.type === 'TSUnionType') {
    const branches = type.types.map((item) => vueStringTypeNames(item, declarations, partial, seen))
    if (!partial && branches.some((branch) => !branch)) return undefined
    return branches.flatMap((branch) => branch ?? [])
  }
  if (type?.type !== 'TSTypeReference' || type.typeName.type !== 'Identifier' || type.typeArguments)
    return undefined
  const name = type.typeName.name
  const targets = declarations.get(name)
  const target = targets?.[0]
  if (
    seen.has(name) ||
    vueTypeParameterShadows(type, name) ||
    targets?.length !== 1 ||
    target?.type !== 'TSTypeAliasDeclaration'
  )
    return undefined
  return vueStringTypeNames(target.typeAnnotation, declarations, partial, new Set(seen).add(name))
}

/**
 * 返回未被当前模块绑定遮蔽的 Vue 编译器宏调用
 *
 * @param {VueAst.Node | null | undefined} expression - 宏调用的候选表达式
 * @param {Set<string>} localNames - 当前模块声明的绑定
 * @returns {(VueAst.CallExpression & { callee: VueAst.Identifier }) | undefined} 可识别的直接调用
 */
function vueContractMacro(expression, localNames) {
  if (!expression) return
  const call = unwrapDocumentationExpression(expression)
  if (
    call.type === 'CallExpression' &&
    call.callee.type === 'Identifier' &&
    !localNames.has(call.callee.name)
  ) {
    return /** @type {VueAst.CallExpression & { callee: VueAst.Identifier }} */ (call)
  }
}

/**
 * 判断本地绑定是否覆盖同名的 Vue 编译器宏
 *
 * @param {import('eslint').Scope.Variable['defs'][number]} definition - ESLint 的作用域定义
 */
function shadowsVueDocumentationMacro(definition) {
  if (String(definition.type) === 'Type') return false
  if (definition.type !== 'ImportBinding') return true
  const { node, parent } = definition
  if (
    ('importKind' in parent && parent.importKind === 'type') ||
    ('importKind' in node && node.importKind === 'type')
  )
    return false
  return !(
    parent.source.value === 'vue' &&
    node.type === 'ImportSpecifier' &&
    node.imported.type === 'Identifier' &&
    node.imported.name === node.local.name
  )
}

/**
 * 获取允许 Vue 编译器宏出现的脚本区间
 *
 * @param {import('eslint').SourceCode} sourceCode - Vue 单文件组件解析结果
 * @returns {[number, number] | undefined} 脚本内容的起止偏移
 */
function vueScriptSetupRange(sourceCode) {
  const fragment =
    /** @type {import('vue-eslint-parser').AST.VDocumentFragment | null | undefined} */ (
      sourceCode.parserServices.getDocumentFragment?.()
    )
  const setup = fragment?.children.find(
    (child) =>
      child.type === 'VElement' &&
      child.name === 'script' &&
      child.startTag.attributes.some((attr) => !attr.directive && attr.key.name === 'setup'),
  )
  if (setup?.type === 'VElement' && setup.endTag)
    return [setup.startTag.range[1], setup.endTag.range[0]]
}

/**
 * 获取可遮蔽 Vue 编译器宏的模块绑定
 *
 * @param {import('eslint').SourceCode} sourceCode - 当前 Vue 脚本作用域
 */
function vueMacroScope(sourceCode) {
  return new Set(
    sourceCode.scopeManager.scopes
      .filter(({ block }) => block === sourceCode.ast)
      .flatMap(({ variables }) =>
        variables
          .filter(({ defs }) => defs.some(shadowsVueDocumentationMacro))
          .map(({ name }) => name),
      ),
  )
}

/**
 * 读取运行时声明中的静态字符串
 *
 * @param {VueAst.Node | null | undefined} node - Vue 声明中的字符串表达式
 * @returns {string | undefined} 可以直接确定的名称
 */
function vueContractString(node) {
  if (node?.type === 'Literal' && typeof node.value === 'string') return node.value
  if (node?.type === 'TemplateLiteral' && node.expressions.length === 0) {
    return node.quasis[0]?.value.cooked ?? undefined
  }
}

/**
 * 读取直接字面量或作用域中一级 const 字面量的组件键
 *
 * @param {VueAst.Node} node - 运行时名称表达式
 * @param {import('eslint').SourceCode} sourceCode - ESLint 解析后的脚本作用域
 * @returns {string | undefined} 可以直接从声明确定的名称
 */
function vueRuntimeName(node, sourceCode) {
  if (node.type !== 'Identifier') return vueContractString(node)
  const occurrence = /** @type {import('estree').Identifier} */ (/** @type {unknown} */ (node))
  /** @type {import('eslint').Scope.Scope | null} */
  let scope = sourceCode.getScope(occurrence)
  for (; scope; scope = scope.upper) {
    const variable = scope.set.get(node.name)
    if (!variable) continue
    const [definition] = variable.defs
    if (
      variable.defs.length !== 1 ||
      definition.type !== 'Variable' ||
      definition.parent?.kind !== 'const' ||
      definition.node.id.type !== 'Identifier' ||
      !definition.node.init
    )
      return undefined

    const value = /** @type {VueAst.Node} */ (/** @type {unknown} */ (definition.node.init))
    return vueContractString(unwrapDocumentationExpression(value))
  }
}

/**
 * 遍历运行时对象明确写出的属性，包括内联对象展开
 *
 * @param {VueAst.Node | undefined} shape - 运行时对象声明或调用参数
 * @returns {Generator<VueAst.Property>} 可以直接定位的对象属性
 */
function* declaredObjectProperties(shape) {
  if (!shape) return
  const object = unwrapDocumentationExpression(shape)
  if (object.type !== 'ObjectExpression') return
  for (const member of object.properties) {
    if (member.type === 'SpreadElement') yield* declaredObjectProperties(member.argument)
    else if (
      member.computed ||
      member.shorthand ||
      member.method ||
      member.kind !== 'init' ||
      documentationMemberName(member) !== '__proto__'
    ) {
      yield member
    }
  }
}

/**
 * 合并同名属性，保留最终可用的定义及配对访问器
 *
 * @param {DocumentationNode | undefined} shape - 可能为对象字面量的表达式
 * @param {(node: VueAst.Node) => string | undefined} [computedName] - 额外解释计算键的语法
 * @returns {VueAst.Property[]} 按源码顺序排列的定义
 */
function literalObjectProperties(shape, computedName) {
  /** @type {Map<string | VueAst.Property, VueAst.Property[]>} */
  const members = new Map()
  for (const member of declaredObjectProperties(shape)) {
    const name =
      documentationMemberName(member) ??
      vueContractString(member.key) ??
      (member.computed ? computedName?.(member.key) : undefined) ??
      member
    const accessors =
      member.kind === 'init'
        ? []
        : (members.get(name) ?? []).filter(({ kind }) => kind !== 'init' && kind !== member.kind)
    members.set(name, [...accessors, member])
  }
  return [...members.values()].flat().toSorted((first, second) => first.range[0] - second.range[0])
}

/**
 * 读取运行时字面量中的组件契约名称及其声明位置
 *
 * @param {VueAst.Node} declaration - 运行时宏声明
 * @param {import('eslint').SourceCode} sourceCode - ESLint 解析后的脚本作用域
 * @returns {Generator<[string, VueAst.Node]>} 静态名称和对应节点
 */
function* vueRuntimeEntries(declaration, sourceCode) {
  const value = unwrapDocumentationExpression(declaration)
  if (value.type === 'ArrayExpression') {
    for (const item of value.elements) {
      if (item?.type === 'SpreadElement') {
        const nested = unwrapDocumentationExpression(item.argument)
        if (nested.type === 'ArrayExpression') yield* vueRuntimeEntries(nested, sourceCode)
      } else if (item) {
        const name = vueRuntimeName(item, sourceCode)
        if (name !== undefined) yield [name, item]
      }
    }
  } else if (value.type === 'ObjectExpression') {
    for (const member of literalObjectProperties(value, (key) => vueRuntimeName(key, sourceCode))) {
      const name =
        documentationMemberName(member) ??
        (member.computed ? vueRuntimeName(member.key, sourceCode) : undefined)
      if (name !== undefined) yield [name, member]
    }
  }
}

/**
 * 检查 Vue 组件显式声明的 Prop 和事件是否有用途说明
 *
 * @type {import('eslint').Rule.RuleModule}
 */
const jsdocVueContractRule = {
  meta: {
    type: 'suggestion',
    schema: [],
    messages: {
      missingProp: 'Vue Prop {{name}} 缺少说明用途的 JSDoc',
      missingEmit: 'Vue 事件 {{name}} 缺少说明用途的 JSDoc',
    },
  },
  create(context) {
    const { sourceCode } = context
    const checked = new Set()

    /**
     * 检查命名契约在声明处的用途说明
     *
     * @param {VueAst.Node} node - Prop、事件或模型的声明位置
     * @param {string} messageId - 相应的缺失诊断
     * @param {string} name - 声明的契约名称
     */
    function reportMissing(node, messageId, name) {
      const key = JSON.stringify([node.range[0], messageId, name])
      if (checked.has(key)) return
      checked.add(key)
      if (!hasDocumentationSummary(sourceCode, node.range[0])) {
        context.report({ loc: node.loc, messageId, data: { name } })
      }
    }

    /**
     * 检查用字面量数组或对象声明的 Vue 契约
     *
     * @param {VueAst.Node | undefined} declaration - 运行时宏声明
     * @param {string} messageId - 相应的缺失诊断
     */
    function checkRuntime(declaration, messageId) {
      if (!declaration) return
      /** @type {Map<string, VueAst.Node>} */
      const members = new Map()
      for (const [name, node] of vueRuntimeEntries(declaration, sourceCode)) {
        if (!members.has(name) || hasDocumentationSummary(sourceCode, node.range[0])) {
          members.set(name, node)
        }
      }
      for (const [name, node] of members) reportMissing(node, messageId, name)
    }

    /**
     * 检查本地可解析的 Props 和 Emits 类型成员
     *
     * @param {VueAst.CallExpression} call - defineProps 或 defineEmits 调用
     * @param {Map<string, VueContractType[]>} localTypes - 当前文件的类型
     * @param {string} messageId - 相应的缺失诊断
     */
    function checkTyped(call, localTypes, messageId) {
      /** @type {Map<string, VueContractEntry[]>} */
      const callSignatures = new Map()
      const types = vueContractMembers(call.typeArguments?.params[0], localTypes)
      for (const { members, named } of types) {
        const signatures = members.filter((member) => member.type === 'TSCallSignatureDeclaration')
        const requiredAnchor =
          named && !signatures.some(({ range }) => findDocumentationSummary(sourceCode, range[0]))
            ? signatures[0]
            : undefined
        for (const member of members) {
          if (member.type === 'TSPropertySignature' || member.type === 'TSMethodSignature') {
            const name = documentationMemberName(member) ?? vueContractString(member.key)
            if (!named && name !== undefined) reportMissing(member, messageId, name)
          } else if (
            messageId === 'missingEmit' &&
            (member.type === 'TSCallSignatureDeclaration' || member.type === 'TSFunctionType')
          ) {
            const checkedByRequired =
              member === requiredAnchor ||
              (named &&
                member.type === 'TSFunctionType' &&
                member.parent.type === 'TSTypeAliasDeclaration')
            const param = member.params[0]
            const annotation =
              param?.type === 'Identifier' ? param.typeAnnotation?.typeAnnotation : undefined
            for (const name of new Set(vueStringTypeNames(annotation, localTypes, true) ?? [])) {
              const entry = { node: member, checkedByRequired }
              const entries = callSignatures.get(name)
              if (entries) entries.push(entry)
              else callSignatures.set(name, [entry])
            }
          }
        }
      }
      for (const [name, entries] of callSignatures) {
        if (
          !entries.some(
            ({ checkedByRequired, node }) =>
              checkedByRequired || hasDocumentationSummary(sourceCode, node.range[0]),
          )
        ) {
          reportMissing(entries[0].node, 'missingEmit', name)
        }
      }
    }

    return {
      Program(program) {
        const range = vueScriptSetupRange(sourceCode)
        if (!range) return

        const { body } = /** @type {VueAst.Program} */ (/** @type {unknown} */ (program))
        /** @type {Map<string, VueContractType[]>} */
        const localTypes = new Map()
        for (const statement of body) {
          if (statement.type === 'ImportDeclaration') {
            for (const { local } of statement.specifiers) localTypes.set(local.name, [])
            continue
          }
          const node =
            statement.type === 'ExportNamedDeclaration' ||
            statement.type === 'ExportDefaultDeclaration'
              ? statement.declaration
              : statement
          if (node?.type === 'ClassDeclaration' && node.id) localTypes.set(node.id.name, [])
          if (node?.type !== 'TSInterfaceDeclaration' && node?.type !== 'TSTypeAliasDeclaration')
            continue
          const entries = localTypes.get(node.id.name)
          if (entries) entries.push(node)
          else localTypes.set(node.id.name, [node])
        }

        const localNames = vueMacroScope(sourceCode)

        /**
         * 按编译器宏的形式检查当前声明
         *
         * @param {VueAst.Node | null} expression - 顶层初始化表达式
         * @param {VueAst.Node} declaration - 宏调用所在声明
         */
        function checkMacro(expression, declaration) {
          let call = vueContractMacro(expression, localNames)
          if (call?.callee.name === 'withDefaults') {
            call = vueContractMacro(call.arguments[0], localNames)
            if (call?.callee.name !== 'defineProps') return
          }
          if (!call) return

          if (call.callee.name === 'defineProps' || call.callee.name === 'defineEmits') {
            const messageId = call.callee.name === 'defineProps' ? 'missingProp' : 'missingEmit'
            checkRuntime(call.arguments[0], messageId)
            checkTyped(call, localTypes, messageId)
          } else if (call.callee.name === 'defineModel') {
            const argument = call.arguments[0]
            if (argument?.type === 'SpreadElement') return
            const name = vueContractString(argument && unwrapDocumentationExpression(argument))
            reportMissing(declaration, 'missingProp', name ?? 'modelValue')
          }
        }

        const [start, end] = range
        for (const statement of body) {
          if (statement.range[0] < start || statement.range[1] > end) continue
          if (statement.type === 'ExpressionStatement') {
            checkMacro(statement.expression, statement)
          } else if (statement.type === 'VariableDeclaration') {
            for (const [index, declaration] of statement.declarations.entries()) {
              checkMacro(declaration.init, index === 0 ? statement : declaration)
            }
          }
        }
      },
    }
  },
}

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
      files: scriptFiles,
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
      name: 'lonelight/jsdoc-layout',
      files: scriptFiles,
      plugins: {
        'lonelight-docs': {
          rules: {
            layout: jsdocLayoutRule,
            required: jsdocRequiredRule,
            'vue-contract': jsdocVueContractRule,
          },
        },
      },
      rules: { 'lonelight-docs/layout': 'error' },
    },
    {
      name: 'lonelight/jsdoc-required',
      files: ['src/**/*.{js,mjs,cjs,jsx,ts,mts,cts,tsx,vue}'],
      ignores: documentationSourceIgnores,
      rules: { 'lonelight-docs/required': 'error' },
    },
    {
      name: 'lonelight/vue-contract',
      files: ['src/**/*.vue'],
      ignores: documentationSourceIgnores,
      rules: { 'lonelight-docs/vue-contract': 'error' },
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

export default createVueTsConfig({
  projectRules: [apiRequestBoundary, piniaStoreNaming, promiseHandling],
})
