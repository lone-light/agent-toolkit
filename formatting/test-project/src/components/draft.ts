/**
 * 去掉草稿首尾的空白
 *
 * @param text - 输入框中的原始文字
 * @returns 整理后的文字，全为空白时返回空字符串
 */
export function normalizeDraft(text: string): string {
  return text.trim()
}

/**
 * 记录草稿的有效提交次数
 */
export class SubmissionCounter {
  /**
   * 自计数器创建以来的有效提交次数
   */
  private total = 0

  /**
   * 记录一次有效提交
   *
   * @returns 本次提交计入后的累计次数
   */
  record(): number {
    this.total += 1
    return this.total
  }
}
