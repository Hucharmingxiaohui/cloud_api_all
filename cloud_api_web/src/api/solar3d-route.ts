import axios from 'axios'

export interface Solar3DPreviewWaypoint {
  waypoint_id?: string
  index?: number
  lon: number
  lat: number
  height: number
  aircraft_heading?: number
  camera?: any
  cameras?: any[]
}

export interface Solar3DPreviewPayload {
  route_draft_id: string
  route_name: string
  type?: string
  imageFormat?: string
  origin?: any
  areas?: any[]
  waypoints: Solar3DPreviewWaypoint[]
}

export interface Solar3DEditedWaypoint {
  lon: number
  lat: number
  height: number
  aircraft_heading: number
  pitch: number
  visible_focal_length_35mm?: number
  ir_focal_length_35mm?: number
}

export interface Solar3DEditedPayload {
  route_draft_id: string
  waypoints: Solar3DEditedWaypoint[]
}

export const SOLAR_3D_PREVIEW_STORAGE_KEY = 'solar3dRoutePreview'

export async function submitSolar3DEditedRoute (data: Solar3DEditedPayload): Promise<any> {
  const isDev = import.meta.env.MODE === 'development'
  const configuredURL = window.CURRENT_CONFIG?.solarRouteURL?.replace(/\/$/, '')
  const url = isDev
    ? '/dev-api/solar3d-edited'
    : `${configuredURL || 'http://172.20.63.157:5001'}/solar/edited`

  const requestId = `solar-edited-${Date.now()}-${Math.random().toString(16).slice(2)}`
  console.log('光伏三维航线编辑回传请求ID:', requestId)

  const result = await axios.post(url, data, {
    headers: {
      'Content-Type': 'application/json',
      'X-Solar-Edited-Request-Id': requestId
    },
    timeout: 12000
  })
  return result.data
}

export async function fetchDevSolar3DPreviewPayload (): Promise<Solar3DPreviewPayload | null> {
  if (import.meta.env.MODE !== 'development') return null

  const result = await axios.get('/dev-api/solar3d-preview', {
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' }
  })
  return result.data?.data || null
}

export async function fetchDevSolar3DPreview (): Promise<{ payload: Solar3DPreviewPayload | null, version: number }> {
  if (import.meta.env.MODE !== 'development') return { payload: null, version: 0 }

  const result = await axios.get('/dev-api/solar3d-preview', {
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' }
  })
  return {
    payload: result.data?.data || null,
    version: Number(result.data?.version || 0)
  }
}

export async function clearDevSolar3DPreview (): Promise<void> {
  if (import.meta.env.MODE !== 'development') return

  await axios.delete('/dev-api/solar3d-preview', {
    params: { _t: Date.now() },
    headers: { 'Cache-Control': 'no-cache' }
  })
}

export function saveSolar3DPreviewPayload (data: Solar3DPreviewPayload) {
  sessionStorage.setItem(SOLAR_3D_PREVIEW_STORAGE_KEY, JSON.stringify(data))
}

export function readSolar3DPreviewPayload (): Solar3DPreviewPayload | null {
  const raw = sessionStorage.getItem(SOLAR_3D_PREVIEW_STORAGE_KEY)
  if (!raw) return null
  return JSON.parse(raw)
}
