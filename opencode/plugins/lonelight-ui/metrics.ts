import type { Plugin } from "@opencode/plugin/tui"

export type ContextMetrics = {
  used: number
  limit?: number
  percent?: number
  cacheReusePercent: number
}

export type McpTone = "base" | "info" | "warning" | "error"

export type McpMetrics = {
  connected: number
  total: number
  tone: McpTone
}

export function getContextMetrics(
  context: Plugin.Context,
  sessionID?: string,
): ContextMetrics | undefined {
  if (!sessionID) return

  const session = context.data.session.get(sessionID)

  if (!session) return

  const messages = context.data.session.message.list(sessionID)

  const boundary = session.revert?.messageID

  const boundaryIndex = boundary
    ? messages.findIndex((message) => message.id === boundary)
    : -1

  if (boundary && boundaryIndex === -1) {
    return
  }

  const end = boundaryIndex === -1 ? messages.length : boundaryIndex

  const compactionIndex = messages.findLastIndex(
    (message, index) =>
      message.type === "compaction" &&
      message.status === "completed" &&
      index < end,
  )

  const last = messages.findLast(
    (message, index) =>
      message.type === "assistant" &&
      message.tokens !== undefined &&
      index > compactionIndex &&
      index < end,
  )

  if (!last || last.type !== "assistant" || !last.tokens) {
    return
  }

  const used =
    last.tokens.input +
    last.tokens.output +
    last.tokens.reasoning +
    last.tokens.cache.read +
    last.tokens.cache.write

  if (used <= 0) return

  const models = context.data.location.model.list(session.location)

  const model = models?.find(
    (item) =>
      item.providerID === last.model.providerID && item.id === last.model.id,
  )

  const rawLimit = model?.limit.context

  const limit = rawLimit && rawLimit > 0 ? rawLimit : undefined

  const percent =
    limit !== undefined ? Math.round((used / limit) * 100) : undefined

  const cacheReusePercent = Math.max(
    0,
    Math.min(100, Math.round((last.tokens.cache.read / used) * 100)),
  )

  return {
    used,
    limit,
    percent,
    cacheReusePercent,
  }
}

export function getMcpMetrics(
  context: Plugin.Context,
  sessionID?: string,
): McpMetrics | undefined {
  if (!sessionID) return

  const session = context.data.session.get(sessionID)

  if (!session) return

  const servers = context.data.location.mcp.server.list(session.location)

  if (!servers) return

  const active = servers.filter((server) => server.status.status !== "disabled")

  if (active.length === 0) return

  const statuses = active.map((server) => server.status.status)

  const connected = statuses.filter((status) => status === "connected").length

  let tone: McpTone = "base"

  if (statuses.includes("failed")) {
    tone = "error"
  } else if (statuses.includes("needs_auth")) {
    tone = "warning"
  } else if (statuses.includes("pending")) {
    tone = "info"
  }

  return {
    connected,
    total: active.length,
    tone,
  }
}
