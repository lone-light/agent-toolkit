import type { AxiosInstance } from 'axios'

export type Client = AxiosInstance
export const loadClient = () => import('axios')
export const loadStatus = () => globalThis.fetch('/api/status')
