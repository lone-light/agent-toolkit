import type { fetchStatus } from '@/api/status'
import type { Stats } from 'node:fs'

export type StatusReader = typeof fetchStatus
export type FileDetails = Stats
