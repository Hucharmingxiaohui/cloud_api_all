import type { RuntimeConfig } from './runtime-config'

declare global {
  interface Window {
    CURRENT_CONFIG?: RuntimeConfig
  }
}

export {}
