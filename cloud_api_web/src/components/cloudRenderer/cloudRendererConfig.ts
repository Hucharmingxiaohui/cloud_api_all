import { CURRENT_CONFIG } from '/@/api/http/config'
import type { CloudRendererConfig } from '/@/types/runtime-config'

export const CLOUD_RENDERER_CONFIG_STORAGE_KEY = 'cloud-renderer-config'

export const DEFAULT_CLOUD_RENDERER_CONFIG: CloudRendererConfig = {
  baseURL: 'http://127.0.0.1:3000',
  renderer: 'outdoor',
  pointCloudFile: '3dgs/7.1.1.ply',
  rendererParams: {
    alignSplatFile: '3dgs/7.1.1.ply',
    alignReferenceTileset: '3dgs/dfelanqiuchang/tileset.json'
  },
  iceServers: []
}

const DEPLOYMENT_CLOUD_RENDERER_CONFIG = JSON.parse(JSON.stringify(CURRENT_CONFIG.cloudRenderer || {})) as Partial<CloudRendererConfig>

function readLocalConfig (): Partial<CloudRendererConfig> {
  try {
    const value = localStorage.getItem(CLOUD_RENDERER_CONFIG_STORAGE_KEY)
    return value ? JSON.parse(value) : {}
  } catch (error) {
    console.warn('读取云渲染本地配置失败:', error)
    return {}
  }
}

export function getCloudRendererConfig (): CloudRendererConfig {
  const deployment = CURRENT_CONFIG.cloudRenderer || {}
  const local = readLocalConfig()
  return {
    ...DEFAULT_CLOUD_RENDERER_CONFIG,
    ...deployment,
    ...local,
    rendererParams: {
      ...DEFAULT_CLOUD_RENDERER_CONFIG.rendererParams,
      ...(deployment.rendererParams || {}),
      ...(local.rendererParams || {})
    },
    iceServers: local.iceServers || deployment.iceServers || DEFAULT_CLOUD_RENDERER_CONFIG.iceServers
  }
}

export function saveCloudRendererConfig (config: CloudRendererConfig) {
  localStorage.setItem(CLOUD_RENDERER_CONFIG_STORAGE_KEY, JSON.stringify(config))
  applyCloudRendererConfig(config)
}

export function applyCloudRendererConfig (config: CloudRendererConfig) {
  CURRENT_CONFIG.cloudRenderer = {
    ...CURRENT_CONFIG.cloudRenderer,
    ...config,
    rendererParams: { ...config.rendererParams },
    iceServers: [...config.iceServers]
  }
}

export function resetCloudRendererConfig () {
  localStorage.removeItem(CLOUD_RENDERER_CONFIG_STORAGE_KEY)
  const deployment = DEPLOYMENT_CLOUD_RENDERER_CONFIG
  const config: CloudRendererConfig = {
    ...DEFAULT_CLOUD_RENDERER_CONFIG,
    ...deployment,
    rendererParams: {
      ...DEFAULT_CLOUD_RENDERER_CONFIG.rendererParams,
      ...(deployment.rendererParams || {})
    },
    iceServers: deployment.iceServers || DEFAULT_CLOUD_RENDERER_CONFIG.iceServers
  }
  applyCloudRendererConfig(config)
  return config
}

export async function fetchRendererResources (baseURL: string, path: string): Promise<string[]> {
  const response = await fetch(new URL(path, baseURL).toString())
  if (!response.ok) throw new Error(`读取云渲染资源失败：${response.status}`)
  const data = await response.json()
  const payload = data?.data ?? data
  const values = Array.isArray(payload)
    ? payload
    : payload?.items || payload?.files || payload?.pointClouds || payload?.referenceTilesets || payload?.tilesets || []
  if (!Array.isArray(values)) throw new Error('云渲染资源响应格式不正确')
  return values.map((item: any) => typeof item === 'string' ? item : item.path || item.file || item.url || item.name).filter(Boolean)
}
