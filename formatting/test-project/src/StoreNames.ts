import { defineStore as makeStore } from 'pinia'
import * as pinia from 'pinia'

export const useStatusStore = makeStore('status', { state: () => ({ available: false }) })
export const queue = makeStore('queue', { state: () => ({ size: 0 }) })
export const account = pinia.defineStore('account', { state: () => ({ name: '' }) })

export function initializeLocalStore(makeStore: () => number) {
  const localStore = makeStore()
  return localStore
}
