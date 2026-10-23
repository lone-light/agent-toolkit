import { Plugin } from "@opencode/plugin/tui"
import {
  Match,
  Show,
  Switch,
  createEffect,
  createMemo,
  createSignal,
  onCleanup,
} from "solid-js"
import {
  type ContextMetrics,
  type McpMetrics,
  getContextMetrics,
  getMcpMetrics,
} from "./metrics"
import {
  BREATH_FRAME_MS,
  CONTEXT_WARNING_PERCENT,
  GLYPH,
  GROUP_GAP,
  ITEM_GAP,
  breatheAlpha,
  contextGlyph,
  formatTokens,
  withAlpha,
} from "./visual"

export default Plugin.define({
  id: "lonelight.ui",

  setup(context) {
    const cleanups = [
      context.ui.slot({
        prepend: "session.composer.top",

        render: (props) => {
          const running = createMemo(
            () => context.data.session.status(props.sessionID) === "running",
          )

          const usage = createMemo(() =>
            getContextMetrics(context, props.sessionID),
          )

          const percent = createMemo(() => usage()?.percent)

          const contextColor = createMemo(() => {
            const value = percent()

            if (value !== undefined && value >= CONTEXT_WARNING_PERCENT) {
              return context.theme.text.feedback.warning.base
            }

            return context.theme.hue.purple[500]
          })

          const [breathElapsed, setBreathElapsed] = createSignal(0)

          let breathTimer: ReturnType<typeof setInterval> | undefined

          createEffect(() => {
            if (!running()) {
              if (breathTimer) {
                clearInterval(breathTimer)
                breathTimer = undefined
              }

              setBreathElapsed(0)
              return
            }

            if (breathTimer) return

            breathTimer = setInterval(() => {
              setBreathElapsed((value) => value + BREATH_FRAME_MS)
            }, BREATH_FRAME_MS)
          })

          onCleanup(() => {
            if (breathTimer) {
              clearInterval(breathTimer)
            }
          })

          const busyColor = createMemo(() =>
            withAlpha(
              context.theme.hue.purple[500],
              breatheAlpha(breathElapsed()),
            ),
          )

          return (
            <box height={1} flexShrink={0}>
              <text wrapMode="none" flexShrink={0}>
                <span
                  style={{
                    fg: context.theme.hue.purple[500],
                  }}
                >
                  {GLYPH.brand}
                </span>

                <span
                  style={{
                    fg: context.theme.text.base,
                  }}
                >
                  {" lonelight"}
                </span>

                <Show
                  when={running()}
                  fallback={
                    <span
                      style={{
                        fg: context.theme.text.muted,
                      }}
                    >
                      {`${ITEM_GAP}${GLYPH.status} 待机`}
                    </span>
                  }
                >
                  <span
                    style={{
                      fg: busyColor(),
                    }}
                  >
                    {`${ITEM_GAP}${GLYPH.status} 处理中`}
                  </span>
                </Show>

                <Show when={percent() !== undefined}>
                  <span
                    style={{
                      fg: contextColor(),
                    }}
                  >
                    {ITEM_GAP}
                    {contextGlyph(percent()!)} {percent()}%
                  </span>
                </Show>
              </text>
            </box>
          )
        },
      }),

      context.ui.slot({
        append: "prompt.footer",

        render: (props) => {
          const usage = createMemo(() =>
            getContextMetrics(context, props.sessionID),
          )

          const mcp = createMemo(() => getMcpMetrics(context, props.sessionID))

          const mcpColor = createMemo(() => {
            switch (mcp()?.tone) {
              case "error":
                return context.theme.text.feedback.error.base

              case "warning":
                return context.theme.text.feedback.warning.base

              case "info":
                return context.theme.text.feedback.info.base

              default:
                return context.theme.text.base
            }
          })

          return (
            <Switch>
              <Match when={props.mode === "shell"}>
                <text fg={context.theme.text.muted} wrapMode="none">
                  esc · 退出 shell
                </text>
              </Match>

              <Match when={props.mode === "normal"}>
                <text wrapMode="none" flexShrink={0}>
                  <Show when={usage()}>
                    {(metrics: () => ContextMetrics) => (
                      <>
                        <Show when={props.showDetails}>
                          <span
                            style={{
                              fg: context.theme.text.muted,
                            }}
                          >
                            {formatTokens(metrics().used)}

                            <Show when={metrics().limit !== undefined}>
                              {`/${formatTokens(metrics().limit!)}`}
                            </Show>
                          </span>
                        </Show>

                        <span
                          style={{
                            fg: context.theme.hue.purple[500],
                          }}
                        >
                          {`${ITEM_GAP}${GLYPH.cache}`}
                        </span>

                        <span
                          style={{
                            fg: context.theme.text.base,
                          }}
                        >
                          {" "}
                          {metrics().cacheReusePercent}%
                        </span>
                      </>
                    )}
                  </Show>

                  <Show when={usage() !== undefined && mcp() !== undefined}>
                    <span>{GROUP_GAP}</span>
                  </Show>

                  <Show when={mcp()}>
                    {(metrics: () => McpMetrics) => (
                      <>
                        <span
                          style={{
                            fg: context.theme.text.muted,
                          }}
                        >
                          MCP{" "}
                        </span>

                        <span
                          style={{
                            fg: mcpColor(),
                          }}
                        >
                          {metrics().connected}/{metrics().total}
                        </span>
                      </>
                    )}
                  </Show>
                </text>
              </Match>
            </Switch>
          )
        },
      }),

      context.ui.slot({
        append: "home.footer.status",

        render: () => (
          <text wrapMode="none" flexShrink={0}>
            <span
              style={{
                fg: context.theme.hue.purple[500],
              }}
            >
              {GLYPH.brand}
            </span>

            <span
              style={{
                fg: context.theme.text.base,
              }}
            >
              {" lonelight"}
            </span>

            <span
              style={{
                fg: context.theme.text.muted,
              }}
            >
              {`${ITEM_GAP}${GLYPH.status} 待机`}
            </span>
          </text>
        ),
      }),
    ]

    return () => {
      for (const cleanup of cleanups) {
        cleanup()
      }
    }
  },
})
