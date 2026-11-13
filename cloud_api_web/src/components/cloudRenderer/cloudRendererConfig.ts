import { CURRENT_CONFIG } from '/@/api/http/config'
import type { CloudRendererConfig } from '/@/types/runtime-config'
import { ref } from 'vue'

export const CLOUD_RENDERER_CONFIG_STORAGE_KEY = 'cloud-renderer-config'

export const DEFAULT_CLOUD_RENDERER_CONFIG: CloudRendererConfig = {
  enabled: true,
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

export const cloudRendererConfigError = ref('')
export const cloudRendererConfigReady = ref(false)
const platformConfig = ref<CloudRendererConfig | null>(null)
let revision = 0
let loadedToken = ''
let pendingLoad: Promise<void> | null = null

function deploymentConfig (): CloudRendererConfig {
  const deployment = DEPLOYMENT_CLOUD_RENDERER_CONFIG
  return {
    ...DEFAULT_CLOUD_RENDERER_CONFIG,
    ...deployment,
    rendererParams: {
      ...DEFAULT_CLOUD_RENDERER_CONFIG.rendererParams,
      ...(deployment.rendererParams || {})
    },
    iceServers: deployment.iceServers || DEFAULT_CLOUD_RENDERER_CONFIG.iceServers
  }
}

export function getCloudRendererConfig (): CloudRendererConfig {
  return JSON.parse(JSON.stringify(platformConfig.value || { ...deploymentConfig(), enabled: false }))
}

export function isCloudRendererEnabled () {
  return cloudRendererConfigReady.value && platformConfig.value?.enabled === true
}

async function requestConfig (method: 'GET' | 'PUT', token: string, body?: unknown) {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 10000)
  try {
    const base = String(CURRENT_CONFIG.baseURL || window.location.origin).replace(/\/+$/, '')
    const response = await fetch(`${base}/api/platform/cloud-renderer-config`, {
      method,
      headers: { 'Content-Type': 'application/json', 'x-auth-token': token },
      cache: 'no-store',
      signal: controller.signal,
      ...(body ? { body: JSON.stringify(body) } : {})
    })
    if (!response.ok) {
      const error = new Error(response.status === 409 ? '云渲染配置已被其他页面修改，请重新读取后再保存' : `云渲染配置${method === 'GET' ? '读取' : '保存'}失败（HTTP ${response.status}）`)
      Object.assign(error, { status: response.status })
      throw error
    }
    const envelope = await response.json()
    const result = envelope.data
    if (envelope.code !== 0 || typeof result?.configured !== 'boolean' || !Number.isSafeInteger(result.revision) || (result.configured && typeof result.config?.enabled !== 'boolean')) throw new Error('云渲染配置接口响应无效')
    return result
  } finally {
    window.clearTimeout(timer)
  }
}

function acceptConfig (result: any, token: string) {
  if (localStorage.getItem('x-auth-token') !== token) throw new Error('登录状态已变化，请重新读取配置')
  if (!result.configured) throw new Error('平台云渲染配置尚未初始化')
  platformConfig.value = result.config
  revision = result.revision
  loadedToken = token
  cloudRendererConfigReady.value = true
  cloudRendererConfigError.value = ''
}

export async function loadCloudRendererConfig () {
  if (pendingLoad) return pendingLoad
  const token = localStorage.getItem('x-auth-token')
  if (!token) {
    cloudRendererConfigReady.value = false
    platformConfig.value = null
    loadedToken = ''
    return
  }
  pendingLoad = (async () => {
    try {
      let result = await requestConfig('GET', token)
      if (!result.configured) {
        // The first authenticated visit seeds the deployment defaults, never an old browser override.
        try { result = await requestConfig('PUT', token, { revision: 0, config: deploymentConfig() }) } catch (error) {
          if ((error as any)?.status !== 409) throw error
          result = await requestConfig('GET', token)
        }
      }
      acceptConfig(result, token)
    } catch (error) {
      cloudRendererConfigReady.value = false
      cloudRendererConfigError.value = error instanceof Error ? error.message : '云渲染配置读取失败'
      throw error
    } finally { pendingLoad = null }
  })()
  return pendingLoad
}

export function getCloudRendererConfigRevision () { return revision }

export async function saveCloudRendererConfig (config: CloudRendererConfig, expectedRevision = revision) {
  const token = localStorage.getItem('x-auth-token')
  if (!token || token !== loadedToken || !cloudRendererConfigReady.value) throw new Error('请先重新读取平台云渲染配置')
  const result = await requestConfig('PUT', token, { revision: expectedRevision, config })
  acceptConfig(result, token)
  return getCloudRendererConfig()
}

export async function resetCloudRendererConfig (expectedRevision = revision) {
  return saveCloudRendererConfig(deploymentConfig(), expectedRevision)
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
