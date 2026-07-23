/** KMZ 解析结果 → 云渲染三维航线 waypoints */

export type CaptureMode = 'none' | 'visable' | 'ir' | 'visable,ir'

export interface CloudWaypointCamera {
  heading: number
  pitch: number
  roll: number
  focalLength: number
}

export interface CloudWaypoint {
  point_name: string
  longitude: number
  latitude: number
  height: number
  capture_mode: CaptureMode
  speed: number
  headingTrue: number
  camera_params: CloudWaypointCamera
}

export interface CloudWaylineDraft {
  waylineId: string
  routeName: string
  selectedIndex: number
  waypoints: CloudWaypoint[]
  source: 'edit'
  createdAt: number
}

export const CLOUD_WAYLINE_EDIT_DRAFT_KEY = 'cloud-wayline-edit-draft'

function asRecord (value: unknown): Record<string, any> | null {
  return value && typeof value === 'object' ? value as Record<string, any> : null
}

function toNumber (value: unknown, fallback = NaN): number {
  if (value == null || value === '') return fallback
  const n = Number(value)
  return Number.isFinite(n) ? n : fallback
}

export function normalizeHeading (value: number): number {
  if (!Number.isFinite(value)) return 0
  return Math.min(((value % 360) + 360) % 360, 359)
}

function normalizePitch (value: number): number {
  if (!Number.isFinite(value)) return -45
  return Math.min(0, Math.max(-90, value))
}

function parseCoordinates (raw: unknown): { longitude: number; latitude: number } | null {
  if (raw == null) return null
  if (typeof raw === 'object') {
    const obj = raw as Record<string, unknown>
    const lon = toNumber(obj.longitude ?? obj.lon ?? obj.lng)
    const lat = toNumber(obj.latitude ?? obj.lat)
    if (Number.isFinite(lon) && Number.isFinite(lat)) return { longitude: lon, latitude: lat }
  }
  const text = String(raw).trim().replace(/\s+/g, ' ')
  if (!text) return null
  const parts = text.split(/[,\s]+/).map(Number)
  if (parts.length < 2 || !Number.isFinite(parts[0]) || !Number.isFinite(parts[1])) return null
  return { longitude: parts[0], latitude: parts[1] }
}

function listActions (placemark: Record<string, any>): Record<string, any>[] {
  const group = placemark.actionGroup || placemark.action_group
  if (!group) return []
  if (Array.isArray(group)) {
    return group.flatMap((g: any) => {
      const list = g?.actionList || g?.action || g?.actions || []
      return Array.isArray(list) ? list : []
    })
  }
  const list = group.actionList || group.action || group.actions || []
  return Array.isArray(list) ? list : []
}

function actionName (action: Record<string, any>): string {
  return String(action.actionActuatorFunc || action.action_actuator_func || action.type || '').trim()
}

function actionParam (action: Record<string, any>): Record<string, any> {
  const param = action.actionActuatorFuncParam || action.action_actuator_func_param || action.param || action
  return asRecord(param) || {}
}

function pickOriented (param: Record<string, any>): Record<string, any> | null {
  return asRecord(param.orientedShoot) || asRecord(param.OrientedShoot) ||
    (param.gimbalYawRotateAngle != null || param.focalLength != null ? param : null)
}

function pickRotateYaw (param: Record<string, any>): Record<string, any> | null {
  return asRecord(param.rotateYaw) || asRecord(param.RotateYaw) ||
    (param.aircraftHeading != null ? param : null)
}

function pickGimbalRotate (param: Record<string, any>): Record<string, any> | null {
  return asRecord(param.gimbalRotate) || asRecord(param.GimbalRotate) ||
    (param.gimbalPitchRotateAngle != null || param.gimbalYawRotateAngle != null ? param : null)
}

function pickZoom (param: Record<string, any>): Record<string, any> | null {
  return asRecord(param.zoom) || asRecord(param.Zoom) ||
    (param.focalLength != null ? param : null)
}

function normalizeCaptureMode (raw: unknown): CaptureMode {
  const text = String(raw || '').toLowerCase().replace(/\s+/g, '')
  if (!text) return 'none'
  const hasVisible = /visable|visible|wide|zoom/.test(text)
  const hasIr = /\bir\b|infrared|thermal/.test(text)
  if (hasVisible && hasIr) return 'visable,ir'
  if (hasIr) return 'ir'
  if (hasVisible) return 'visable'
  if (text.includes('none')) return 'none'
  return 'visable'
}

function extractPlacemarks (wayline: Record<string, any>): Record<string, any>[] {
  const folder = wayline.folder || wayline.Folder || {}
  const list = folder.placeMarks || folder.placemarks || folder.placemarkList || folder.Placemark || []
  return Array.isArray(list) ? list : []
}

function extractRouteName (wayline: Record<string, any>, fallback: string): string {
  const mission = wayline.missionConfig || wayline.MissionConfig || {}
  const name = mission.fileName || mission.file_name || wayline.routeName || wayline.name || fallback
  return String(name || fallback).replace(/_/g, '-').trim() || fallback
}

function mapOnePlacemark (
  placemark: Record<string, any>,
  index: number,
  globalSpeed: number,
  globalHeight: number
): CloudWaypoint | null {
  const point = placemark.point || placemark.Point || {}
  const coords = parseCoordinates(point.coordinates || placemark.coordinates)
  if (!coords) return null

  let height = toNumber(
    placemark.ellipsoidHeight ?? placemark.ellipsoid_height ?? placemark.height ?? placemark.executeHeight,
    NaN
  )
  if (!Number.isFinite(height) || (toNumber(placemark.useGlobalHeight, 0) === 1 && Number.isFinite(globalHeight))) {
    height = Number.isFinite(globalHeight) ? globalHeight : height
  }
  if (!Number.isFinite(height)) return null

  const speed = toNumber(placemark.waypointSpeed ?? placemark.waypoint_speed ?? placemark.speed, globalSpeed)
  const headingParam = placemark.waypointHeadingParam || placemark.waypoint_heading_param || {}
  let headingTrue = toNumber(headingParam.waypointHeadingAngle ?? headingParam.waypoint_heading_angle, NaN)
  let pitch = toNumber(placemark.gimbalPitchAngle ?? placemark.gimbal_pitch_angle, NaN)
  let focalLength = NaN
  let captureMode: CaptureMode = 'none'
  let parsedYaw = NaN

  const actions = listActions(placemark)
  for (const action of actions) {
    const name = actionName(action)
    const param = actionParam(action)

    if (name === 'orientedShoot' || name === 'oriented_shoot') {
      const oriented = pickOriented(param) || {}
      const yaw = toNumber(oriented.gimbalYawRotateAngle ?? oriented.gimbal_yaw_rotate_angle, NaN)
      const p = toNumber(oriented.gimbalPitchRotateAngle ?? oriented.gimbal_pitch_rotate_angle, NaN)
      const f = toNumber(oriented.focalLength ?? oriented.focal_length, NaN)
      const lens = oriented.payloadLensIndex ?? oriented.payload_lens_index ?? oriented.imageFormat
      if (Number.isFinite(yaw)) {
        parsedYaw = yaw
        if (!Number.isFinite(headingTrue)) headingTrue = yaw
      }
      const aircraftHeading = toNumber(oriented.aircraftHeading ?? oriented.aircraft_heading, NaN)
      if (Number.isFinite(aircraftHeading)) headingTrue = aircraftHeading
      if (Number.isFinite(p)) pitch = p
      if (Number.isFinite(f)) focalLength = f
      captureMode = normalizeCaptureMode(lens || 'visable')
      continue
    }

    if (name === 'rotateYaw' || name === 'rotate_yaw') {
      const rotate = pickRotateYaw(param) || {}
      const aircraftHeading = toNumber(rotate.aircraftHeading ?? rotate.aircraft_heading, NaN)
      if (Number.isFinite(aircraftHeading)) headingTrue = aircraftHeading
      continue
    }

    if (name === 'gimbalRotate' || name === 'gimbal_rotate') {
      const gimbal = pickGimbalRotate(param) || {}
      const yaw = toNumber(gimbal.gimbalYawRotateAngle ?? gimbal.gimbal_yaw_rotate_angle, NaN)
      const p = toNumber(gimbal.gimbalPitchRotateAngle ?? gimbal.gimbal_pitch_rotate_angle, NaN)
      if (Number.isFinite(yaw) && !Number.isFinite(parsedYaw)) parsedYaw = yaw
      if (Number.isFinite(p) && !Number.isFinite(pitch)) pitch = p
      continue
    }

    if (name === 'zoom') {
      const zoom = pickZoom(param) || {}
      const f = toNumber(zoom.focalLength ?? zoom.focal_length, NaN)
      if (Number.isFinite(f) && !Number.isFinite(focalLength)) focalLength = f
      continue
    }

    if (name === 'takePhoto' || name === 'take_photo' || name === 'panoShot') {
      if (captureMode === 'none') {
        const photo = asRecord(param.takePhoto) || param
        captureMode = normalizeCaptureMode(photo.payloadLensIndex || photo.payload_lens_index || 'visable')
      }
    }
  }

  // 统一真实偏航：rotateYaw/aircraft > waypointHeading > oriented/gimbal yaw
  if (!Number.isFinite(headingTrue)) headingTrue = Number.isFinite(parsedYaw) ? parsedYaw : 0
  headingTrue = normalizeHeading(headingTrue)
  if (!Number.isFinite(pitch)) pitch = -45
  if (!Number.isFinite(focalLength)) focalLength = 75

  const idx = toNumber(placemark.index, index)
  const suffix = String(idx + 1).padStart(3, '0')

  return {
    point_name: String(placemark.point_name || placemark.name || `WP_${suffix}`),
    longitude: coords.longitude,
    latitude: coords.latitude,
    height,
    capture_mode: captureMode,
    speed: Number.isFinite(speed) && speed > 0 ? speed : 5,
    // 兼容字段：与 camera_params.heading 同为真实偏航
    headingTrue,
    camera_params: {
      heading: headingTrue,
      pitch: normalizePitch(pitch),
      roll: toNumber(placemark.roll, 0) || 0,
      focalLength: focalLength > 0 ? focalLength : 75
    }
  }
}

/**
 * 将 getKmzWaypointWayLineInfo / editWaylineInfo 返回的 data 映射为云渲染航线草稿
 */
export function mapParsedWaylineToCloudDraft (
  waylinePayload: unknown,
  options: { waylineId: string; routeName?: string }
): CloudWaylineDraft {
  const root = asRecord(waylinePayload)
  if (!root) throw new Error('航线解析结果为空')

  // 兼容 { code, data } 或直接 Wayline
  const wayline = asRecord(root.data) || root
  const folder = asRecord(wayline.folder) || asRecord(wayline.Folder) || {}
  const globalSpeed = toNumber(folder.autoFlightSpeed ?? folder.auto_flight_speed, 5)
  const globalHeight = toNumber(folder.globalHeight ?? folder.global_height, NaN)
  const placemarks = extractPlacemarks(wayline)
  if (!placemarks.length) throw new Error('航线中未解析到航点')

  const waypoints: CloudWaypoint[] = []
  placemarks.forEach((pm, index) => {
    const mapped = mapOnePlacemark(asRecord(pm) || {}, index, globalSpeed, globalHeight)
    if (mapped) waypoints.push(mapped)
  })
  if (!waypoints.length) throw new Error('航点坐标/高度无效，无法进入三维编辑')

  return {
    waylineId: options.waylineId,
    routeName: extractRouteName(wayline, options.routeName || `wayline-${options.waylineId.slice(0, 8)}`),
    selectedIndex: waypoints.length ? 0 : -1,
    waypoints,
    source: 'edit',
    createdAt: Date.now()
  }
}

export function saveCloudWaylineEditDraft (draft: CloudWaylineDraft) {
  sessionStorage.setItem(CLOUD_WAYLINE_EDIT_DRAFT_KEY, JSON.stringify(draft))
}

export function readCloudWaylineEditDraft (): CloudWaylineDraft | null {
  try {
    const raw = sessionStorage.getItem(CLOUD_WAYLINE_EDIT_DRAFT_KEY)
    if (!raw) return null
    const draft = JSON.parse(raw) as CloudWaylineDraft
    if (!draft || !Array.isArray(draft.waypoints) || !draft.waypoints.length) return null
    return draft
  } catch {
    return null
  }
}

export function clearCloudWaylineEditDraft () {
  sessionStorage.removeItem(CLOUD_WAYLINE_EDIT_DRAFT_KEY)
}
