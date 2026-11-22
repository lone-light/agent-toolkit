/**
 * 服务状态的查询选项
 */
export interface StatusOptions {
  available: boolean
}

/**
 * 服务状态的展示信息
 */
export type StatusSummary = {
  label: string
}

/**
 * 记录服务的查询次数
 */
export class StatusHistory {
  private count = 0

  record(): void {
    this.count += 1
  }
}

export function parseAvailableFlag(flag: string): boolean {
  return flag === 'available'
}

/**
 * 生成服务状态标签
 */
export function createStatusLabel(options: StatusOptions): StatusSummary {
  return { label: options.available ? '可用' : '不可用' }
}

/** 通知界面重新查询服务状态 */
export function requestStatusRefresh(): void {
  window.dispatchEvent(new Event('status-refresh'))
}
