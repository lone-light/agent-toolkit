/**
 * 查询服务状态
 *
 * @returns HTTP 请求是否成功
 */
export async function fetchStatus(): Promise<boolean> {
  const response = await fetch('/api/status')
  return response.ok
}
