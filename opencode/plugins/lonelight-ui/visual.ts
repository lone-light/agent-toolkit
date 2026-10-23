export const CONTEXT_WARNING_PERCENT = 85

export const ITEM_GAP = "   "
export const GROUP_GAP = "     "

export const BREATH_FRAME_MS = 80
export const BREATH_PERIOD_MS = 2400
export const BREATH_MIN_ALPHA = 0.74

export const GLYPH = {
  brand: "✦",
  status: "♡",
  cache: "✧",
} as const

export function formatTokens(value: number) {
  if (value >= 1_000_000) {
    return `${(value / 1_000_000).toFixed(1).replace(/\.0$/, "")}m`
  }

  if (value >= 1_000) {
    return `${(value / 1_000).toFixed(1).replace(/\.0$/, "")}k`
  }

  return String(value)
}

export function contextGlyph(percent: number) {
  if (percent < 13) return "○"
  if (percent < 38) return "◔"
  if (percent < 63) return "◑"
  if (percent < 88) return "◕"

  return "●"
}

export function withAlpha<
  Color extends {
    a: number
  },
>(color: Color, alpha: number) {
  const faded = Object.assign(
    Object.create(Object.getPrototypeOf(color)),
    color,
  ) as Color

  const sourceBuffer = (
    color as Color & {
      buffer?: {
        slice(): unknown
      }
    }
  ).buffer

  if (sourceBuffer) {
    const fadedWithBuffer = faded as Color & {
      buffer: unknown
    }
    fadedWithBuffer.buffer = sourceBuffer.slice()
  }

  faded.a *= Math.max(0, Math.min(1, alpha))
  return faded
}

export function breatheAlpha(elapsed: number) {
  const phase = (elapsed % BREATH_PERIOD_MS) / BREATH_PERIOD_MS

  const wave = (1 + Math.cos(phase * Math.PI * 2)) / 2

  return BREATH_MIN_ALPHA + (1 - BREATH_MIN_ALPHA) * wave
}
