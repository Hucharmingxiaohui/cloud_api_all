import { getPlatformInfo } from '/@/api/manage'
import { importSubKmzFile, overwriteWaylineFile } from '/@/api/wayline'
import { ELocalStorageKey } from '/@/types'
import { getCloudRendererConfig } from './cloudRendererConfig'

export type CaptureMode = 'none' | 'visable' | 'ir' | 'visable,ir'

export type PhotoActionLens = 'visable' | 'ir' | 'visable,ir'

export interface WaypointPhotoAction {
  imageFormat: PhotoActionLens
  focalLength: number
  pitch: number
  /** 可选云台偏航：真实北向 0-360（顺时针）；缺省跟随航点机头（对接文档 4.6.4） */
  yaw?: number
}

export interface WaypointCamera {
  heading: number
  pitch: number
  roll: number
  focalLength: number
}

export interface CloudKmzWaypoint {
  point_name: string
  longitude: number
  latitude: number
  height: number
  capture_mode: CaptureMode
  speed: number
  headingTrue: number
  camera_params: WaypointCamera
  /**
   * 多拍照动作（对接文档 4.6.4）：存在且非空时按项生成拍照动作（KMZ actionIndex 从 1 顺延），
   * 覆盖 capture_mode 单动作模式；空数组 = 过渡点；缺省 = 回退单动作（向后兼容）。
   */
  photo_actions?: WaypointPhotoAction[]
}

export interface BuildWaylineKmzOptions {
  routeName: string
  waypoints: Array<Partial<CloudKmzWaypoint>>
  signal?: AbortSignal
  overwriteWaylineId?: string
  importToLibrary?: boolean
}

export interface BuildWaylineKmzResult {
  blob: Blob
  fileName: string
  imported: boolean
}

export function normalizeRouteName (name: string) { return String(name).replace(/_/g, '-') }

export function sanitizeFileName (name: string) { return normalizeRouteName(name).replace(/[\\/:*?"<>|\s_]+/g, '-') || 'wayline' }

export function sanitizeAsciiFileName (name: string) {
  const ascii = sanitizeFileName(name)
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9.-]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-+|-+$/g, '')
  return ascii || `wayline-${Date.now()}`
}

export function normalizeHeading (value: number) { return Math.min(((Number(value) % 360) + 360) % 360, 359) }

export function normalizeCaptureMode (mode: unknown): CaptureMode {
  const normalized = String(mode || 'visable').replace(/visible/g, 'visable')
  if (normalized === 'none' || normalized === 'visable' || normalized === 'ir' || normalized === 'visable,ir') return normalized
  if (normalized === 'ir,visable') return 'visable,ir'
  return 'visable'
}

/**
 * 规范化多拍照动作（对接文档 4.6.4）：剔除焦距非法/镜头组合非法的项，最多保留 10 个。
 * 缺省（非数组）返回 undefined，保持单动作向后兼容语义。
 */
export function normalizePhotoActions (value: unknown): WaypointPhotoAction[] | undefined {
  if (!Array.isArray(value)) return undefined
  const actions: WaypointPhotoAction[] = []
  for (const raw of value.slice(0, 10)) {
    const item = (raw && typeof raw === 'object' ? raw : {}) as Record<string, unknown>
    const lens = normalizeCaptureMode(item.imageFormat ?? item.image_format)
    const focalLength = Number(item.focalLength ?? item.focal_length)
    if (lens === 'none' || !Number.isFinite(focalLength) || focalLength <= 0) continue
    const pitchRaw = Number(item.pitch)
    const pitch = Number.isFinite(pitchRaw) ? Math.min(0, Math.max(-90, pitchRaw)) : -45
    const action: WaypointPhotoAction = { imageFormat: lens, focalLength, pitch }
    const rawYaw = item.yaw
    if (rawYaw !== null && rawYaw !== undefined && String(rawYaw).trim() !== '') {
      const yaw = Number(rawYaw)
      if (Number.isFinite(yaw)) action.yaw = ((yaw % 360) + 360) % 360
    }
    actions.push(action)
  }
  return actions
}

export function normalizeWaypoint (point: Partial<CloudKmzWaypoint>, index: number): CloudKmzWaypoint {
  const camera: Partial<WaypointCamera> = point.camera_params || {}
  const headingFromCamera = Number(camera.heading)
  const headingFromTrue = Number(point.headingTrue)
  const heading = normalizeHeading(Number.isFinite(headingFromCamera) ? headingFromCamera : (Number.isFinite(headingFromTrue) ? headingFromTrue : 0))
  return {
    point_name: String(point.point_name || `WP_${String(index + 1).padStart(3, '0')}`),
    longitude: Number(point.longitude),
    latitude: Number(point.latitude),
    height: Number(point.height),
    capture_mode: normalizeCaptureMode(point.capture_mode),
    speed: Number(point.speed ?? 5),
    headingTrue: heading,
    camera_params: {
      heading,
      pitch: Number(camera.pitch ?? -45),
      roll: Number(camera.roll ?? 0),
      focalLength: Number(camera.focalLength ?? 75)
    },
    photo_actions: normalizePhotoActions(point.photo_actions)
  }
}

export function validateWaypoints (points: Array<Partial<CloudKmzWaypoint>>) {
  if (!points.length) return '请至少添加一个航点'
  if (points.length > 2000) return '航点数量不能超过 2000'
  for (let index = 0; index < points.length; index += 1) {
    const rawMulti = points[index]?.photo_actions
    if (Array.isArray(rawMulti) && rawMulti.length > 10) return `航点 ${index + 1} 拍照动作不能超过 10 个`
    const point = normalizeWaypoint(points[index], index)
    if (!Number.isFinite(point.longitude) || point.longitude < -180 || point.longitude > 180) return `航点 ${index + 1} 经度无效`
    if (!Number.isFinite(point.latitude) || point.latitude < -90 || point.latitude > 90) return `航点 ${index + 1} 纬度无效`
    if (!Number.isFinite(point.height)) return `航点 ${index + 1} 高度无效`
    if (!Number.isFinite(point.speed) || point.speed <= 0) return `航点 ${index + 1} 速度无效`
    if (!Number.isFinite(point.camera_params.heading)) return `航点 ${index + 1} 缺少偏航角`
    if (point.capture_mode !== 'none' && (!Number.isFinite(point.camera_params.focalLength) || point.camera_params.focalLength <= 0)) return `航点 ${index + 1} 焦距无效`
    if (point.photo_actions && point.photo_actions.length > 10) return `航点 ${index + 1} 拍照动作不能超过 10 个`
  }
  return ''
}

export function buildWaylineRequest (routeName: string, points: Array<Partial<CloudKmzWaypoint>>) {
  const waypoints = points.map(normalizeWaypoint)
  return {
    routeName,
    templateType: 'waypoint',
    droneType: 100,
    subDroneType: 1,
    payloadType: 99,
    payloadPosition: 0,
    finishAction: 'goHome',
    exitOnRcLostAction: 'goBack',
    globalHeight: Number(waypoints[0].height || 80),
    takeOffSecurityHeight: 20,
    globalRTHHeight: 100,
    globalTransitionalSpeed: 10,
    autoFlightSpeed: 5,
    imageFormat: 'visable',
    gimbalPitchMode: 'usePointSetting',
    waypointHeadingReq: { waypointHeadingMode: 'fixed', waypointHeadingAngle: 0 },
    waypointTurnReq: { waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', useStraightLine: 1 },
    startActionList: [],
    routePointList: waypoints.map((point, index) => {
      const heading = normalizeHeading(point.camera_params.heading)
      const actions: Record<string, unknown>[] = [{ actionIndex: 0, aircraftHeading: heading, aircraftPathMode: 'counterClockwise' }]
      const photoActions = point.photo_actions
      if (photoActions !== undefined) {
        // photo_actions 存在时优先使用：空数组表示过渡点；每个动作的 actionIndex 从 1 连续顺延。
        photoActions.forEach((photoAction, actionIndex) => {
          const actionYaw = Number(photoAction.yaw)
          actions.push({
            actionIndex: actionIndex + 1,
            takePhotoType: 2,
            useGlobalImageFormat: 0,
            imageFormat: photoAction.imageFormat,
            orientedPhotoMode: 'normalPhoto',
            focalLength: photoAction.focalLength,
            gimbalYawRotateAngle: Number.isFinite(actionYaw) ? normalizeHeading(actionYaw) : heading,
            gimbalPitchRotateAngle: photoAction.pitch,
            imageWidth: 960,
            imageHeight: 720,
            orientedCameraApertue: 440,
            orientedCameraLuminance: 3800,
            orientedCameraShutterTime: 0.003,
            orientedCameraISO: 100,
            AFPos: 159,
            focusX: 480,
            focusY: 360,
            focusRegionWidth: 480,
            focusRegionHeight: 360,
            orientedFileSuffix: point.point_name || `WP_${index + 1}`
          })
        })
      } else if (point.capture_mode !== 'none') {
        // 向后兼容：旧单动作航点仍从 capture_mode/camera_params 组装。
        actions.push({
          actionIndex: 1,
          takePhotoType: 2,
          useGlobalImageFormat: 0,
          imageFormat: point.capture_mode,
          orientedPhotoMode: 'normalPhoto',
          focalLength: Number(point.camera_params.focalLength || 75),
          gimbalYawRotateAngle: heading,
          gimbalPitchRotateAngle: Number(point.camera_params.pitch ?? -45),
          imageWidth: 960,
          imageHeight: 720,
          orientedCameraApertue: 440,
          orientedCameraLuminance: 3800,
          orientedCameraShutterTime: 0.003,
          orientedCameraISO: 100,
          AFPos: 159,
          focusX: 480,
          focusY: 360,
          focusRegionWidth: 480,
          focusRegionHeight: 360,
          orientedFileSuffix: point.point_name || `WP_${index + 1}`
        })
      }
      return {
        routePointIndex: index,
        longitude: Number(point.longitude),
        latitude: Number(point.latitude),
        height: Number(point.height),
        speed: Number(point.speed || 5),
        gimbalPitchAngle: 0,
        waypointHeadingReq: { waypointHeadingMode: 'fixed', waypointHeadingAngle: heading },
        waypointTurnReq: { waypointTurnMode: 'toPointAndStopWithDiscontinuityCurvature', useStraightLine: 1 },
        actions
      }
    })
  }
}

export async function resolveWorkspaceId () {
  const storedWorkspaceId = localStorage.getItem(ELocalStorageKey.WorkspaceId)?.trim()
  if (storedWorkspaceId) return storedWorkspaceId

  try {
    const platformInfo = await getPlatformInfo()
    const workspaceId = String(platformInfo?.data?.workspace_id || platformInfo?.workspace_id || '').trim()
    if (!workspaceId) throw new Error('未获取到工作空间，请重新登录后再试')
    localStorage.setItem(ELocalStorageKey.WorkspaceId, workspaceId)
    return workspaceId
  } catch (error: unknown) {
    const status = typeof error === 'object' && error && 'response' in error ? (error as { response?: { status?: number } }).response?.status : undefined
    if (status === 401) throw new Error('登录已过期，请重新登录后再导入航线')
    throw new Error(error instanceof Error ? error.message : '获取工作空间失败，请重新登录')
  }
}

export async function importKmzBlob (workspaceId: string, blob: Blob, fileName: string, overwriteWaylineId?: string) {
  const fileData = new FormData()
  const uploadFileName = `${sanitizeAsciiFileName(fileName.replace(/\.kmz$/i, ''))}.kmz`
  fileData.append('file', new File([blob], uploadFileName, { type: 'application/vnd.google-earth.kmz' }))
  const importRes = overwriteWaylineId
    ? await overwriteWaylineFile(workspaceId, overwriteWaylineId, fileData)
    : await importSubKmzFile(workspaceId, fileData)
  if (importRes?.code !== 0) {
    const msg = String(importRes?.message || '航线导入失败')
    if (/already exists|已存在|filename/i.test(msg)) throw new Error('航线名称已存在，请更换名称后重试')
    throw new Error(msg)
  }
}

export async function buildAndDownloadWaylineKmz (options: BuildWaylineKmzOptions): Promise<BuildWaylineKmzResult> {
  const routeName = normalizeRouteName(options.routeName.trim())
  const error = validateWaypoints(options.waypoints)
  if (error) throw new Error(error)
  if (!routeName) throw new Error('请输入航线名称')

  const config = getCloudRendererConfig()
  const response = await fetch(new URL('/api/wayline/build-kmz', config.baseURL).toString(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/vnd.google-earth.kmz' },
    body: JSON.stringify(buildWaylineRequest(routeName, options.waypoints)),
    signal: options.signal
  })
  if (!response.ok) throw new Error(await response.text() || `服务返回 ${response.status}`)
  const contentType = response.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    const payload = await response.json().catch(() => null)
    throw new Error(payload?.message || payload?.error || '云渲染服务未返回 KMZ 文件')
  }

  const blob = await response.blob()
  if (!blob.size) throw new Error('云渲染服务返回了空的 KMZ 文件')
  const fileName = `${sanitizeAsciiFileName(routeName)}.kmz`
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)

  if (options.importToLibrary === false) return { blob, fileName, imported: false }
  const workspaceId = await resolveWorkspaceId()
  await importKmzBlob(workspaceId, blob, fileName, options.overwriteWaylineId)
  return { blob, fileName, imported: true }
}
